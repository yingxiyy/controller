package net.flex.dci.otc.controller.otdr.service.impl;

import com.alibaba.fastjson.JSON;
import java.math.BigInteger;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.otdr.components.OTDRGraphics;
import net.flex.dci.otc.controller.otdr.components.OTDRQueryParser;
import net.flex.dci.otc.controller.otdr.manager.OtdrResultManager;
import net.flex.dci.otc.controller.otdr.model.LatestOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.breifly.OtdrBrieflyResultOutput;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrRes;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrResOutput;
import net.flex.dci.otc.controller.otdr.model.graphics.ShowOtdrResults;
import net.flex.dci.otc.controller.otdr.model.otsLink.GetOmsLinkOtdrLatestResultOutputDto;
import net.flex.dci.otc.controller.otdr.service.BaseService;
import net.flex.dci.otc.controller.otdr.service.OtdrResultService;
import net.flex.dci.otc.controller.otdr.utils.OtdrConstants;
import net.flex.dci.otc.controller.otdr.validator.OTDRRestValidator;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrDetailInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrDetailOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.ShowOtdrGraphicsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.get.otdr.detail.output.CurrentBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.task.rev191210.GetOmsLinkOtdrLatestResultInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkRole;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/31 17:20
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrResultServiceImpl extends BaseService implements OtdrResultService {

    private final OtdrResultManager otdrResultManager;

    private final OTDRQueryParser otdrQueryParser;

    private final OTDRRestValidator otdrValidator;

    private final OTDRGraphics otdrGraphics;

    /**
     * get latest otdr latest result
     *
     * @param input
     * @return
     */
    @Override
    public LatestOtdrResOutput getOtdrLatestResult(String input) {
        GetOtdrResInput getInput = formRpcInput(input, GetOtdrResInput.class);
        String monitorTpId = getInput.getTpId();
        log.info("start to get latest otdr the tp id is :{}", monitorTpId);
        LatestOtdrResOutput latestOtdrResOutput = otdrResultManager.getLatestOTDRResult(
                monitorTpId);
        return latestOtdrResOutput;
//        OtdrResultRecord otdrResult = otdrResultManager.getLatestOtdrResult(
//                monitorTpId);
//        if (otdrResult != null) {
//            String output = otdrResult.getContent();
//            return JSON.parseObject(output);
//        } else {
//            return "";
//        }
    }

//    @Override
//    public Object getOTDRResults(String input) {
//        GetOtdrResultsInput getOtdrResultsInput = formRpcInput(input, GetOtdrResultsInput.class);
//        log.info("start to get otdr results for the phy link");
//        LinkId linkId = getOtdrResultsInput.getLinkId();
//        if (linkId == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "the link id is required. please try again!");
//        }
//        GetOtdrResultsOutput resultsOutput = otdrResultManager.getOtdrResultsPaged(
//                getOtdrResultsInput);
//        String jsonResult = formRpcOutput(resultsOutput);
//        return JSON.parseObject(jsonResult);
//    }

    @Override
    public Object getOTDRDetail(String input) {
        GetOtdrDetailInput getOtdrDetailInput = formRpcInput(input, GetOtdrDetailInput.class);
        log.info("get OTDR detail result");
        BigInteger taskId = getOtdrDetailInput.getTaskId();
        if (null == taskId) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the task id is required,please try again");
        }
        String otdrResultContent = otdrResultManager.getOtdrResultDetail(
                getOtdrDetailInput);
        log.debug("otdr result :{}", otdrResultContent);
        GetOtdrResultOutput getOtdrResultOutput = formRpcOutput(OtdrConstants.NAMESPACE,
                OtdrConstants.operation,
                otdrResultContent,
                GetOtdrResultOutput.class);
        CurrentBuilder currentBuilder = new CurrentBuilder();
        currentBuilder.fieldsFrom(getOtdrResultOutput);
        GetOtdrDetailOutputBuilder getOtdrDetailOutputBuilder = new GetOtdrDetailOutputBuilder();
        getOtdrDetailOutputBuilder.setCurrent(currentBuilder.build());
        String jsonResult = formRpcOutput(getOtdrDetailOutputBuilder.build());
        return JSON.parseObject(jsonResult);
    }

    @Override
    public ShowOtdrResOutput showOtdrResultGraphics(String requestBody) {
        log.debug("start to graphics the otdr result ,the request body is:{}", requestBody);
        ShowOtdrGraphicsInput showOtdrGraphicsInput = formRpcInput(requestBody,
                ShowOtdrGraphicsInput.class);
        otdrValidator.validateShowOtdrResultGraphicsInput(showOtdrGraphicsInput);
        List<String> taskIds = showOtdrGraphicsInput.getTaskIds();
        List<ShowOtdrRes> results = otdrGraphics.showOtdrGraphicsByTaskIds(taskIds);
        ShowOtdrResOutput showOtdrResOutput = ShowOtdrResOutput.builder()
                .output(ShowOtdrResults.builder().result(results).build())
                .build();
        return showOtdrResOutput;
    }

    @Override
    public OtdrBrieflyResultOutput getOTDRBrieflyResults(String input) {
        GetOtdrResultsInput getOtdrResultsInput = formRpcInput(input, GetOtdrResultsInput.class);
        log.info("start to get otdr results for the phy link");
        LinkId linkId = getOtdrResultsInput.getLinkId();
        if (linkId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the link id is required. please try again!");
        }
        OtdrBrieflyResultOutput resultsOutput = otdrResultManager.getOtdrBrieflyResultsPaged(
                getOtdrResultsInput);
        return resultsOutput;
    }

    @Override
    public GetOmsLinkOtdrLatestResultOutputDto getOMSLinkOtdrLatestResultOutput(String input) {
        GetOmsLinkOtdrLatestResultInput getOmsLinkOtdrLatestResultInput = formRpcInput(input,
                GetOmsLinkOtdrLatestResultInput.class);
        LinkId omsLinkId = getOmsLinkOtdrLatestResultInput.getOmsLinkId();
        LinkRole linkRole = getOmsLinkOtdrLatestResultInput.getLinkRole();
        if (omsLinkId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the oms link id is required,please try again!");
        }
        if (linkRole == null) {
            linkRole = LinkRole.Primary;
        }

        log.info("start to get oms link latest result ,oms link result is:{}",
                omsLinkId.getValue());
        GetOmsLinkOtdrLatestResultOutputDto getOmsLinkOtdrLatestResultOutput = otdrResultManager.getOMSRefOtsLinkLatestOtdrResult(
                omsLinkId.getValue(), linkRole);
//        GetOmsLinkOtdrLatestResultOutput output = buildGetOmsLinkLatestOtdrOutput(
//                getOmsLinkOtdrLatestResultOutput);
//        String jsonReturn = SerializeUtil.serializeRpcOutput2Json(output);
//        return JSON.parseObject(jsonReturn);
        return getOmsLinkOtdrLatestResultOutput;
    }

