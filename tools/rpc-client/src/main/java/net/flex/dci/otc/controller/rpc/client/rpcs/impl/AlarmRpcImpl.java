/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.AlarmRpcCmd;
import net.flex.dci.otc.controller.rpc.client.dto.Alarm;
import net.flex.dci.otc.controller.rpc.client.rpcs.AlarmRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmAttributeNameType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ClearAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ClearAlarmInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ClearAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GenerateAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GenerateAlarmInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GenerateAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetCurrentAlarmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetCurrentAlarmsInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetCurrentAlarmsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.sort.query.params.SortInfos;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.sort.query.params.SortInfosBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/30 14:16
 */
@Component
@Slf4j
public class AlarmRpcImpl extends BasicRpc implements AlarmRpc {


    public AlarmRpcImpl() {
        NAMESPACE = "alarm";
        MODULE_NAME = "status";
    }


    @Override
    public void generateAlarm(Alarm alarm) throws CommonException {
        log.info("execute the generate alarm rpc,alarm record is {}", alarm);
        try {
            GenerateAlarmInput generateAlarmInput = covert2GenerateInput(alarm);
            String requestOp = AlarmRpcCmd.GENERATE_ALARM;
            String requestBody = formRpcInput(requestOp, generateAlarmInput);
            String responseBody = executeRequest(requestOp, requestBody);
            GenerateAlarmOutput generateAlarmOutput = (GenerateAlarmOutput) formRpcOutPut(requestOp,
                    responseBody);
            if (!generateAlarmOutput.getReturnCode().equals(RpcResultType.Success)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        generateAlarmOutput.getReturnMessage());
            }
            log.info("finish to generate alarm rpc");
        } catch (Exception ex) {
            log.error("failed to generate alarm ", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to generate alarm through the rpc api,the reason is :"
                            + ex.getMessage(), ex);
        }
    }

    @Override
    public void clearAlarm(String alarmId) {
        log.info("execute the clear alarm rpc,alarm id is {}", alarmId);
        try {
            ClearAlarmInput clearAlarmInput = new ClearAlarmInputBuilder().setAlarmId(alarmId)
                    .build();
            String requestOp = AlarmRpcCmd.CLEAR_ALARM;
            String requestBody = formRpcInput(requestOp, clearAlarmInput);
            String responseBody = executeRequest(requestOp, requestBody);
            ClearAlarmOutput output = (ClearAlarmOutput) formRpcOutPut(requestOp, responseBody);
            if (!output.getReturnCode().equals(RpcResultType.Success)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        output.getReturnMessage());
            }
            log.info("finish to clear the alarm");
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to clear alarm through the rpc api", ex);
        }
    }

    @Override
    public GetCurrentAlarmsOutput getAlarms() {
        log.info("execute the get current alarms rpc");
        SortInfos sortInfo = new SortInfosBuilder()
                .setAscending(true)
                .setSortName(AlarmAttributeNameType.CreationTime)
                .build();
        List<SortInfos> list = new LinkedList<>();
        list.add(sortInfo);
        GetCurrentAlarmsInput input = new GetCurrentAlarmsInputBuilder()
                .setStartPos(0)
                .setHowMany(20)
                .setSortInfos(list)
                .build();
        try {
            String requestOp = AlarmRpcCmd.GET_CURRENT_ALARMS;
            String requestBody = formRpcInput(requestOp, input);
            String responseBody = executeRequest(requestOp, requestBody);
            GetCurrentAlarmsOutput output = (GetCurrentAlarmsOutput) formRpcOutPut(requestOp,
                    responseBody);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get current alarms", ex);
        }
    }


    private GenerateAlarmInput covert2GenerateInput(Alarm alarm) {
        try {
            GenerateAlarmInputBuilder builder = new GenerateAlarmInputBuilder()
                    .setAlarmGroup(alarm.getAlarmGroup())
                    .setAlarmId(alarm.getAlarmId())
                    .setAlarmText(alarm.getAlarmText())
                    .setAlarmTypeId(alarm.getAlarmTypeId())
                    .setCreationTime(
                            BigInteger.valueOf(alarm.getCreationTime()))
                    .setCreationReceivedTime(
                            BigInteger.valueOf(alarm.getCreationTime()))
                    .setNmlKey(alarm.getNmlKey())
                    .setSa(alarm.getSa())
                    .setEquipmentRef(alarm.getComponentRef())
                    .setResourceRef(alarm.getResourceRef())
                    .setServerity(alarm.getSeverity());
            return builder.build();
        } catch (Exception e) {
            log.error("convert alarm error", e);
            return null;
        }
    }
}
