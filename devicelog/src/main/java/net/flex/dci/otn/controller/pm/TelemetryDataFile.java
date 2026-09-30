package net.flex.dci.otn.controller.pm;

import com.google.protobuf.util.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.telemetry.proto.TelemetryOuterClass;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Data
public class TelemetryDataFile {
    private Path file;
    private TelemetryOuterClass.TelemetryStreamPublish telemetryData;

    public TelemetryDataFile(Path file) {
        this.file = file;
    }

    public TelemetryDataFile parse() throws Exception {
        String json = new String(Files.readAllBytes(file), "UTF-8");

        TelemetryOuterClass.TelemetryStreamPublish.Builder builder = TelemetryOuterClass.TelemetryStreamPublish.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(json, builder);
        telemetryData = builder.build();

//        debugLog();
        return this;
    }

    private void debugLog() {
        log.debug("Node ID: " + telemetryData.getHeader().getNodeId().getValue());
        log.debug("Version: " + telemetryData.getHeader().getVersion().getValue());
        log.debug("IP: " + telemetryData.getHeader().getIpaddr().getValue());

        telemetryData.getFieldsList().forEach(field -> {
            log.debug("Timestamp: " + field.getTimestamp());
            log.debug("Base Path: " + field.getBasePath().getValue());

            // 这里是 TypedValue 的 oneof，检查是否是 transceiver
            if (field.getValue().hasTransceiver()) {
                field.getValue().getTransceiver().getTransceiverInfoList().forEach(info -> {
                    log.debug("Transceiver Name: " + info.getName().getValue());
                    log.debug("Temperature Instant: " + info.getState().getTemperature().getInstant().getValue());
                    log.debug("Output Power Instant: " + info.getState().getOutputPower().getInstant().getValue());
                    log.debug("Input Power Instant: " + info.getState().getInputPower().getInstant().getValue());
                    log.debug("Laser Bias Current Instant: " + info.getState().getLaserBiasCurrent().getInstant().getValue());
                    log.debug("Input Voltage Instant: " + info.getState().getInputVoltage().getInstant().getValue());
                });
            }
        });
    }


    public static void main(String[] args) throws Exception {
        String fileName = "C:\\working\\dci\\controller\\devicelog\\historypm.json";
        Path filePath = Paths.get(fileName);
        TelemetryDataFile telemetryFile = new TelemetryDataFile(filePath);
        telemetryFile.parse();

        System.out.println("Node ID: " + telemetryFile.getTelemetryData().getHeader().getNodeId().getValue());
        System.out.println("Version: " + telemetryFile.getTelemetryData().getHeader().getVersion().getValue());

        telemetryFile.getTelemetryData().getFieldsList().forEach(field -> {
            System.out.println("Timestamp: " + field.getTimestamp());
            System.out.println("Base Path: " + field.getBasePath().getValue());

            // 这里是 TypedValue 的 oneof，检查是否是 transceiver
            if (field.getValue().hasTransceiver()) {
                field.getValue().getTransceiver().getTransceiverInfoList().forEach(info -> {
                    System.out.println("Transceiver Name: " + info.getName().getValue());
                    System.out.println("Temperature Instant: " + info.getState().getTemperature().getInstant().getValue());
                    System.out.println("Output Power Instant: " + info.getState().getOutputPower().getInstant().getValue());
                    System.out.println("Input Power Instant: " + info.getState().getInputPower().getInstant().getValue());
                    System.out.println("Laser Bias Current Instant: " + info.getState().getLaserBiasCurrent().getInstant().getValue());
                    System.out.println("Input Voltage Instant: " + info.getState().getInputVoltage().getInstant().getValue());
                });
            }
        });

        BroadcastMessager.publishMessage(
                "DCI",
                telemetryFile.getTelemetryData().toByteArray());
    }
}
