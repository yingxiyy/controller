/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otn.controller.nms.nms.dto.FrequencyMapDto;
import net.flex.dci.otn.controller.nms.nms.handler.ResourceHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.SendResponseUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardTypeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEnvPropertyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapSiteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetObjectDetailsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetObjectDetailsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetVersionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RemoveResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResynchronizeNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResynchronizeNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * getResourceByOrderId rec recycleResourceByOrderId
 *
 * removeResourceByOrderId
 *
 * get-termination-point
 *
 * getFriendlyName
 *
 *
 * updateNodeLocation
 *
 * getTransceiverByTp
 *
 * getVersion
 *
 * resynchronizeNe
 *
 * syncEquipment
 *
 * syncTerminationPoint
 *
 * getMuxSpectrum
 *
 * getWssChannel
 *
 * getRegionList
 *
 *
 * getZoneList
 *
 * getCampusList
 *
 * getEnvProperty
 *
 * getFrequencyMap
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologyResource extends BaseNms {

    private static final String GET_VERSION_CMD = "nms:get-version";
    private static final String GET_CARD_TYPE = "nms:get-card-type";
    private static final String GET_OT_CARD_CAPABILITY = "nms:get-ot-card-capability";
    private static final String GET_FREQ_MAP = "nms:get-frequency-map";
    private static final String GET_FREQ_MAP_SITE_LINK = "nms:get-frequency-map-site-link";
    private static final String GET_UNIT_LIST = "nms:get-unit-list";
    private static final String GET_WSS_CHANNEL = "nms:get-wss-channel";
    private static final String GET_MUX_SPEC = "nms:get-mux-spectrum";
    private static final String GET_MUX_SPEC_WITH_SITE_LINKS = "nms:get-mux-spectrum-with-site-link";
    private static final String GET_TRANSCEIVER_BY_TP = "nms:get-transceiver-by-tp";
    private static final String GET_FRIENDLY_NAME = "nms:get-friendly-name";
    private static final String REMOVE_RESOURCE_BY_ORDER_ID = "nms:remove-resource-by-order-id";
    private static final String GET_SCAN_TP = "nms:get-scan-tp";
    private static final String RECYCLE_RESOURCE_BY_ORDER_ID = "nms:recycle-resource-by-order-id";
    private static final String GET_RESOURCE_BY_ORDER_ID = "nms:get-resource-by-order-id";
    private static final String GET_ENV_PROPERTY = "nms:get-env-property";
    private static final String RE_SYNC_NE = "nms:resynchronize-ne";
    private static final String SYNC_EQ = "nms:sync-equipment";
    private static final String GET_CITY_LIST = "nms:get-city-list";
    private static final String GET_OBJECT_DETAILS = "nms:get-object-details";

    @Autowired
    private ResourceHandler resourceHandler;

    public TopologyResource(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        switch (cmd) {
            case GET_VERSION_CMD:
                returnValue = executeGetVersionCmd(cmd);
                break;
            case GET_CARD_TYPE:
                returnValue = executeGetCardType(cmd, requestBody);
                break;
            case GET_OT_CARD_CAPABILITY:
                returnValue = executeGetOTCapability(cmd);
                break;
            case GET_FREQ_MAP:
                returnValue = executeGetFreqMap(cmd, requestBody);
                break;
            case GET_FREQ_MAP_SITE_LINK:
                returnValue = executeGetFreqMapSiteLink(cmd, requestBody);
                break;
            case GET_UNIT_LIST:
                returnValue = executeGetUnitList(cmd, requestBody);
                break;
            case GET_WSS_CHANNEL:
                returnValue = executeGetWSSChannel(cmd, requestBody);
                break;
            case GET_MUX_SPEC:
                returnValue = executeGetMuxSpec(cmd, requestBody);
                break;
            case GET_MUX_SPEC_WITH_SITE_LINKS:
                returnValue = executeGetMuxSpecWithSiteLinks(cmd, requestBody);
                break;
            case GET_TRANSCEIVER_BY_TP:
                returnValue = executeGetTransceiverByTP(cmd, requestBody);
                break;
            case GET_FRIENDLY_NAME:
                returnValue = executeGetFriendName(cmd, requestBody);
                break;
            case REMOVE_RESOURCE_BY_ORDER_ID:
                returnValue = executeRemoveResource(cmd, requestBody);
                break;
            case GET_SCAN_TP:
                returnValue = executeScanTp(cmd, requestBody);
                break;
            case RECYCLE_RESOURCE_BY_ORDER_ID:
                returnValue = executeRecycleResource(cmd, requestBody);
                break;
            case GET_RESOURCE_BY_ORDER_ID:
                returnValue = executeGetResourceByOrderId(cmd, requestBody);
                break;
            case GET_ENV_PROPERTY:
                returnValue = executeGetEnvProperty(cmd, requestBody);
                break;
            case RE_SYNC_NE:
                returnValue = executeReSynchNe(cmd, requestBody);
                break;
            case GET_OBJECT_DETAILS:
                returnValue = executeGetObjectDetails(cmd, requestBody);
                break;
            default:
                throw new UnsupportedOperationException("unknown nms operations for resources");
        }

        return returnValue;
    }

    private String executeGetObjectDetails(String cmd, String requestBody) {
        log.info("execute get object details the request body is:{}", requestBody);
        try {
            GetObjectDetailsInput input = parseInput(cmd, requestBody, GetObjectDetailsInput.class);
            List<ObjectDetail> names = this.resourceHandler
                    .getObjectsDetails(input);
            GetObjectDetailsOutputBuilder outputBuilder = new GetObjectDetailsOutputBuilder();
            outputBuilder.setObjectDetail(names);

            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get friendly name reason is:" + ex.getMessage());
        }
    }

    private String executeReSynchNe(String cmd, String requestBody) {
        log.info("start to re sync ne the request body is:{} ", requestBody);
        try {
            ResynchronizeNeInput input = parseInput(cmd,
                    requestBody, ResynchronizeNeInput.class);
            NeManagerRpc neManagerRpc = SpringBeanFinder.getBean(NeManagerRpc.class);
            neManagerRpc.uploadNe(input.getNeId());
            ResynchronizeNeOutputBuilder output = new ResynchronizeNeOutputBuilder().setReturnCode(
                    RpcResultType.Success);

            return serializeDataObject(cmd, output.build());
        } catch (Exception ex) {
            log.error("failed to synchronized the ne,{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to synchronize ne");
        }
    }


    private String executeGetEnvProperty(String cmd, String requestBody) throws CommonException {
        log.info("get env property the request body is:{}", requestBody);
        try {
            GetEnvPropertyOutput envPropertyOutput = this.resourceHandler.getEnvProperty();
            return serializeDataObject(cmd, envPropertyOutput);
//            return null;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "get env property failed the reason is " + ex.getMessage());
        }
    }

    private String executeGetResourceByOrderId(String cmd, String requestBody)
            throws CommonException {
        log.info("get resource by order id");
        try {
            GetResourceByOrderIdInput input = parseInput(cmd,
                    requestBody, GetResourceByOrderIdInput.class);
            GetResourceByOrderIdOutput output = this.resourceHandler.getResourceByOrderId(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "get resource by order id failed the reason is " + ex.getMessage());
        }
    }

    private String executeRecycleResource(String cmd, String requestBody) throws CommonException {
        log.info("recycle resource by order id");
        try {
            RecycleResourceByOrderIdInput input = parseInput(cmd,
                    requestBody, RecycleResourceByOrderIdInput.class);
            RecycleResourceByOrderIdOutput output = this.resourceHandler
                    .recycleResourceByOrderId(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "recycle resource by order id failed the reason is " + ex.getMessage());
        }
    }

    private String executeRemoveResource(String cmd, String requestBody) throws CommonException {
        log.info("remove resource by order id ");
        try {
            RemoveResourceByOrderIdInput input = parseInput(cmd,
                    requestBody, RemoveResourceByOrderIdInput.class);
            this.resourceHandler.removeResourceByOrderId(input);
            return SendResponseUtils.successNMSResult();
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "remove resource by order id failed the reason is " + ex.getMessage());
        }
    }

    private String executeScanTp(String cmd, String requestBody) {
        log.debug("get scan TP");
        try {
            GetScanTpInput input = parseInput(cmd,
                    requestBody, GetScanTpInput.class);
            GetScanTpOutput output = this.resourceHandler.getScanTerminationPoint(input);

            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "get TP failed the reason is " + ex.getMessage());
        }
    }


    private String executeGetFriendName(String cmd, String requestBody) throws CommonException {
        try {
            GetFriendlyNameInput input = parseInput(cmd, requestBody, GetFriendlyNameInput.class);
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object> names = this.resourceHandler
                    .getFriendName(input);
            GetFriendlyNameOutputBuilder outputBuilder = new GetFriendlyNameOutputBuilder();
            outputBuilder.setObject(names);

            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get friendly name reason is:" + ex.getMessage());
        }
    }

    private String executeGetTransceiverByTP(String cmd, String requestBody)
            throws CommonException {
        try {
            GetTransceiverByTpInput input = parseInput(cmd, requestBody,
                    GetTransceiverByTpInput.class);
            GetTransceiverByTpOutput output = this.resourceHandler.getTransceiverByTp(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get transceiver by tp the reason is:" + ex.getMessage());
        }
    }

    private String executeGetMuxSpec(String cmd, String requestBody) throws CommonException {
        log.info("get mux spec ");
        try {
            GetMuxSpectrumInput input = parseInput(cmd, requestBody, GetMuxSpectrumInput.class);
            GetMuxSpectrumOutput output = this.resourceHandler.getMuxSpectrum(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get mux spec the reason is " + ex.getMessage());
        }
    }

    private String executeGetMuxSpecWithSiteLinks(String cmd, String requestBody)
            throws CommonException {
        log.info("get mux spec with site links");
        try {
            GetMuxSpectrumWithSiteLinkInput input = parseInput(cmd, requestBody,
                    GetMuxSpectrumWithSiteLinkInput.class);
            GetMuxSpectrumWithSiteLinkOutput output = this.resourceHandler.getMuxSpectrumWithSiteLinks(
                    input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get mux spec the reason is" + ex.getMessage());
        }
    }

    private String executeGetWSSChannel(String cmd, String requestBody) throws CommonException {
        log.info("get wss unit list input {},", requestBody);
        try {
            GetWssChannelInput input = parseInput(cmd, requestBody, GetWssChannelInput.class);
            GetWssChannelOutputBuilder outputBuilder = new GetWssChannelOutputBuilder();
            GetWssChannelOutput wssChannel = this.resourceHandler
                    .getWssChannel(input);
            outputBuilder.setChannel(wssChannel.getChannel());
            outputBuilder.setUpdateRange(wssChannel.getUpdateRange());
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get wss channel the reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get wss channel the reason is:" + ex.getMessage(), ex);
        }
    }

    private String executeGetUnitList(String cmd, String requestBody) throws CommonException {
//        log.info("get unit list input {}", requestBody);
//        GetUnitListInput input = parseInput(cmd, requestBody);
//        GetUnitListOutputBuilder outputBuilder = new GetUnitListOutputBuilder();
//        outputBuilder.setUnitInfo(resourceHandler.getUnitList(input));
//        return serializeDataObject(cmd, outputBuilder.build());
        return null;
    }

    private String executeGetFreqMap(String cmd, String requestBody) throws CommonException {
        log.info("get frequency map,requestBody:{}", requestBody);
        try {
            GetFrequencyMapInput input = parseInput(cmd, requestBody, GetFrequencyMapInput.class);
            GetFrequencyMapOutputBuilder outputBuilder = new GetFrequencyMapOutputBuilder();
//            FrequencyMap map = resourceHandler.getFrequencyMap(input);
            FrequencyMapDto map = resourceHandler.getFrequencyMapDto(input);
            outputBuilder.setGrid(map.getGrid());
            outputBuilder.setMap(map.getMapList());
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get freq map,exception is :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get freq map,the reason is " + ex.getMessage(), ex);
        }
    }

    private String executeGetFreqMapSiteLink(String cmd, String requestBody)
            throws CommonException {
        log.info("get frequency map on site link:{}", requestBody);
        try {
            GetFrequencyMapSiteLinkInput input = parseInput(cmd, requestBody,
                    GetFrequencyMapSiteLinkInput.class);
            GetFrequencyMapSiteLinkOutputBuilder outputBuilder = new GetFrequencyMapSiteLinkOutputBuilder();
//            FrequencyMap map = resourceHandler.getFrequencyMap(input);
            FrequencyMapDto map = resourceHandler.getFrequencyMapBySiteLink(input);
            outputBuilder.setGrid(input.getGrid());
            outputBuilder.setMap(map.getMapList());
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get freq map,exception is :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get freq map,the reason is " + ex.getMessage(), ex);
        }
    }

    private String executeGetOTCapability(String cmd) throws CommonException {
        log.info("get card capability");
//        GetOtCardCapabilityOutputBuilder outputBuilder = new GetOtCardCapabilityOutputBuilder();
//        outputBuilder.setCardType(resourceHandler.getOtCardCapability());
//        return serializeDataObject(cmd, outputBuilder.build());
        return null;
    }

    /**
     * get card type
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String executeGetCardType(String cmd, String requestBody) throws CommonException {
        log.info("get card type ");
        GetCardTypeInput input = parseInput(cmd, requestBody, GetCardTypeInput.class);
        GetCardTypeOutputBuilder outputBuilder = new GetCardTypeOutputBuilder();
        if (input.getCardClass() != null && input.getCardClass() == EquipType.OT) {
            List<String> cardList = this.resourceHandler.getCardType(input);
            outputBuilder.setCardType(cardList);
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "bad-attribute" +
                    "input card type is not supported.");
        }
        return serializeDataObject(cmd, outputBuilder.build());
    }

    /**
     * execute get version cmd
     *
     * @return
     */
    private String executeGetVersionCmd(String cmd) throws CommonException {
        try {
            GetVersionOutput output = this.resourceHandler.getVersion();
            return serializeDataObject(cmd, output);
        } catch (Exception e) {
            log.error("Failed to get controller version info", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to get controller version info");
        }
    }
}
