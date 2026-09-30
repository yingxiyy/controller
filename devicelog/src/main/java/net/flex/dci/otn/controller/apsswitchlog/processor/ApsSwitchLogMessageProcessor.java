package net.flex.dci.otn.controller.apsswitchlog.processor;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ChangeType;
import net.flex.dci.otc.common.enums.ElementType;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otn.controller.apsswitchlog.component.ApsSwitchLogConvertor;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsSwitchLogDto;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import net.flex.dci.otn.db.jpa.entity.ApsSwitchLog;
import org.springframework.stereotype.Component;

/**
 * 2026/5/17
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ApsSwitchLogMessageProcessor {


    private final ApsSwitchLogConvertor apsSwitchLogConvertor;

    public void processApsSwitchLogMessage(ApsSwitchLog apsSwitchLog) {
        log.debug("process handle the aps Switch Log,the aps ref NeId:{}", apsSwitchLog.getNeId());
        String neId = apsSwitchLog.getNeId();
        ApsSwitchLogDto apsSwitchLogDto = apsSwitchLogConvertor.convert2ApsSwitchLog(
                apsSwitchLog);

        sendSwitchgrassLogAsynchronous(neId, apsSwitchLogDto);

    }


    public void processBatch(List<ApsSwitchLog> batch) {
        log.debug("process batch aps switch log:{}", batch);
        List<ApsSwitchLogDto> apsSwitchLogDtos = apsSwitchLogConvertor.convertApsSwitchLog2RestOutput(
                batch);
        sendSwitchgrassLogAsynchronous(apsSwitchLogDtos);
    }

    private void sendSwitchgrassLogAsynchronous(List<ApsSwitchLogDto> apsSwitchLogDtos) {
        log.info("send aps switch log asynchronous size:{}", apsSwitchLogDtos.size());
        apsSwitchLogDtos.forEach(
                apsSwitchLogDto -> sendSwitchgrassLogAsynchronous(apsSwitchLogDto.getNeId(),
                        apsSwitchLogDto));
    }


    private void sendSwitchgrassLogAsynchronous(String neId, ApsSwitchLogDto apsSwitchLogDto) {
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .content(apsSwitchLogDto)
                .changeType(ChangeType.CREATE)
                .elementType(ElementType.APS_SWITCH_LOG)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification, neId);
    }
}
