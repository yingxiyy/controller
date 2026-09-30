package net.flex.dci.otc.controller.otdr.notification.impl;

import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.controller.otdr.domain.OtdrScanDetail;
import net.flex.dci.otc.controller.otdr.domain.OtdrTaskResult;
import net.flex.dci.otc.controller.otdr.domain.OtdrTaskResultDetail;
import net.flex.dci.otc.controller.otdr.notification.OtdrScanStateNotification;
import net.flex.dci.otc.controller.otdr.service.OtdrNotificationService;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/2 15:38
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrScanStateNotificationImpl implements OtdrScanStateNotification {

    private final OtdrNotificationService otdrNotificationService;


    @Override
    public void sendOtdrScanStateNotification(OtdrScanDetail otdrScanDetail) {
        log.debug("send otdr scan progress ");

        OtdrTaskResult otdrTaskResult = OtdrTaskResult.builder()
                .detail(OtdrTaskResultDetail.builder()
                        .scanResultType(otdrScanDetail.getState())
                        .monitorTpName(otdrScanDetail.getMonitorName())
                        .nodeId(otdrScanDetail.getNodeId())
                        .build())
                .build();
        if (otdrScanDetail.getState() != OtdrScanResultType.FAIL
                && otdrScanDetail.getState() != OtdrScanResultType.NOSCAN) {
            otdrNotificationService.publishNotification(EventType.Update, otdrTaskResult);
        }
        broadcastScanProgress(otdrScanDetail);
    }

    /**
     * send broad case message to show the scan progress
     *
     * @param otdrScanDetail
     */
    private void broadcastScanProgress(OtdrScanDetail otdrScanDetail) {
        log.debug("current otdr scan progress is:{}", otdrScanDetail.getState());
        OtdrScanResultType scanResultType = otdrScanDetail.getState();
        BroadcastMessage.BroadcastMessageBuilder broadcastMessageBuilder = BroadcastMessage.builder();
        boolean isError = false;

        String message = null;
        String title = BroadCastConstant.OTDR_SCAN_TASK_IN_PROGRESS;
        switch (scanResultType) {
            case INPROGRESS:
                message = String.format(
                        "The NE:%s monitor tp:%s monitor direction :%s otdr scan work is in progress",
                        otdrScanDetail.getNodeName(),
                        otdrScanDetail.getMonitorName(),
                        otdrScanDetail.getDirection().name());
                title = BroadCastConstant.OTDR_SCAN_TASK_IN_PROGRESS;
                break;
            case FAIL:
                message = String.format(
                        "The NE:%s monitor tp:%s monitor direction :%s otdr scan work Failed",
                        otdrScanDetail.getNodeName(),
                        otdrScanDetail.getMonitorName(),
                        otdrScanDetail.getDirection().name());
                title = BroadCastConstant.OTDR_SCAN_TASK_FAILED;
                isError = true;
                break;
            case COMPLETE:
                message = String.format(
                        "The NE:%s monitor tp:%s monitor direction :%s otdr scan work is completely",
                        otdrScanDetail.getNodeName(),
                        otdrScanDetail.getMonitorName(),
                        otdrScanDetail.getDirection().name());
                title = BroadCastConstant.OTDR_SCAN_TASK_COMPLETED;
                break;
        }
        broadcastMessageBuilder.error(isError);
        if (Objects.nonNull(message)) {
            broadcastMessageBuilder.message(message);
            broadcastMessageBuilder.title(title);
            BroadcastMessager.publishKafkaMessage(broadcastMessageBuilder.build());
        }

    }


}
