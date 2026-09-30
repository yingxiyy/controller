/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.service.impl;

import static net.flex.dci.otc.controller.status.util.Constants.EMPTY;

import com.google.gson.Gson;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.type.ActionType;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.controller.status.alarm.controller.AlarmOutputAdapter;
import net.flex.dci.otc.controller.status.alarm.service.AlarmService;
import net.flex.dci.otc.controller.status.alarm.service.extractor.AlarmQueryParamExtractor;
import net.flex.dci.otc.controller.status.alarm.service.handler.AlarmHandler;
import net.flex.dci.otc.controller.status.alarm.utils.AlarmConverterUtils;
import net.flex.dci.otc.controller.status.common.AsynchronousExecutor;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmResultDto;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import net.flex.dci.otn.db.jpa.service.dao.AlarmHistoryDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmSourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ClearAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ClearAlarmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GenerateAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GenerateAlarmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmDetailInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmDetailOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmStatisticsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetCurrentAlarmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetCurrentAlarmsInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetHistoryAlarmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.RefreshAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.RefreshAlarmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.RefreshDeviceAlarmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.attribute.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/10/21 14:58
 */
@Slf4j
@Component
public class AlarmServiceImpl implements AlarmService {

    @Autowired
    private AlarmDaoService alarmDaoService;
    @Autowired
    private JsonUtil jsonUtil;
    @Autowired
    private AdapterRpc adapterRpc;
    @Autowired
    private AdapterDao adapterDao;

    @Autowired
    private AlarmHandler alarmHandler;

    @Autowired
    private AlarmOutputAdapter alarmOutputAdapter;

    @Autowired
    private AlarmHistoryDaoService alarmHistoryDaoService;

    @Autowired
    private AlarmQueryParamExtractor alarmQueryParamExtractor;


    private final Gson gson = new Gson();


    public String statisticsAlarm() {
        log.info("start to get alarm statics ");

        Map<AlarmSeverity, Long> severityMap = alarmDaoService.staticsAlarmBySeverity();

        long cleared =
                severityMap.get(AlarmSeverity.Cleared) != null ? severityMap
                        .get(AlarmSeverity.Cleared)
                        : 0;
        long minor =
                severityMap.get(AlarmSeverity.Minor) != null ? severityMap.get(AlarmSeverity.Minor)
                        : 0;
        long unknown =
                severityMap.get(AlarmSeverity.Unknown) != null ? severityMap
                        .get(AlarmSeverity.Unknown)
                        : 0;
        long warning =
                severityMap.get(AlarmSeverity.Warning) != null ? severityMap
                        .get(AlarmSeverity.Warning)
                        : 0;
        long major =
                severityMap.get(AlarmSeverity.Major) != null ? severityMap.get(AlarmSeverity.Major)
                        : 0;
        long critical =
                severityMap.get(AlarmSeverity.Critical) != null ? severityMap
                        .get(AlarmSeverity.Critical)
                        : 0;

        GetAlarmStatisticsOutputBuilder builder = new GetAlarmStatisticsOutputBuilder();
        builder.setCleared(cleared);
        builder.setMinor(minor);
        builder.setUnknown(unknown);
        builder.setWarning(warning);
        builder.setMajor(major);
        builder.setCritical(critical);
        builder.setTotal(cleared + minor + unknown + warning + major + critical);
        return jsonUtil.fromDataObjectToJson(builder.build(), true);
    }

    @Override
    public String clearAlarm(String input) {
        log.info("start to clear  the alarm,req body is {}", input);
        ClearAlarmInput clearAlarmInput = (ClearAlarmInput) jsonUtil
                .fromJsonToDataObject(input, true);
        String alarmId = clearAlarmInput.getAlarmId();
        String nmlKey = clearAlarmInput.getNmlKey();
        alarmDaoService.deleteAlarmByNmlKeyAndAlarmId(nmlKey, alarmId);
        ClearAlarmOutputBuilder builder = new ClearAlarmOutputBuilder();
        builder.setReturnCode(RpcResultType.Success);
        return jsonUtil.fromDataObjectToJson(builder.build(), true);
    }

