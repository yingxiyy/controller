/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.data.DataConvertors;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.DataTimeConvert;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TopologyDao;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.MandatoryNode;
import org.opendaylight.yang.gen.v1.http.nokia.com.cd.otc.policies.rev190319.route.restriction.MandatorySiteLink;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.risk.group.RiskGroup;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.risk.group.risk.group.Plane;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelCreationAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.RiskGroupInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class ParamCreaionBasic {

    public static final Set<String> ODU4_SUPPORT_SIGNALS = new HashSet<>(Arrays.asList(Prot100GE.class.getSimpleName(), Prot100GEFlexE.class.getSimpleName(), ProtOTU4.class.getSimpleName()));
    public static final Set<String> ODU2_SUPPORT_SIGNALS = new HashSet<>(
            Arrays.asList(Prot10GE.class.getSimpleName(), Prot10GEWan.class.getSimpleName(), ProtStm64.class.getSimpleName(), ProtOTU2.class.getSimpleName()));
    public static final Set<String> ODU2_SUPPORT_CLIENT_MEDIUMS = new HashSet<>(
            Arrays.asList(ETH10GBASELR.class.getSimpleName(), STM64XL64.class.getSimpleName(), STM64XI64.class.getSimpleName(), STM64XS64.class.getSimpleName()
            ));
    public static final Set<String> ODU4_SUPPORT_CLIENT_MEDIUMS = new HashSet<>(
            Arrays.asList(ETH100GBASELR4.class.getSimpleName(), ETH100GBASECWDM4.class.getSimpleName())
    );
//    private String opCardType=DEFAULT_OP3_CARD_TYPE;//todo
    public static final String DEFAULT_OP3_CARD_TYPE = "OLP3_3";//3条腿
    public static final String DEFAULT_OP2_CARD_TYPE = "OLP3";//2条腿
    public static final String DEFAULT_OT_CARD_TYPE = "T2X4C8";
    public static final Class<? extends ETHERNETCOMPLIANCECODE> DEFAULT_CLIENT_MEDIUM_TYPE = ETH100GBASELR4.class;

    //==================key value for doIt tunnel
    private String cardType;
    private Class<? extends ProtectionType> protectionType;

    protected Integer failureRate;

    private Map<String, Integer> failureRateVendors;

    public void setRiskGroupName(String riskGroupName) {
        this.riskGroupName = riskGroupName;
    }

    public void setPlaneName(String planeName) {
        this.planeName = planeName;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }

    private String riskGroupName;
    private String planeName;
    private String planeId;
    private String orderId;
    private String customer;
    protected int bundleNumber;
    private Node desSite;
    private Node srcSite;

    protected Class<? extends ETHERNETCOMPLIANCECODE> clientMedium = null;
    protected Class<? extends SignalProtocolType> linePortSignalRate = null;
    protected Class<? extends SignalProtocolType> tunnelSignalRate = null;

    private List<Link> possibleSiteLinks;
    protected int lineRateNumber;
    protected int clientRateNumber;
    private DateAndTime creationTime;
    protected OduGranularity tunnelOdu;
    protected Integer clientLineRate;   //基于颗粒度，可以装几个客户口速率

    //表示line的速率占用的odu速率
    private OduGranularity OdukType;
    //表示OCH上的最小ODU颗粒度
    private OduGranularity ochOduGranularity;

    //new attribute importing from byteDance requirement.
    protected Class<? extends ETHERNETCOMPLIANCECODE> clientMediumA = null;
    protected Class<? extends ETHERNETCOMPLIANCECODE> clientMediumZ = null;
    private SERVICETYPE serviceType;
    private GridType frequencyWidthOfLinePort = null;

    //===================


    public ParamCreaionBasic() {
        possibleSiteLinks = new LinkedList<>();
    }

    public void parser(TunnelCreationAttributes input) throws CommonException {
        SiteNodeDao siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);

        creationTime = DataTimeConvert.convertToDateAndTime(DataTimeConvert.long2date(System.currentTimeMillis()));
        if (input.getOrderId() == null) {
            orderId = "";
        } else {
            orderId = input.getOrderId();
        }

        cardType = input.getCardType();
        isSupportedOtCard(cardType);

        if (input.getClientPhysicalMedium() == null) {
            if (input.getClientPhysicalMediumA() == null && input.getClientPhysicalMediumZ() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "ClientPhysicalMediumA and ClientPhysicalMediumZ is mandatory");
            } else {
                clientMediumA = input.getClientPhysicalMediumA();
                clientMediumZ = input.getClientPhysicalMediumZ();

                clientMedium = clientMediumA;
            }
        } else {
            clientMedium = input.getClientPhysicalMedium();
            if (input.getClientPhysicalMediumA() == null) {
                clientMediumA = input.getClientPhysicalMedium();
            } else {
                clientMediumA = input.getClientPhysicalMediumA();
            }
            if (input.getClientPhysicalMediumZ() == null) {
                clientMediumZ = input.getClientPhysicalMedium();
            } else {
                clientMediumZ = input.getClientPhysicalMediumZ();
            }
        }

        String srcSiteId = input.getSrcSite().getValue();
        srcSite = siteNodeDao.getSiteNodeById(srcSiteId);
        if (srcSite == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "error source siteNode info");
        }

        String desSiteId = input.getDstSite().getValue();
        desSite = siteNodeDao.getSiteNodeById(desSiteId);
        if (desSite == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "error destination siteNode info");
        }

