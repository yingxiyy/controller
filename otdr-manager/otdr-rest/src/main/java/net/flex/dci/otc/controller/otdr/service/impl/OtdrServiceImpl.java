package net.flex.dci.otc.controller.otdr.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import java.math.BigInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otc.controller.otdr.manager.OtdrManager;
import net.flex.dci.otc.controller.otdr.manager.OtdrResultManager;
import net.flex.dci.otc.controller.otdr.service.BaseService;
import net.flex.dci.otc.controller.otdr.service.OtdrService;
import net.flex.dci.otc.controller.otdr.validator.OTDRRestValidator;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.SetOtdrBaseInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitors;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/30 11:09
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrServiceImpl extends BaseService implements OtdrService {


    private final OtdrManager otdrManager;

    private final OTDRRestValidator otdrValidator;

    private final OtdrResultManager otdrResultManager;

    @Override
    public String getOtdrResult(String input) {

        return null;
    }

    @Override
    public JSONObject getOtdrMonitorStatus(String input) throws CommonException {
        GetOtdrMonitorInput getOtdrMonitorInput = formRpcInput(input, GetOtdrMonitorInput.class);
        String refNodeId = getOtdrMonitorInput.getNodeId().getValue();
        String refCardId = getOtdrMonitorInput.getEquipId();
        log.debug("start to get otdr monitor for the ne:{} and refCardId:{}", refNodeId, refCardId);
        OtdrMonitors otdrMonitors = otdrManager.getOtdrMonitors(refNodeId, refCardId);
        GetOtdrMonitorOutput getOtdrMonitorOutput = new GetOtdrMonitorOutputBuilder().setOtdrMonitors(
                otdrMonitors).build();
        String returnString = formRpcOutput(getOtdrMonitorOutput);
        return JSON.parseObject(returnString);
    }

    @Override
    public Object startOtdrOutput(String input, String user) {
        log.debug("user:{} start to get otdr the input is :{}", user, input);
        TaskInfoMessage taskInfoMessage = TaskInfoMessage.builder()
                .actionTime(System.currentTimeMillis())
                .actionType(ActionType.otdr)
                .detail(input)
                .who(user)
                .resourceType(ResourceType.phyLink)
                .build();
        StartOtdrInput startOtdrInput = formRpcInput(input, StartOtdrInput.class);

        StartOtdrOutput output = otdrManager.startOtdr(startOtdrInput, taskInfoMessage);
        return JSON.parseObject(formRpcOutput(output));
    }

    @Override
    public void setOtdrBaseBenchmark(String requestBody) throws CommonException {
        log.info("set otdr base benchmark start");
        SetOtdrBaseInput setOtdrBaseInput = formRpcInput(requestBody, SetOtdrBaseInput.class);
        otdrValidator.validateSetOtdrBaseBenchmarkInput(setOtdrBaseInput);
        String linkId = setOtdrBaseInput.getLinkId();
        BigInteger taskId = setOtdrBaseInput.getTaskId();
        log.info("set the task:{}  for  the link :{} base otdr benchmark", taskId, linkId);
        otdrResultManager.setOtdrBaseBenchmark(linkId, taskId);
    }
}