    @Override
    public String generateAlarm(String input) throws CommonException {
        log.info("start to generate alarm ,input is {}", input);
        GenerateAlarmInput generateAlarmInput = (GenerateAlarmInput) jsonUtil
                .fromJsonToDataObject(input, true);
        AlarmHistoryRecord alarm = new AlarmHistoryRecord();
        alarm.setAlarmId(generateAlarmInput.getAlarmId());
        alarm.setAlarmText(generateAlarmInput.getAlarmText());
        alarm.setSeverity(generateAlarmInput.getServerity());
        alarm.setAlarmGroup(generateAlarmInput.getAlarmGroup());
        alarm.setAlarmTypeId(generateAlarmInput.getAlarmTypeId());
        alarm.setComponentRef(
                generateAlarmInput.getResourceRef() != null ? generateAlarmInput.getResourceRef()
                        : "");
        alarm.setResourceRef(
                generateAlarmInput.getResourceRef() != null ? generateAlarmInput.getResourceRef()
                        : "");
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.add(Calendar.MONTH, -1);
        alarm.setCreationTime(calendar.getTime().getTime());
        alarm.setNmlKey(generateAlarmInput.getNmlKey());
        alarm.setNmlReceivedTime(new Date().getTime());
        alarm.setSa(generateAlarmInput.isSa());
        alarm.setArchivedTime(new Date().getTime());
        alarm.setClearedTime(new Date().getTime());
        alarm.setArchivedTime(new Date().getTime());
        alarm.setActionType(ActionType.Close);
        alarmHistoryDaoService.save(alarm);
        GenerateAlarmOutputBuilder builder = new GenerateAlarmOutputBuilder();
        builder.setReturnCode(RpcResultType.Success);
        return jsonUtil.fromDataObjectToJson(builder.build(), true);
    }

    @Override
    public String getAlarmsDetail(String input) throws CommonException {
        log.info("get the alarm detail info, input is {}", input);
        GetAlarmDetailInput getAlarmDetailInput = (GetAlarmDetailInput) jsonUtil
                .fromJsonToDataObject(input, true);
        Long alarmIndex = getAlarmDetailInput.getAlarmIndex();
        AlarmSourceType alarmSourceType = getAlarmDetailInput.getAlarmSource();
        Object alarmObject = null;
        alarmObject = alarmHandler.findAlarm(alarmIndex, alarmSourceType);
//        if (alarmSourceType == AlarmSourceType.Current) {
////            alarmObject = alarmDaoService.findAlarm(alarmIndex);
//            if (alarmObject == null) {
//                throw new CommonException(CommonExceptionType.ALARM_ERROR,
//                        String.format("Current alarm with id %s does not exist",
//                                String.valueOf(alarmIndex)));
//            }
//
//        } else {
//            alarmObject = alarmDaoService.findHistoryAlarm(alarmIndex);
//            if (alarmObject == null) {
//                throw new CommonException(CommonExceptionType.ALARM_ERROR,
//                        String.format("History alarm with id %s Adoes not exist",
//                                String.valueOf(alarmIndex)));
//            }
//        }
        GetAlarmDetailOutput output = AlarmConverterUtils.convert2AlarmDetails(alarmObject);
        return jsonUtil.fromDataObjectToJson(output, true);
    }

