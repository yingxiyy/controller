/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.DataTimeConvert;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.common.util.Constant;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.info.ReallocateEquipment;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class CreateSiteLinkParam {

    //    private SiteLinkDao siteLinkDao;
//    private TopologyDao topologyDao;
//    private SiteNodeDao siteNodeDao;
    private static final Set<String> DEFAULT_PROTECTED_MODELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("4", "6")));
    private static final Set<String> DEFAULT_ALL_MODELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("2", "4", "6")));
    private static final String BONE20_ONE_TO_TWO_MODEL = "10";
    private ChangedObject changedObject;
    AllocatorConfig configuration = SpringBeanFinder.getBean(AllocatorConfig.class);
    public String yangModel;

    public final int DEFAULT_OT_LINE_TX_POWER = -1;
    public final Set<String> PROTECTED_MODELS = DEFAULT_PROTECTED_MODELS;
    public final Set<String> ALL_MODELS = DEFAULT_ALL_MODELS;  //2一般无保护，4 1:1 保护，6: 腾讯定义的一种扩展保护，最终没有支持
    public final Set<String> ALL_GROUPS = new HashSet<>(Arrays.asList("C", "L", "Separation", "Integration", "Band_Expansion"));

    //key attributes for siteLink
    //===================
    private String vendorName;
    private String vendorType;
    // Initial channel capacity supplied by UI; siteLink bandwidth becomes a remaining counter later.
    private Integer bandwidth;
    private boolean isProtected;
    private String modelType;
    private WDM_Band linkGroup;
    private String orderId;
    boolean spareFound = false;
    boolean slaveFound = false;
    private GridType grid;
    private String planeName;
    private String planeId;
    private String riskGroupName;
    private List<Segment> segments;
    private String srcSiteId;
    private String dstSiteId;

    private DateAndTime creationTime;
    private Map<String, Node> siteNodeMap = new HashMap<>();
    private List<Segment> mainSegment = new ArrayList<>();  //special for Method1
    private List<Segment> spareSegment = new ArrayList<>();   //special for Method1
    private List<Segment> slaveSegment = new ArrayList<>();

    private String linkModel;
    private Class<? extends ProtectionType> protectionType;

//    //===============key for reallocate=======
//    private ReuseResource reuseResource = null;
//    //===================


    public CreateSiteLinkParam(ChangedObject changedObject) {
        this.changedObject = changedObject;
        init();
    }

    public CreateSiteLinkParam() {
        this(new ChangedObject());
    }

    private void init() {
        this.yangModel = configuration.getYangModelString();
//        siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
//        topologyDao = SpringBeanFinder.getBean(TopologyDao.class);
//        siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
    }

    public LinkTerminationNodeType getSiteTypeInLink(String nodeId) {
        for (Segment segment : segments) {
            if (segment.getSource().equals(nodeId)) {
                return segment.getSourceNodeType();
            } else if (segment.getDestination().equals(nodeId)) {
                return segment.getDestinationNodeType();
            }
        }
        log.error("impossible, I can not find node in creation segments {}", nodeId);
        return LinkTerminationNodeType.SITE;
    }

    public String getSrcSiteName() {
        Node node = getSrcSiteNode();
        return node.getAugmentation(Node1.class).getSite().getFriendlyName();
    }

    public String getDstSiteName() {
        Node node = getDstSiteNode();
        return node.getAugmentation(Node1.class).getSite().getFriendlyName();
    }

    public Node getSrcSiteNode() {
        return siteNodeMap.get(srcSiteId);
    }

    public Node getDstSiteNode() {
        return siteNodeMap.get(dstSiteId);
    }

    public void parser(CreationParams input) throws CommonException {
        log.info("check CreateLinkInput start");
        creationTime = DataTimeConvert.convertToDateAndTime(DataTimeConvert.long2date(System.currentTimeMillis()));

        modelType = input.getLinkModel();
        if (modelType == null || "".equals(modelType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "modelType is mandatory");
        }

        orderId = input.getOrderId();
        if (orderId == null || "".equals(orderId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "orderId is mandatory");
        }
        if (orderId.length() > Constant.maxLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "orderId should be smaller than 255 characters");
        }