//    if (srcSiteId.equals(desSiteId)) {
//      throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//              "can't create tunnel in the same site");
//    }
        if (input.getLineSignalRate() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "missing mandatory param line port signal rate ");
        }
        if (input.getSignalRate() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "missing mandatory param tunnel signal rate ");
        }

        linePortSignalRate = input.getLineSignalRate();
        tunnelSignalRate = input.getSignalRate();

        if (input.getServiceType() == null) {
            //兼容原来版本
            serviceType = getServiceTypeWithLine_ClientSignalRate();
        } else {
            serviceType = input.getServiceType();
        }
        if (input.getFrequenceWidth() == null) {
            frequencyWidthOfLinePort = GridType._75;
        } else {
            frequencyWidthOfLinePort = input.getFrequenceWidth();
        }
        validFreuenceWith();
//        lineRateNumber = getNumByLineSignalRate(linePortSignalRate);
//        clientRateNumber = getNumByTunnelSignalRate(tunnelSignalRate);

        if (input.getBundleNumber() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "missing mandatory param bundle number ");
        }

        bundleNumber = input.getBundleNumber();
        tunnelOdu = DataConvertors.getOduGranularity(tunnelSignalRate);
//        if (tunnelOdu.equals(OduGranularity.Odu2) && !ODU2_SUPPORT_CLIENT_MEDIUMS.contains(clientMediumA.getSimpleName())) {
//            //C口光模块类型
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "ClientPhysicalMedium error, not match tunnelSignalRate");
//        }
//        if (tunnelOdu.equals(OduGranularity.Odu4) && !ODU4_SUPPORT_CLIENT_MEDIUMS.contains(clientMediumA.getSimpleName())) {
//            //C口光模块类型
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "ClientPhysicalMedium error,not match tunnelSignalRate");
//        }

        OdukType = DataConvertors.getOduGranularity(linePortSignalRate);