    @Override
    public String refreshAlarm(String input) throws CommonException {
        log.info("start to refresh alarm ,the input is {}", input);
        RefreshAlarmInput ipt = (RefreshAlarmInput) jsonUtil
                .fromJsonToDataObject(input, true);
        AsynchronousExecutor.execute(() -> {
            String neId = ipt.getNodeId();
            Adapter adapter = getAdapterByNeID(neId);
            if (adapter != null) {
                adapterRpc.refreshAlarm(adapter, neId);
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format(
                                "Failed to refresh alarm for ne %s because no adapter for it.",
                                neId));
            }
        });
        RefreshAlarmOutputBuilder outputBuilder = new RefreshAlarmOutputBuilder();
        outputBuilder.setReturnCode(RpcResultType.Success);
        return jsonUtil.fromDataObjectToJson(outputBuilder.build(), true);
    }

    @Override
    public String getAlarms(String input) throws Exception {
        log.info("start to get alarms conditional ,input is {}", input);
        GetAlarmsInput ipt = (GetAlarmsInput) jsonUtil
                .fromJsonToDataObject(input, true);
        GetCurrentAlarmsInputBuilder builder = new GetCurrentAlarmsInputBuilder();
        builder.fieldsFrom(ipt);
//        int startPos = ipt.getStartPos() != null ? ipt.getStartPos().intValue() : -1;
//        int recordNum = ipt.getHowMany() != null ? ipt.getHowMany().intValue() : -1;
//        Map<String, Object> params = alarmParamUtil.getParams(builder.build());
//        List<Map<String, String>> sorts = AlarmSortUtil.getSortList(ipt.getSortInfos());
//        List<AlarmHistoryRecord> alarmRecordList = (List<AlarmHistoryRecord>) alarmDaoService
//                .getAlarmByCondition(AlarmSearchType.ALL, startPos, recordNum, params, sorts);
        AlarmConditionDto alarmConditionDto = alarmQueryParamExtractor.parseQueryCondition(
                builder.build());
        if (alarmConditionDto.getNeIds() != null
                && alarmConditionDto.getNeIds().size() == 1
                && alarmConditionDto.getNeIds().contains(EMPTY)) {
            GetAlarmsOutputBuilder outputBuilder = new GetAlarmsOutputBuilder();
            outputBuilder.setTotalRecords(0);
            outputBuilder.setAlarm(Collections.emptyList());
            return jsonUtil.fromDataObjectToJson(outputBuilder.build(), true);
        }
        int offset = ipt.getStartPos() != null ? ipt.getStartPos() : 0;
        int limit = ipt.getHowMany() != null ? ipt.getHowMany() : 20;
        Page<AlarmHistoryRecord> pagedAlarms = alarmHandler.listAllAlarmsByConditionPaged(
                offset, limit, alarmConditionDto);
        GetAlarmsOutputBuilder outputBuilder = new GetAlarmsOutputBuilder();
//        outputBuilder.setTotalRecords((int) pagedAlarms.getTotalElements());
//        outputBuilder.setAlarm(alarmOutputAdapter.adapterHistoryAlarmListForAll(alarmRecordList));
        outputBuilder.setTotalRecords(
                (int) pagedAlarms.getTotalElements());
        outputBuilder.setAlarm(
                AlarmConverterUtils.convert2HistoryAlarmListForAll(pagedAlarms.toList()));
        return jsonUtil.fromDataObjectToJson(outputBuilder.build(), true);
    }

    @Override
    public String getHistoryAlarms(String input) throws Exception {
        log.info("start to get history alarms :{}", input);
        GetHistoryAlarmsInput ipt = (GetHistoryAlarmsInput) jsonUtil
                .fromJsonToDataObject(input, true);
        GetCurrentAlarmsInputBuilder builder = new GetCurrentAlarmsInputBuilder();
        builder.fieldsFrom(ipt);
        AlarmConditionDto alarmConditionDto = alarmQueryParamExtractor.parseQueryCondition(
                builder.build());
        if (alarmConditionDto.getNeIds() != null
                && alarmConditionDto.getNeIds().size() == 1
                && alarmConditionDto.getNeIds().contains(EMPTY)) {
            AlarmResultDto alarmResultDto = new AlarmResultDto();
            alarmResultDto.setTotalRecords(0L);
            alarmResultDto.setAlarm(Collections.emptyList());
            return gson.toJson(alarmResultDto);
        }
        int offset = ipt.getStartPos() != null ? ipt.getStartPos() : 0;
        int limit = ipt.getHowMany() != null ? ipt.getHowMany() : 20;
        Page<AlarmHistoryRecord> pagedAlarms = alarmHandler.listHistoryAlarmsByConditionPaged(
                offset, limit, alarmConditionDto);
        AlarmResultDto alarmResultDto = new AlarmResultDto();
        alarmResultDto.setTotalRecords(pagedAlarms.getTotalElements());

        alarmResultDto.setAlarm(AlarmConverterUtils.convert2HisAlarmDtoList(pagedAlarms.toList()));
        String result = gson.toJson(alarmResultDto);

        return result;
    }

    @Override
    public String getCurrentAlarms(String input) throws Exception {
        log.info("start to get current alarms :{}", input);
        long t0 = System.currentTimeMillis();
        GetCurrentAlarmsInput ipt = (GetCurrentAlarmsInput) jsonUtil
                .fromJsonToDataObject(input, true);
        long t1 = System.currentTimeMillis();
        AlarmConditionDto alarmConditionDto = alarmQueryParamExtractor.parseQueryCondition(ipt);
        if (alarmConditionDto.getNeIds() != null
                && alarmConditionDto.getNeIds().size() == 1
                && alarmConditionDto.getNeIds().contains(EMPTY)) {
            AlarmResultDto alarmResultDto = new AlarmResultDto();
            alarmResultDto.setTotalRecords(0L);
            alarmResultDto.setAlarm(Collections.emptyList());
            return gson.toJson(alarmResultDto);
        }
        int offset = ipt.getStartPos() != null ? ipt.getStartPos() : 0;
        int limit = ipt.getHowMany() != null ? ipt.getHowMany() : 20;
        long t2 = System.currentTimeMillis();
        Page<AlarmRecord> pagedAlarms = alarmHandler.listAlarmsByConditionPaged(
                offset, limit, alarmConditionDto);
//        GetCurrentAlarmsOutputBuilder builder = new GetCurrentAlarmsOutputBuilder();
//        builder.setTotalRecords((int) pagedAlarms.getTotalElements());
        AlarmResultDto alarmResultDto = new AlarmResultDto();
        alarmResultDto.setTotalRecords(pagedAlarms.getTotalElements());
        long t3 = System.currentTimeMillis();
        alarmResultDto.setAlarm(AlarmConverterUtils.convert2AlarmDtoList(pagedAlarms.toList()));
        String result = gson.toJson(alarmResultDto);
        long t4 = System.currentTimeMillis();
        log.info("[GET-CURRENT-ALARMS-MONITOR] offset={} limit={} total={}  "
                        + "parse={}ms sql={}ms convert={}ms json={}ms total={}ms",
                offset, limit, pagedAlarms.getTotalElements(),
                t1 - t0, t2 - t1, t3 - t2, t4 - t3, t4 - t0);
        return result;
    }

    @Override
    public String refreshDeviceAlarm() throws Exception {
        log.info("start to refresh device alarm");
        alarmHandler.refreshDeviceAlarm();
        RefreshDeviceAlarmOutputBuilder refreshDeviceAlarmOutputBuilder = new RefreshDeviceAlarmOutputBuilder();
        refreshDeviceAlarmOutputBuilder.setReturnCode(RpcResultType.Success);
        return jsonUtil.fromDataObjectToJson(refreshDeviceAlarmOutputBuilder.build(), true);
    }


    private Adapter getAdapterByNeID(String neId) {
        log.debug("get adapters for the ne,neId: {}", neId);
        List<Adapter> adapters = adapterDao.getAdapters();
        if (adapters != null) {
            for (Adapter adapter : adapters) {
                List<Ne> neList = adapter.getNe();
                if (neList != null) {
                    for (Ne ne : neList) {
                        if (ne.getNodeId().getValue().equals(neId)) {
                            return adapter;
                        }
                    }
                }
            }
        }
        return null;
    }
}
