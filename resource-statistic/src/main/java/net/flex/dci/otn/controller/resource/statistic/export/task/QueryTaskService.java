package net.flex.dci.otn.controller.resource.statistic.export.task;

import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;

/**
 * 2026/7/11
 *
 * @author musa
 * @version 1.0
 **/
public interface QueryTaskService {

    String submitQueryTask(UnifiedResourceQueryParam unifiedResourceQueryParam,
            HttpServletRequest request);


    String submitQueryTask(UnifiedQueryParam unifiedQueryParam, HttpServletRequest request);

}