//        clientLineRate = OtCardType.parserOduMux(cardType, tunnelOdu, linePortSignalRate);
//        ochOduGranularity = OtCardType.parserOduGranularity(cardType);
        buildGranularity(serviceType);

        //因为create 和 computer是两个API，两次调用的时间间隔不确定，
        //所以在create的时候还是需要计算一次可用的siteLink
        List<RiskGroupInfo> riskGroupInfos = input.getRiskGroupInfo();
        if (riskGroupInfos == null || riskGroupInfos.get(0) == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "riskGroup info is mandatory");
        } else {
            for (RiskGroupInfo info : riskGroupInfos) {
                riskGroupName = info.getRiskGroupName();
                planeName = info.getPlaneName();
                planeId = info.getPlaneId();

               /* List<LinkId> wholeLinks = getAllSiteLinksMatchRiskPlane(riskGroupName, planeName);

                wholeLinks = filterSrcDstSiteLink(wholeLinks, input.getSrcSite(), input.getDstSite());
                wholeLinks = filterMandatroySiteLink(wholeLinks, input.getMandatorySiteLink());
                possibleSiteLinks = filterMandatorySiteNode(wholeLinks, input.getMandatoryNode());
                possibleSiteLinks = filterBasedGrid(possibleSiteLinks, linePortSignalRate);*/
            }
        }

       /* if (possibleSiteLinks.size() == 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Group[%s] and plane[%s] can't find valid route", riskGroupName,
                            planeName));
        }*/

        this.protectionType = input.getProtectionType();

        // failure-rate为空时按1处理：所有bundle可以落在同一组TD设备/OCH资源上。
        this.failureRate = input.getFailureRate() == null ? 1 : input.getFailureRate();
        if (this.failureRate <= 0) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "failureRate error, should > 0");
        }
        if (this.failureRate != null && this.failureRate > this.bundleNumber) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "failureRate error, should <= bundle number");
        }
        handleFailureRate(input);
    }

    private void handleFailureRate(TunnelCreationAttributes input) {
        failureRateVendors = new LinkedHashMap<>();
        for (VendorOccupationRate vendor : input.getVendorOccupationRate()) {
            int vendorBundles = vendor.getNumber();
            // failure-rate表达的是全量业务要拆成多少组互相隔离的TD设备/OCH资源。
            // 这里按厂商业务占比折算到每个厂商，避免多厂商场景下每个厂商都完整创建failure-rate组。
            // 每个厂商需要拆分出的failure group数 = ceil(厂商业务数 * 总failure-rate / 总业务数)
            int requiredFailureGroups = new BigDecimal(vendorBundles)
                    .multiply(new BigDecimal(this.failureRate))
                    .divide(new BigDecimal(bundleNumber), 10, RoundingMode.HALF_UP)
                    .setScale(0, RoundingMode.CEILING)
                    .intValue();

            failureRateVendors.put( TunnelUtil.createVendorProductKey(vendor.getVendorName(), vendor.getProductType()), requiredFailureGroups);
        }

    }

    private void validFreuenceWith() {
        switch (frequencyWidthOfLinePort) {
            case _50:
            case _75:
            case _100:
            case _150:
                return;
            default:
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "must be one of fixed grid value");
        }
    }

    private SERVICETYPE getServiceTypeWithLine_ClientSignalRate() {
        if (tunnelSignalRate.equals(Prot100GE.class)) {
            if (linePortSignalRate.equals(ProtOTUc2.class)) {
                return SERVICETYPE.MUX2x100G;
            } else if (linePortSignalRate.equals(ProtOTUc3.class)) {
                return SERVICETYPE.MUX3x100G;
            } else if (linePortSignalRate.equals(ProtOTUc4.class)) {
                return SERVICETYPE.MUX4x100G;
            } else if (linePortSignalRate.equals(ProtOTUc6.class)) {
                return SERVICETYPE.MUX6x100G;
            }
        }
        return SERVICETYPE.Unknown;
    }

    private void buildGranularity(SERVICETYPE serviceType) {
        switch (serviceType) {
            case MUX8x100G:
            case MUX8x100GS:
                clientLineRate = 8;
                ochOduGranularity = OduGranularity.Odu4;
                break;
            case MUX6x100G:
            case MUX6x100GS:
                clientLineRate = 6;
                ochOduGranularity = OduGranularity.Odu4;
                break;
            case MUX4x100G:
            case MUX4x100GS:
                clientLineRate = 4;
                ochOduGranularity = OduGranularity.Odu4;
                break;
            case MUX2x400G:
            case MUX2x400GS:
            case MUX2x400GT:
                clientLineRate = 2;
                ochOduGranularity = OduGranularity.Odu4x4;
                break;
            case MUX1x400G:
            case TR400G:
            case TR1x400G:
            case TR400GS:
                clientLineRate = 1;
                ochOduGranularity = OduGranularity.Odu4x4;
                break;
            case MUX3x100G:
                clientLineRate = 1;
                ochOduGranularity = OduGranularity.Odu4;
                break;
            case MUX2x100G:
                clientLineRate = 2;
                ochOduGranularity = OduGranularity.Odu4;
                break;
            default:
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "unknown service type: " + serviceType.name());
        }
    }

    private void isSupportedOtCard(String cardType) {

        if (cardType == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cardType is mandatory");
        }

        if (cardType.equals("L1X12C8-L")) {
            cardType = "L1X12C8_L";
        }

//        if (!cardType.equals(Constant.EquipmentClass.T2X2C4) &&
//                !cardType.equals(Constant.EquipmentClass.T2X4C8) &&
//                !cardType.equals(Constant.EquipmentClass.T2X6C12) &&
//                !cardType.equals(Constant.EquipmentClass.TMUX) &&
//                !cardType.equals(Constant.EquipmentClass.TMUX1) &&
//                !cardType.equals(Constant.EquipmentClass.TMUX2) &&
//                !cardType.equals(Constant.EquipmentClass.L1X12C8) &&
//                !cardType.equals(Constant.EquipmentClass.L1X12C8_L) &&
//                !cardType.equals(Constant.EquipmentClass.L3X8C7)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "cardType only support T2X2C4, T2X4C8, T2X6C12, TMUX, TMUX-1, TMUX-2, L1X12C8, L1X12C8-L, L3X8C7");
//        }
    }


    protected OduGranularity parseTunnelOdu() throws CommonException {
        if (this.cardType.equals("T2X2C4")) {
            if (this.tunnelSignalRate.equals(Prot100GE.class)) {
                return OduGranularity.Odu4;
            }
        } else if (this.cardType.equals("T2X4C8")) {
            if (this.tunnelSignalRate.equals(Prot100GE.class)) {
                return OduGranularity.Odu4;
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "not supported cardType");
    }

    protected int getNumByLineSignalRate(Class<? extends SignalProtocolType> signalType) throws CommonException {
        //配合card Type来的，以后很可能变化, 2.4,6 是基于100G客户侧ODU4的。如果客户侧变化，返回值也会变
        if (tunnelSignalRate.equals(Prot100GE.class)) {
            if (signalType.equals(ProtOTUc2.class)) {
                return 2;
            } else if (signalType.equals(ProtOTUc3.class)) {
                return 3;
            } else if (signalType.equals(ProtOTUc4.class)) {
                return 4;
            } else if (signalType.equals(ProtOTUc6.class)) {
                return 6;
            }
        }

        String msg = "line side is " + signalType + "client side is " + tunnelSignalRate;
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "mismatch client/line slot " + msg);
    }

    protected int getNumByTunnelSignalRate(Class<? extends SignalProtocolType> signalType) throws CommonException {
        //配合card Type来的，以后很可能变化
        if (signalType.equals(Prot100GE.class)) {
            return 1;
        }

        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "unsupported tunnel rate " + signalType);
    }

    /**
     * convert from linePortSignalRate
     *
     * @return
     */
    public OduGranularity getOdukType() throws CommonException {
        if (linePortSignalRate.equals(ProtOTUc2.class)) {
            return OduGranularity.Odu4x2;
        } else if (linePortSignalRate.equals(ProtOTUc3.class)) {
            return OduGranularity.Odu4x3;
        } else if (linePortSignalRate.equals(ProtOTUc4.class)) {
            return OduGranularity.Odu4x4;
        } else if (linePortSignalRate.equals(ProtOTUc6.class)) {
            return OduGranularity.Odu4x6;
        } else if (linePortSignalRate.equals(ProtOTUc8.class)) {
            return OduGranularity.Odu4x8;
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Unknown Line port signal rate " + linePortSignalRate.getSimpleName());
    }

    private List<LinkId> filterSrcDstSiteLink(List<LinkId> wholeLinks, NodeId srcSite, NodeId dstSite) {
        List<LinkId> filterOut = new ArrayList<>();
        for (LinkId linkId : wholeLinks) {
            if (linkId.getValue().contains(srcSite.getValue()) && linkId.getValue().contains(dstSite.getValue())) {
                filterOut.add(linkId);
            }
        }
        return filterOut;
    }

    /**
     * 基于UI输入mandatoryLink，提取特定的link
     *
     * @param wholeLinks
     * @param mandatorySiteLinkList
     * @return
     */
    private List<LinkId> filterMandatroySiteLink(List<LinkId> wholeLinks, List<MandatorySiteLink> mandatorySiteLinkList) throws CommonException {
        if (mandatorySiteLinkList == null || mandatorySiteLinkList.isEmpty()) {
            return wholeLinks;
        }

        List<LinkId> newLinkList = new ArrayList<>();
        for (MandatorySiteLink ml : mandatorySiteLinkList) {
            boolean found = false;
            for (LinkId linkId : wholeLinks) {
                if (linkId.getValue().equals(ml.getLinkId())) {
                    found = true;
                    newLinkList.add(new LinkId(ml.getLinkId()));
                    break;
                }
            }
            if (!found) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "required mandatory siteLink isn't match risk/plane " + ml.getLinkId());
            }
        }
        return newLinkList;
    }

    /**
     * 基于UI输入mandatoryNode，提取特定的link
     *
     * @param wholeList
     * @param MandatoryNodeList
     * @return required siteLink
     */
    private List<Link> filterMandatorySiteNode(List<LinkId> wholeList, List<MandatoryNode> MandatoryNodeList) {
        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        List<Link> siteLinkList = new ArrayList<>();
        for (LinkId siteLinkId : wholeList) {
            Link link = siteLinkDao.getSiteLinkById(siteLinkId.getValue());
            if (link != null) {
                siteLinkList.add(link);
            }
        }

        if (MandatoryNodeList == null || MandatoryNodeList.size() == 0) {

            return siteLinkList;
        }

        List<Link> filterOut = new ArrayList<>();
        for (Link siteLink : siteLinkList) {
            for (MandatoryNode mdNode : MandatoryNodeList) {
                if (existedInRoute(siteLink, mdNode)) {
                    filterOut.add(siteLink);
                    break;
                }
            }
        }
        return filterOut;
    }

    /**
     * remove based on siteLink grid and L port requirement
     *
     * @param possibleSiteLinks
     * @param linePortSignalRate
     * @return
     */
    private List<Link> filterBasedGrid(List<Link> possibleSiteLinks, Class<? extends SignalProtocolType> linePortSignalRate) {
        if (linePortSignalRate.equals(ProtOTUc4.class) || linePortSignalRate.equals(ProtOTUc6.class)) {
            //the site link's grid must be 75GHZ, the possible grid is flex or _75
            Iterator<Link> iter = possibleSiteLinks.iterator();
            while (iter.hasNext()) {
                Link link = iter.next();
                Site siteLinkAttr = link.getAugmentation(Link1.class).getSite();
                if (siteLinkAttr.getGrid().equals(GridType._0) || siteLinkAttr.getGrid().equals(GridType._75)) {
                    continue;
                } else {
                    iter.remove();
                }
            }
        }
        return possibleSiteLinks;
    }

    /**
     * based on siteLink's supporting link's ID because the link is phyLink, and the ID name is based on phyNode's ID
     *
     * @param siteLink
     * @param mdNode
     * @return
     */
    private boolean existedInRoute(Link siteLink, MandatoryNode mdNode) {
        for (SupportingLink sl : siteLink.getSupportingLink()) {
            if (sl.getLinkRef().getValue().contains(mdNode.getNodeId())) {
                return true;
            }
        }
        return false;
    }

    private List<LinkId> getAllSiteLinksMatchRiskPlane(String riskGroupName, String planeName) throws CommonException {
        TopologyDao topologyDao = SpringBeanFinder.getBean(TopologyDao.class);

        List<LinkId> siteLinkList = new LinkedList<>();
        RiskGroup riskGroup = topologyDao.getSiteTopoRiskGroup(riskGroupName);
        if (riskGroup == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "hasn't found required group " + riskGroupName);
        }
        if (riskGroup.getPlane() == null && planeName != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "hasn't found required plane " + planeName);
        }
        for (Plane plane : riskGroup.getPlane()) {
            if (planeName != null && !planeName.equals(plane.getPlaneName())) {
                continue;
            }
            siteLinkList.addAll(plane.getLinkRef());
        }
        return siteLinkList;
    }

    public Integer getOchNumber(Integer bundleNumber) {
        return (int) Math.ceil(bundleNumber / (double) clientLineRate);

    }

    public String getOpCardType() {
       if(protectionType.equals(ProtectionBidir1To1.class)){
           return DEFAULT_OP2_CARD_TYPE;
       }
       if(protectionType.equals(ProtectionBidir1To2.class)){
           return DEFAULT_OP3_CARD_TYPE;
       }
       return null;
    }
}
