//package net.flex.dci.otn.controller.schedule.core;
//
//import static net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.BACKUP_FOLDER;
//import static net.flex.dci.otn.controller.schedule.utils.ScheduleConstants.RESTORE_FOLDER;
//
//import cn.hutool.core.exceptions.ExceptionUtil;
//import java.math.BigInteger;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.Instant;
//import java.time.LocalDateTime;
//import java.time.ZoneId;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
//import net.flex.dci.otn.controller.schedule.message.TaskInfo;
//import net.flex.dci.otn.controller.schedule.task.TaskFactory;
//import net.flex.dci.otn.controller.schedule.utils.ConfigLoader;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.sort.query.params.SortInfos;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.sort.query.params.SortInfosBuilder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.controller.rev190906.ResultData1;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.controller.rev190906.ResultData1Builder;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.ExecuteResult;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.TaskStatus;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.result.ResultDataBuilder;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class Task {
//
//    private final ConfigLoader configLoader;
//    private final ScheduleRepo repo;
//    private final TaskFactory taskFactory;
//    private final DefaultTask defaultTask;
//    private final TaskInfo taskInfo;
//
//    private boolean initized = false;
//
//    ExecutorService executorService = Executors.newFixedThreadPool(5);
//
//    //    @PostConstruct
//    public void checkFtpEnv() {
//        String ftpServerName = configLoader.getSftpServerName();
//        if (ftpServerName == null || ftpServerName.isEmpty()) {
////            throw new IllegalArgumentException("SFTP server name is not configured");
//            log.info("SFTP server name is not configured, fetch from database first item");
//
//            List<FtpServer> ftpServers = configLoader.getFtpServerDao().listFtpServers();
//            if (ftpServers.isEmpty()) {
//                throw new IllegalArgumentException("No SFTP server is configured in database");
//            }
//            ftpServerName = ftpServers.get(0).getName();
//            log.info("fetch SFTP server name from database: {}", ftpServerName);
//        }
//
//        if (!configLoader.getFtpServerDao().existsByFtpServerName(ftpServerName)) {
//            throw new IllegalArgumentException(
//                    String.format("SFTP server name '%s' is NOT existed in database",
//                            ftpServerName));
//        }
//
//        Path backup = Paths.get(BACKUP_FOLDER); // local folder
//        Path restore = Paths.get(RESTORE_FOLDER); // local folder
//        try {
//            if (Files.notExists(backup)) {
//                Files.createDirectory(backup);
//            }
//            if (Files.notExists(restore)) {
//                Files.createDirectory(restore);
//            }
//        } catch (Exception e) {
//            throw new RuntimeException(
//                    String.format("cannot create local folder {}/{}", BACKUP_FOLDER,
//                            BACKUP_FOLDER));
//        }
//    }
//
//
//    @Scheduled(cron = "0 */1 * * * *") //每1分钟执行一次
////    @Scheduled(cron = "*/5 * * * * *") //每5s执行一次  for test
//    public void execute() {
//        log.debug("start search schedule table, find out which should be accept");
//        if (!initized) {
//            initDefaultTasks();
//            initized = true;
//        }
//
//        List<SortInfos> sortInfoList = new ArrayList<>();
//        sortInfoList.add(new SortInfosBuilder().setAscending(true).setSortName("_start").build());
//
//        long nowSeconds = Instant.now().getEpochSecond();
//        List<MoSchedule> moList = repo.getPaged(
//                String.format("_start >= %d and _start <= %d and _status == %s",
//                        nowSeconds - 60, nowSeconds + 60,
//                        TaskStatus.Activated.name()),
//                sortInfoList,
//                0, 0, Integer.MAX_VALUE);
////         moList = repo.getAll(); //for test
//
//        for (MoSchedule mo : moList) {
//            if (mo.getStatus().equals(TaskStatus.Expired)) {
//                continue;
//            } else if (mo.getStatus().equals(TaskStatus.Running)) {
//                log.info("The task {} is running", mo.getName());
//                return;
//            }
//
//            executorService.submit(() -> {
//                Thread.currentThread().setName(mo.getName());
//                long startTime = Instant.now().getEpochSecond();
//
//                log.info("Executing task: {}", mo.getName());
//                mo.setStatus(TaskStatus.Running);
//                repo.update(mo);
//                try {
//                    taskFactory.getImplement(mo.getOpertionGrouop()).process(mo);
//
//                    if (mo.getRepeatInterval() != null && mo.getRepeatInterval() > 0) {
//                        BigInteger nextStart = calculateNextRunTime(mo.getStart().longValue(),
//                                mo.getRepeatInterval());
//                        mo.setStart(nextStart);
//                    }
//                    mo.setResult(ExecuteResult.Success);
//                    mo.setResultData(new ResultDataBuilder()
//                            .addAugmentation(ResultData1.class, new ResultData1Builder()
//                                    .setDetail(ExecuteResult.Success.name())
//                                    .build())
//                            .build());
//                    return String.format("schedule action (%s) done.", mo.getName());
//                } catch (Exception e) {
//                    log.error("schedule action faield: {} ", mo.getName(), e);
//                    mo.setResult(ExecuteResult.Failure);
//                    mo.setResultData(new ResultDataBuilder()
//                            .addAugmentation(ResultData1.class, new ResultData1Builder()
//                                    .setDetail(ExceptionUtil.getRootCauseMessage(e))
//                                    .build())
//                            .build());
//                    return String.format("schedule action (%s) failed.", mo.getName());
//                } finally {
//                    taskInfo.send2TaskCenter(startTime * 1000, mo);
//                    repo.done(mo);
//                }
//            });
//        }
//    }
//
//    public void initDefaultTasks() {
//        List<MoSchedule> defaultTasks = defaultTask.build(configLoader.getSftpServerName());
//        defaultTasks.forEach(repo::save);
//    }
//
//    public static BigInteger calculateNextRunTime(Long currentStart, long repeatIntervalSeconds) {
//        if (currentStart == null || currentStart == 0) {
//            // First run: calculate the first occurrence of daily fixed time (0:30)
//            LocalDateTime now = LocalDateTime.now();
//            LocalDateTime target = now.withHour(0).withMinute(30).withSecond(0).withNano(0);
//
//            // If current time has already passed today's target, schedule for tomorrow
//            if (now.isAfter(target)) {
//                target = target.plusDays(1);
//            }
//
//            return BigInteger.valueOf(target.atZone(ZoneId.systemDefault()).toEpochSecond());
//        } else {
//            // Subsequent runs: just add repeat interval in seconds
//            return BigInteger.valueOf(currentStart + repeatIntervalSeconds);
//        }
//    }
//
//}
