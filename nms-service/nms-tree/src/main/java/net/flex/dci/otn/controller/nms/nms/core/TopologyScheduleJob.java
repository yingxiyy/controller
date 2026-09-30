/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.springframework.stereotype.Component;

/**
 * getSchedulePaged
 *
 * deleteSchedules
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyScheduleJob extends BaseNms {

    private static final String GET_SCHEDULE_PAGED = "nms:get-schedule-paged";

    private static final String DEL_SCHEDULE = "nms:delete-schedules";

//    @Autowired
//    private ScheduleJobHandler scheduleJobs;

    public TopologyScheduleJob(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        if (cmd.equals(GET_SCHEDULE_PAGED)) {
            returnValue = getSchedulePaged(cmd, requestBody);
//        } else if (cmd.equals(DEL_SCHEDULE)) {
//            returnValue = delSchedule(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException("unsupported nms operations cmd " + cmd);
        }
        return returnValue;
    }

//    /**
//     * delete schedule job
//     *
//     * @param cmd
//     * @param requestBody
//     * @return
//     */
//    private String delSchedule(String cmd, String requestBody) throws CommonException {
//        try {
//            log.info("start to del schedule job");
//            DeleteSchedulesInput input = parseInput(cmd, requestBody, DeleteSchedulesInput.class);
//            DeleteSchedulesOutput output = this.scheduleJobs.deleteSchedules(input);
//            return serializeDataObject(cmd, output);
//        } catch (Exception ex) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "failed to delete schedule job the reason is:" + ex.getMessage());
//        }
//    }

    /**
     * execute get schedule paged
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getSchedulePaged(String cmd, String requestBody) throws CommonException {
//        try {
//            GetSchedulePagedInput input = parseInput(cmd, requestBody, GetSchedulePagedInput.class);
//            GetSchedulePagedOutput output = this.scheduleJobs.getSchedulePaged(input);
//            return serializeDataObject(cmd, output);
//        } catch (Exception ex) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "failed to get schedule job,the reason is:" + ex.getMessage());
//        }
        return null;
    }
}
