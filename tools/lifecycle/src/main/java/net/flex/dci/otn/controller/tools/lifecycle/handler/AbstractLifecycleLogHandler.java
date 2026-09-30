/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.handler;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.tools.lifecycle.dto.LifeCycleLog;
import net.flex.dci.otn.db.jpa.entity.LifecycleLogRecord;
import net.flex.dci.otn.db.jpa.repository.LifecycleLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/17 10:59
 */
@Component
@Slf4j
public class AbstractLifecycleLogHandler implements ILifecycleLog {

    @Autowired
    protected LifecycleLogRepository repo;

    @Override
    public void recordLog(LifeCycleLog lifeCycleLog) {
        log.info("test for lifecycle handler");

        LifecycleLogRecord lifecycleLogRecord = convert2LifeLogRecord(lifeCycleLog);
        repo.save(lifecycleLogRecord);
    }

    private LifecycleLogRecord convert2LifeLogRecord(LifeCycleLog lifeCycleLog) {

        LifecycleLogRecord lifecycleLogRecord = new LifecycleLogRecord();
        lifecycleLogRecord.setModule(lifeCycleLog.getOperModel());
        lifecycleLogRecord.setClientIp(lifeCycleLog.getClientIp());
        lifecycleLogRecord.setObjectId(lifeCycleLog.getObjectId());
        lifecycleLogRecord.setObjectType(lifeCycleLog.getObjectType());
        lifecycleLogRecord.setOperUrl(lifeCycleLog.getOper_url());
        lifecycleLogRecord.setOperation(lifeCycleLog.getOperation());
        lifecycleLogRecord.setCreateTimestamp(lifeCycleLog.getCreateTimestamp());
        lifecycleLogRecord.setUserId(lifeCycleLog.getSessionId());
        lifecycleLogRecord.setStatus(lifeCycleLog.getOperStatus());
        lifecycleLogRecord.setExc_message(lifeCycleLog.getExp_msg());
        lifecycleLogRecord.setOper_req_param(lifeCycleLog.getOper_req_param());
        lifecycleLogRecord.setOper_resp_param(lifeCycleLog.getOper_resp_param());
        lifecycleLogRecord.setOperationName(lifeCycleLog.getOperationName());
        return lifecycleLogRecord;
    }


}