//        if (CommonUtil.checkSpace(orderId)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "orderId can't contain space");
//        }

        planeName = input.getPlaneName();
        planeId = input.getPlaneId();
        if (planeName == null || "".equals(planeName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "planeName is mandatory");
        }
        if (planeName.length() > Constant.maxLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "planeName should be smaller than 255 characters");
        }
//        if (CommonUtil.checkSpace(planeName)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "planeName can't contain space");
//        }

        riskGroupName = input.getRiskGroupName();
        if (riskGroupName == null || "".equals(riskGroupName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "riskGroupName is mandatory");
        }
        if (riskGroupName.length() > Constant.maxLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "riskGroupName should be smaller than 255 characters");
        }
//        if (CommonUtil.checkSpace(riskGroupName)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "riskGroupName can't contain space");
//        }

        int gridTmp = input.getFrequencyGrid();
        if (gridTmp != 0 && gridTmp != 50 && gridTmp != 75 && gridTmp != 100 && gridTmp != 150) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "error grid");
        }
        grid = GridType.forValue(gridTmp);

        if (input.getVendorOccupationRate() == null
                || input.getVendorOccupationRate().size() == 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "vendor info is mandatory");
        }
        vendorName = input.getVendorOccupationRate().get(0).getVendorName();
        vendorType = input.getVendorOccupationRate().get(0).getProductType();
        vendorType = ProductTypeResolver.resolveProductType(vendorName, vendorType);
        bandwidth = retrieveBandwidth(input);

        if (vendorName == null || "".equals(vendorName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "vendorName is mandatory");
        }
        if (vendorType == null || "".equals(vendorType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "vendorType is mandatory");
        }
//        if (!vendorType.equals("CHASSIS-2.0") && !vendorType.equals("CHASSIS-1.0") ) {
//            log.error("only support CHASSIS-2.0 and CHASSIS-1.0");
//        }

        validateLinkModelAndProtection();
        validateBone20LinkGroup(input.getLinkGroup());
        validateAndDefaultBandwidth();
        linkGroup = retrieveLinkBankGroup(input.getLinkGroup(), configuration.getYangModelString());
        segments = input.getSegment();
        if (segments == null || segments.size() == 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "segments is mandatory");
        }

        checkSegments(segments);

        linkModel = input.getLinkModel();
        this.protectionType = getProtectionType(input);

        log.info("siteLink product-type resolved to {}, linkGroup: {}, bandwidth: {}",
                vendorType, linkGroup, bandwidth);
        log.info("checkInput end");
    }

    private void validateBone20LinkGroup(String linkGroup) {
        if (isBone20() && !"C".equalsIgnoreCase(linkGroup)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Bone2.0 CHASSIS2.0 only supports C band siteLink");
        }
    }

    private Integer retrieveBandwidth(CreationParams input) {
        try {
            Object value = CreationParams.class.getMethod("getBandwidth").invoke(input);
            return value == null ? null : ((Number) value).intValue();
        } catch (ReflectiveOperationException e) {
            // Keep controller source compatible until the updated platform YANG artifact is installed.
            return null;
        }
    }

    private void validateAndDefaultBandwidth() {
        if (!isBone20()) {
            return;
        }
        int gridValue = grid.getIntValue();
        if (gridValue != 0 && gridValue != 75 && gridValue != 150) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Bone2.0 CHASSIS2.0 only supports Flex, 75GHz or 150GHz C-band siteLink");
        }
        if (gridValue != 0) {
            return;
        }
        if (bandwidth == null) {
            bandwidth = 64;
        }
        if (bandwidth != 32 && bandwidth != 64) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Bone2.0 CHASSIS2.0 Flex does not support bandwidth %s", bandwidth));
        }
    }

    private void validateLinkModelAndProtection() {
        isProtected = resolveIsProtectedModel(modelType, isBone20());
    }

    static boolean resolveIsProtectedModel(String modelType, boolean bone20ProductType) {
        // Keep 2606-release model validation for all existing products; model 10 is only the Bone2.0 OLP3-3 1:2 extension.
        boolean bone20OneToTwo = bone20ProductType && BONE20_ONE_TO_TWO_MODEL.equals(modelType);
        if (!DEFAULT_ALL_MODELS.contains(modelType) && !bone20OneToTwo) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "error OMS protection type");
        }
        return DEFAULT_PROTECTED_MODELS.contains(modelType) || bone20OneToTwo;
    }

    private boolean isBone20() {
        return ProductTypeResolver.isBone20ProductType(vendorName, vendorType);
    }

    private Class<? extends ProtectionType> getProtectionType(CreationParams input) {

        // Bone2.0 link-model 10 means OLP3-3 provides three legs, independent of route roles present in input.
        if (isBone20() && "10".equals(input.getLinkModel())) {
            return ProtectionBidir1To2.class;
        }
        Map<RoutingType, List<Segment>> segmentMap = input.getSegment().stream().collect(Collectors.groupingBy(segment -> segment.getRole()));
        if (segmentMap.containsKey(RoutingType.Third)) {
            return ProtectionBidir1To2.class;
        }
        if (segmentMap.containsKey(RoutingType.Slave)) {
            return ProtectionBidir1To1.class;
        }
        return ProtectionUnprotected.class;
    }

    private WDM_Band retrieveLinkBankGroup(String linkGroup, String yangModel) {

        if (!ALL_GROUPS.contains(linkGroup)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "未知的波段类型/unknown band type: " + linkGroup);
        }
        if (yangModel.equalsIgnoreCase(NeYangModel.ByteDance.getName())) {
            if (linkGroup.equalsIgnoreCase("Integration")) {
                return WDM_Band.fromString("C+L");
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("{}, 当前仅仅支持 C,L 一体的模式", NeYangModel.ByteDance.getName()));
            }
            return WDM_Band.fromString(linkGroup);
        } else if (!linkGroup.equalsIgnoreCase("C")) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("{}, 当前仅仅支持 C 波段", yangModel));
        }

        return WDM_Band.fromString("C");
    }

    public void parser(CreateLink2Input input) throws CommonException {
        CreateLinkInput createLinkInput = new CreateLinkInputBuilder(input).build();

        new ReuseNodeChecker(changedObject).checkInitialEnv(input.getReusedNodesSnapshot());
//        checkInitialEnv(input.getReusedNodesSnapshot());
        parser(createLinkInput);
//        if (input.getReallocateUiNeInfo() != null) {
//            parser(input.getReallocateUiNeInfo().getReallocateEquipment());
//        }
    }
