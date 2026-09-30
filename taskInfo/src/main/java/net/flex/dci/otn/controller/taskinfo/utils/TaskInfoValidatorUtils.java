package net.flex.dci.otn.controller.taskinfo.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.db.jpa.service.dao.TaskInfoDaoService;

/**
 * @version 1.0
 * @date 2022/4/16 13:10
 */
@Slf4j
public class TaskInfoValidatorUtils {

    private static final TaskInfoDaoService taskDaoService;

    static {
        taskDaoService = SpringBeanFinder.getBean(TaskInfoDaoService.class);
    }

    public static void validateTaskInfoId(Long id) {
        log.debug("start to validate task Info id is:{}", id);
        boolean existed = taskDaoService.existsById(id);
        if (!existed) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("can not find the task info id:{%s}", id));
        }
    }

}
