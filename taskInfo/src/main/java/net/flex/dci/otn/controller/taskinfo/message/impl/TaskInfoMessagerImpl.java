package net.flex.dci.otn.controller.taskinfo.message.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ChangeType;
import net.flex.dci.otc.common.enums.ElementType;
import net.flex.dci.otc.common.model.element.ElementChangeNotification;
import net.flex.dci.otn.controller.taskinfo.dto.TaskInfoDto;
import net.flex.dci.otn.controller.taskinfo.message.TaskInfoMessager;
import net.flex.dci.otn.controller.tools.kafka.service.ElementChangeMessager;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/5 17:03
 */
@Slf4j
@Component
public class TaskInfoMessagerImpl implements TaskInfoMessager {

    @Override
    public void notifyTaskInfoCreate(TaskInfoDto taskInfoDto) {
        log.debug("notify task info create :{}", taskInfoDto);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .content(taskInfoDto)
                .elementType(ElementType.TASK_INFO)
                .changeType(ChangeType.CREATE)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    @Override
    public void notifyTaskInfoDelete(TaskInfoDto taskInfoDto) {
        log.debug("notify task info delete :{}", taskInfoDto);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .content(taskInfoDto)
                .elementType(ElementType.TASK_INFO)
                .changeType(ChangeType.DELETE)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }

    @Override
    public void notifyTaskInfoUpdate(TaskInfoDto taskInfoDto) {
        log.debug("notify task info update :{}", taskInfoDto);
        ElementChangeNotification elementChangeNotification = ElementChangeNotification.builder()
                .content(taskInfoDto)
                .elementType(ElementType.TASK_INFO)
                .changeType(ChangeType.UPDATE)
                .build();
        ElementChangeMessager.publishChangeMessage(elementChangeNotification);
    }
}