//
//    /**
//     * 重用网元，需要确定现在的这些网元和初始情况一致
//     * snapshot 中的equip, 需要与当前数据库中读出来的值一致
//     *    * 因为UI看到的数据就是OP树的（如果有的话）,
//     *    * 一个equip(包括transceiver)被Link使用了由属性"used-in-link" = true 表达
//     *    * 数据合法性需要检查 板卡类型相同，used-in-link=false;
//     *
//     * @param reusedNodesSnapshot
//     */
//    private void checkInitialEnv(ReusedNodesSnapshot reusedNodesSnapshot) throws CommonException {
//        log.debug("start checking reuse snapshot");
//        reuseResource = new ReuseResource();
//        for (Nodes node : reusedNodesSnapshot.getConfig().getNodes()) {
//            reuseResource.addAndCheck(node);
//        }
//        for (Nodes node : reusedNodesSnapshot.getOp().getNodes()) {
//            reuseResource.addAndCheck(node);
//        }
//    }

    private void parser(List<ReallocateEquipment> reallocateEquipmentList) throws CommonException {
        if (reallocateEquipmentList == null) {
            return;
        }

        for (ReallocateEquipment reallocate : reallocateEquipmentList) {
            if (!PhysicalNodeIdNamingRule.getSiteId(reallocate.getNewNodeId()).equals(PhysicalNodeIdNamingRule.getSiteId(reallocate.getNewNodeId()))) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "reallocated Node should in same site");
            }
        }
    }


    private void checkSegments(List<Segment> segments) throws CommonException {

        Collections.sort(segments, new SortByIndex());

        Set<String> affectedSiteNodeId = new HashSet<>();

        for (Segment segment : segments) {
            affectedSiteNodeId.add(segment.getSource());
            affectedSiteNodeId.add(segment.getDestination());
            checkSegment(segment);
        }

//        if (slaveFound && !spareFound) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "site link in 1:3 model, must provide spare related segment");
//        }

        validateProtectionLegs(modelType, slaveFound, isProtected, isBone20());
        if (srcSiteId.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "source is mandatory");
        }

        if (dstSiteId == null || dstSiteId.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "destination is mandatory");
        }

        if (srcSiteId.equals(dstSiteId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "source coincide with destination");
        }

        Iterator<String> iter = affectedSiteNodeId.iterator();
        while (iter.hasNext()) {
            String siteId = iter.next();
            Node siteNode = changedObject.getChangedSiteNode(siteId);
            if (siteNode == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find site " + siteId);
            }
            siteNodeMap.put(siteId, siteNode);
        }
    }

    static void validateProtectionLegs(String modelType, boolean slaveFound, boolean isProtected,
            boolean bone20ProductType) {
        // Preserve 2606-release validation for non-Bone2.0 products; only CHASSIS2.0 model 10 owns this relaxation.
        boolean bone20OneToTwo = bone20ProductType && "10".equals(modelType);
        boolean missingRequiredSlave = !slaveFound && isProtected && !bone20OneToTwo;
        if ((slaveFound && !isProtected) || missingRequiredSlave) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "site link model isn't match with segments");
        }
    }

    private void checkSegment(Segment segment) {
        if (segment.getRole().equals(RoutingType.Main)) {
            mainSegment.add(segment);
        } else if (segment.getRole().equals(RoutingType.Third)) {
            spareFound = true;
            spareSegment.add(segment);
        } else if (segment.getRole().equals(RoutingType.Slave)) {
            slaveFound = true;
            slaveSegment.add(segment);
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("unknown segment role {%s}", segment.getRole().name()));
        }

        if (isTerminalSite(segment.getSourceNodeType()) && (srcSiteId == null || "".equals(srcSiteId))) {
            srcSiteId = segment.getSource();
        }
        if (isTerminalSite(segment.getDestinationNodeType()) && (dstSiteId == null || "".equals(dstSiteId))) {
            dstSiteId = segment.getDestination();
        }

        if (segment.getProvider() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Provider is mandatory");
        }