//    private GetOmsLinkOtdrLatestResultOutput buildGetOmsLinkLatestOtdrOutput(
//            GetOmsLinkOtdrLatestResultOutputDto getOmsLinkOtdrLatestResultOutput) {
//        OmsOtsLinkOtdrLatestRecord resultRecord = getOmsLinkOtdrLatestResultOutput.getOutput();
//        OmsLinkBuilder omsLinkBuilder = new OmsLinkBuilder();
//        omsLinkBuilder.setOmsLinkId(LinkId.getDefaultInstance(resultRecord.getOmsLinkId()));
//        omsLinkBuilder.setOmsLinkName(resultRecord.getOmsLinkName());
//        omsLinkBuilder.setAzRouteLength(resultRecord.getAZRouteLength());
//        omsLinkBuilder.setZaRouteLength(resultRecord.getZARouteLength());
//        omsLinkBuilder.setContractAzRouteLength(resultRecord.getContractAzRouteLength());
//        omsLinkBuilder.setContractZaRouteLength(resultRecord.getContractZaRouteLength());
//        List<OtsLinks> otsLinkResults = resultRecord.getOtsLinks().stream()
//                .map(this::buildOtsLinks).collect(
//                        Collectors.toList());
//        OtsResults otsResults = new OtsResultsBuilder().setOtsLinks(otsLinkResults).build();
//
//        GetOmsLinkOtdrLatestResultOutputBuilder outputBuilder = new GetOmsLinkOtdrLatestResultOutputBuilder();
//        outputBuilder.setOmsLink(omsLinkBuilder.build());
//        outputBuilder.setOtsResults(otsResults);
//        return outputBuilder.build();
//    }

//    private OtsLinks buildOtsLinks(OtsLinkOtdrLatestResult otsLinkLatestResult) {
//        log.debug("build ots link latest result ");
//        OtsLinksBuilder otsLinksBuilder = new OtsLinksBuilder();
//        otsLinksBuilder.setLinkId(LinkId.getDefaultInstance(otsLinkLatestResult.getLinkId()));
//        otsLinksBuilder.setLinkName(otsLinkLatestResult.getLinkName());
//        OtsDirectionOtdrResult azDirection = otsLinkLatestResult.getAToz();
//        OtsDirectionOtdrResult zaDirection = otsLinkLatestResult.getZToa();
//        //a to z
//        AToZBuilder aToZBuilder = new AToZBuilder();
//        aToZBuilder.setAttenuationDb(azDirection.getAttenuationDb());
//        aToZBuilder.setLengthKm(azDirection.getLength());
//        aToZBuilder.setContractAttenuationDb(azDirection.getContractAttenuationDb());
//        aToZBuilder.setContractLengthKm(azDirection.getContractLength());
//
//        ZToABuilder zToABuilder = new ZToABuilder();
//        zToABuilder.setAttenuationDb(zaDirection.getAttenuationDb());
//        zToABuilder.setLengthKm(zaDirection.getLength());
//        zToABuilder.setContractAttenuationDb(zaDirection.getContractAttenuationDb());
//        zToABuilder.setContractLengthKm(zaDirection.getContractLength());
//
//        otsLinksBuilder.setAToZ(aToZBuilder.build());
//        otsLinksBuilder.setZToA(zToABuilder.build());
//        return otsLinksBuilder.build();
//    }


}
