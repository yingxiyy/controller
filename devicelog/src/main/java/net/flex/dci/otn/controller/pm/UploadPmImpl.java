package net.flex.dci.otn.controller.pm;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.configuration.ConfigLoader;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.utils.AuxTools;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.upload.history.pm.input.RemoteServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.upload.history.pm.input.RemoteServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FileTransferProtocol;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UploadPmImpl {

    public static final String HISTORY_PM_FOLDER = "/upload/historyPM";

    private final Executor taskExecutor;
    private final PhyNodeDao phyNodeDao;
    private final ConfigLoader configLoader;
    private final FTPClient ftpClient;
    private final NeManagerRpc neRpc;

    // ========================== Entry ==========================

    @Async("taskExecutor")
    public void upload(List<String> neIds) {
        CmdStatusStore statusStore = new CmdStatusStore();
        FtpServer ftpServer = ftpClient.fetchFTPServer();

        List<CompletableFuture<Void>> tasks = neIds.stream()
                .map(neId -> CompletableFuture.runAsync(
                        () -> processNe(ftpServer, neId, statusStore),
                        taskExecutor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    log.info("All NE processed: {}", statusStore);
                    BroadcastMessager.publishKafkaMessage(
                            BroadcastMessage.builder()
                                    .title(BroadCastConstant.UPLOAD_NE_HISTORY_PM)
                                    .message(statusStore.toString())
                                    .error(true) //statusStore.hasError())
                                    .build());
                });
    }

    // ========================== NE level ==========================

    private void processNe(
            FtpServer ftpServer,
            String neId,
            CmdStatusStore statusStore) {

        Physical phy;
        try {
            Node node = phyNodeDao.getOpPhyNodeById(neId);
            if (node == null) {
                statusStore.set(neId, "Error: Node not found");
                return;
            }
            phy = node.getAugmentation(Node1.class).getPhysical();
        } catch (Exception e) {
            statusStore.set(neId, "Error: load node failed - " + e.getMessage());
            return;
        }

        String neName = phy.getFriendlyName();
        String neIp = phy.getIp();

        try {
            statusStore.set(neId, neName + " start 15Min PM");
            processPmType(ftpServer, neId, neName, neIp, "15Min");

            statusStore.set(neId, neName + " start 24Hour PM");
            processPmType(ftpServer, neId, neName, neIp, "24Hour");

            statusStore.set(neId, neName + " Success");

            cleanRemoteEnv(ftpServer, neName);

        } catch (Exception e) {
            String msg = String.format(
                    "Error: %s failed - %s",
                    neName,
                    e.getMessage());
            statusStore.set(neId, msg);
            log.error(msg, e);
        }
    }

    // ========================== PMType level ==========================

    /**
     * 单个 PMType 的完整生命周期 失败直接抛异常，由 processNe 统一记录状态
     */
    private void processPmType(
            FtpServer ftpServer,
            String neId,
            String neName,
            String neIp,
            String pmType) throws Exception {

        String remoteDir = HISTORY_PM_FOLDER + "/" + neName;
        File localBaseDir = new File(ConfigLoader.PROCESSING_ROOT, neName);
        File unzipDir = new File(localBaseDir, "tmp");

        // ---------- clean before start ----------
        ftpClient.createRemoteFolder(ftpServer, remoteDir);
        ftpClient.cleanRemoteFolder(ftpServer, remoteDir);
        cleanDirectory(localBaseDir);

        // ---------- RPC ----------
        long end = System.currentTimeMillis();
        long start = end - 7L * 24 * 60 * 60 * 1000;
        long duration = historyPmIntervalMillis(pmType);

        String remoteFile = remoteDir + "/" + System.currentTimeMillis() + ".tar.gz";
        rpcCall(neId, neIp, start, end, duration, ftpServer, remoteFile);

        // ---------- download ----------
        ftpClient.downloadFile(ftpServer, remoteDir, localBaseDir.getAbsolutePath());
        // ---------- unzip ----------
        unzipAll(localBaseDir, unzipDir);

        // ---------- parse ----------
        parsePmFiles(unzipDir);
    }

    static long historyPmIntervalMillis(String pmType) {
        return pmType.equals("15Min")
                ? Duration.ofMinutes(15).toMillis()
                : Duration.ofHours(24).toMillis();
    }

    // ========================== RPC ==========================

    private String rpcCall(
            String neId,
            String neIp,
            long start,
            long end,
            long duration,
            FtpServer ftpServer,
            String remoteDir) {

        RemoteServer remoteServer = new RemoteServerBuilder()
                .setUploadPath(remoteDir)
                .setAddress(ftpServer.getAddress())
                .setPort(ftpServer.getPort())
                .setUser(ftpServer.getUser())
                .setPassword(ftpServer.getPassword())
                .setProtocol(FileTransferProtocol.SFTP)
                .setSourceAddress(neIp)
                .build();

        UploadHistoryPmOutput output =
                neRpc.uploadHistoryPmFromNe(neId, start, end, duration, remoteServer);

        if (output.getReturnCode() == RpcResultType.Success ||
                output.getReturnCode() == RpcResultType.AcceptAndStartAsync) {
            return null;
        }

        throw new RuntimeException("RPC failed: " + output.getReturnMessage());
    }

    // ========================== File handling ==========================

    private void unzipAll(File downloadDir, File unzipDir) throws Exception {
        unzipDir.mkdirs();

        File[] tarFiles = downloadDir.listFiles(
                f -> f.getName().endsWith(".tar.gz") || f.getName().endsWith(".tgz"));

        if (tarFiles == null || tarFiles.length == 0) {
            throw new RuntimeException("No tar files in " + downloadDir);
        }

        for (File tar : tarFiles) {
            String cmd = String.format(
                    "tar -xzf \"%s\" -C \"%s\"",
                    tar.getAbsolutePath(),
                    unzipDir.getAbsolutePath());
            AuxTools.execLocal(cmd);
        }
    }

    private void parsePmFiles(File folder) throws Exception {
        List<Exception> exceptions = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            for (Path f : (Iterable<Path>) paths::iterator) {

                if (!Files.isRegularFile(f)) {
                    continue;
                }

                TelemetryDataFile df = new TelemetryDataFile(f);
                try {
                    df.parse();
                    BroadcastMessager.publishMessage(
                            configLoader.getKafkaTopic(),
                            df.getTelemetryData().toByteArray());
                } catch (Exception e) {
                    log.error("Failed to parse file '{}': {}", f, e.getMessage(), e);
                    exceptions.add(new Exception("Failed to parse " + f, e));
                }
            }
        }

        if (!exceptions.isEmpty()) {
            // Combine all exceptions into one
            Exception combined = new Exception("One or more files failed to parse");
            exceptions.forEach(combined::addSuppressed);
            throw combined;
        }
    }


    private void cleanDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }
        try {
            Files.walk(dir.toPath())
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (Exception ignored) {
        }
    }


    private void cleanRemoteEnv(FtpServer ftpServer, String neName) {
        String remoteDir = HISTORY_PM_FOLDER + "/" + neName;

        ftpClient.removeRemoteFolder(ftpServer, remoteDir);
    }

}