//        if (segment.getProvider().getAttenuation() == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Attenuation is mandatory");
//        }
        if (segment.getProvider().getAttenuationAz() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Attenuation-AZ is mandatory");
        }
        checkScaleLength(segment.getProvider().getAttenuationAz());
        if (segment.getProvider().getAttenuationZa() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Attenuation-ZA is mandatory");
        }
        checkScaleLength(segment.getProvider().getAttenuationZa());
        if (segment.getProvider().getFiberType() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "FiberType is mandatory");
        }
//        if (segment.getProvider().getDistance() == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Distance is mandatory");
//        }
        if (segment.getProvider().getDistanceAz() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Distance-AZ is mandatory");
        }
        if (segment.getProvider().getDistanceZa() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Distance-ZA is mandatory");
        }

        if (srcSiteId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "source is mandatory");
        }
        if (srcSiteId.equals(dstSiteId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "source coincide with destination in segment");
        }
    }

    private boolean isTerminalSite(LinkTerminationNodeType siteType) {

        return (siteType.equals(LinkTerminationNodeType.SITE) ||
                siteType.equals(LinkTerminationNodeType.OTM) ||
                siteType.equals(LinkTerminationNodeType.ROADM) ||
                siteType.equals(LinkTerminationNodeType.REG)
        );

    }

    private void checkScaleLength(BigDecimal numbers) throws CommonException {
        if (numbers.scale() > 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Scale Length of Attenuation is error");
        }
    }

    public Class<? extends ProtectionType> getProtectionType() {
        return protectionType;
    }

    public void setProtectionType(Class<? extends ProtectionType> protectionType) {
        this.protectionType = protectionType;
    }


    class SortByIndex implements Comparator<Segment> {

        @Override
        public int compare(Segment arg0, Segment arg1) {
            return arg0.getIndex().compareTo(arg1.getIndex());
        }
    }

}
