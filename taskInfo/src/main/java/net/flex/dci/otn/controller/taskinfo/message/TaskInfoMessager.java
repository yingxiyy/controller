package net.flex.dci.otn.controller.taskinfo.message;

import net.flex.dci.otn.controller.taskinfo.dto.TaskInfoDto;

/**
 * @version 1.0
 * @date 2022/5/5 17:02
 */
public interface TaskInfoMessager {

    void notifyTaskInfoCreate(TaskInfoDto taskInfoDto);

    void notifyTaskInfoDelete(TaskInfoDto taskInfoDto);

    void notifyTaskInfoUpdate(TaskInfoDto taskInfoDto);

}
