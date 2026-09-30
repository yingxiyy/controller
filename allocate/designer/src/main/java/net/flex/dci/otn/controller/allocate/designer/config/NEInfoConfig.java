/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.OchProtectionType;
import net.flex.dci.otn.controller.allocate.designer.model.SiteLinkProtectionType;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.Ne;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetCardCapabilityInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetCardCapabilityOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetCardCapabilityOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetOchLinkProtectionTypeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetOchLinkProtectionTypeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetOchLinkProtectionTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetOtCardCapabilityOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetOtCardCapabilityOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSiteLinkProtectionTypeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSiteLinkProtectionTypeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSiteLinkProtectionTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSupportedFrequencyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSupportedFrequencyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetSupportedFrequencyOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetVendorListInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetVendorListOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.GetVendorListOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.card.capability.input.VendorOccupationRate;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.card.capability.output.CardVendor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.card.capability.output.CardVendorBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.CardType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.vendor.list.output.Vendor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.vendor.list.output.VendorBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.vendor.list.output.VendorKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.stereotype.Service;

@Service
@Slf4j
//todo Only support load json files from resources right now, later can imporve to support load json files dynamically.
public class NEInfoConfig {

    public static final String BYTEDANCE_2_YANG_MODEL = "ByteDance2.0";
    private static final String LEGACY_CHASSIS_PRODUCT_TYPE = "CHASSIS";

    @Value("${server.yangModel}")
    private String yangModel;


    private static final String PATTERN = "classpath*:*card.json";
    private static final List<String> SITE_LINK_PROTECTION_TYPES_FLEX = Arrays.asList(SiteLinkProtectionType.noProtection.name(), SiteLinkProtectionType.OMSP.name());
    private static final List<String> SITE_LINK_PROTECTION_TYPES_FLEX_WSS = Arrays.asList(SiteLinkProtectionType.noProtection.name());
    private static final List<String> SITE_LINK_PROTECTION_TYPES_Fix = Arrays.asList(SiteLinkProtectionType.noProtection.name(), SiteLinkProtectionType.OMSP.name(),
            SiteLinkProtectionType.OTSP.name());
    private static final List<String> OCH_LINK_PROTECTION_TYPES_wss = Arrays.asList(OchProtectionType.noProtection.name(), OchProtectionType.OCHP_OP6.name(), OchProtectionType.OCHP_OPS.name());

    private Map<String, NeInfo> neInfoMap = new HashMap<>();//vendorName-vendorType-nodeType is the key

    @Autowired
    private ResourceLoader resourceLoader;

    @PostConstruct
    public void load() {
        log.info("Begin to load NE info.");
        ObjectMapper mapper = new ObjectMapper();
        try {
            org.springframework.core.io.Resource[] resources = ResourcePatternUtils
                    .getResourcePatternResolver(resourceLoader).getResources(PATTERN);
            for (org.springframework.core.io.Resource jsonResource : resources) {
                InputStream jsonFile;
                Ne ne;
                String fileName;
                try {
                    jsonFile = jsonResource.getInputStream();
                    fileName = jsonResource.getFilename();
                    File extFile = new File(fileName);
                    if (extFile != null && extFile.exists()) {//外部配置优先
                        log.info("Loading the json file: {} outside of jar.", fileName);
                        try {
                            ne = mapper.readValue(extFile, Ne.class);
                        } catch (Exception e) {
                            log.error("Failed to load NE info json file by external resource: {}, use default resource instead", fileName, e);
                            ne = mapper.readValue(jsonFile, Ne.class);
                        }
                    } else {
                        ne = mapper.readValue(jsonFile, Ne.class);
                    }
                } catch (Exception e) {
                    log.error("Failed to load NE info json file by resource: {}", jsonResource, e);
                    continue;

                }
                if (!isLoadableYangModel(ne.getYangModel())) {
                    continue;
                }
                String key = createKey(fileName);
                log.info("Loading the json file: {}, and storing by key: {}", fileName, key);
                try {
                    neInfoMap.put(key, new NeInfo(ne, fileName));
                } catch (NeDesignerException e) {
                    log.error("Failed to load NE Info from resource:{}", fileName, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to load NE Info from resource.", e);
        }
    }


    private String createKey(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("-")).toUpperCase();
    }

    private boolean isLoadableYangModel(String model) {
        if (yangModel.equalsIgnoreCase(model)) {
            return true;
        }
        // Bone2.0 keeps yangModel=ByteDance2.0 inside its resource JSON, but runtime selection
        // is now based on product-type=CHASSIS2.0 rather than a persisted model property.
        return "ByteDance".equalsIgnoreCase(yangModel)
                && BYTEDANCE_2_YANG_MODEL.equalsIgnoreCase(model);
    }


    public NeInfo getNeInfo(String vendorName, String vendorType, String nodeType) throws NeDesignerException {
        String model = getYangModelKeyByProductType(vendorName, vendorType);
        String mapKey = getMapKey(vendorName, vendorType, model, nodeType);
        NeInfo neInfo = this.neInfoMap.get(mapKey);
        if (neInfo == null && ProductTypeResolver.isBone20ProductType(vendorName, vendorType)
                && !NodeType.OD.name().equals(nodeType)) {
            // Bone2.0 currently productizes OD resources only. TD card capability remains the
            // original CHASSIS resource until a CHASSIS2.0 TD JSON is added.
            neInfo = this.neInfoMap.get(getMapKey(vendorName, LEGACY_CHASSIS_PRODUCT_TYPE, "ByteDance", nodeType));
        }
        if (neInfo == null) {
            throw new NeDesignerException(
                    String.format("Failed to load NE info by key %s, vendorName: %s, vendorType:%s, nodeType:%s",
                            mapKey, vendorName, vendorType, nodeType));
        }
        return neInfo;
    }

    private String getYangModelKeyByProductType(String vendorName, String vendorType) {
        return ProductTypeResolver.isBone20ProductType(vendorName, vendorType)
                ? "ByteDance" : yangModel;
    }

    private String getMapKey(String vendorName, String vendorType, String nodeType) {
        return getMapKey(vendorName, vendorType, yangModel, nodeType);
    }

    private String getMapKey(String vendorName, String vendorType, String model, String nodeType) {
        return String.format("%s-%s-%s-%s", vendorName, vendorType, model, nodeType).toUpperCase();
    }

    private boolean isDefaultResourceModelKey(String key) {
        // Capability APIs are driven by loaded resource files. ByteDance2.0 has its own
        // product-type segment in the file name, so include it instead of appending aliases.
        return key.contains("-" + yangModel.toUpperCase() + "-")
                || "ByteDance".equalsIgnoreCase(yangModel)
                && key.contains("-" + BYTEDANCE_2_YANG_MODEL.toUpperCase() + "-");
    }

    private String getFilterKey(String vendorName, String vendorType, NodeType nodeType) {
        StringBuilder stringBuilder = new StringBuilder();
        if (vendorName != null) {
            stringBuilder.append(vendorName);
            stringBuilder.append("-");
        }
        if (vendorType != null) {
            stringBuilder.append(vendorType);
            stringBuilder.append("-");
        }
        if (nodeType != null) {
            stringBuilder.append(nodeType.name());
        }
        return stringBuilder.toString().toUpperCase();
    }

    /**
     * Note: Actually the cardType in input is the cardVendorType in NeInfo
     *
     * @param input
     * @return
     */
    public GetVendorListOutput getVendorList(GetVendorListInput input) {
        String filterKey = getFilterKey(input.getVendorName(), input.getProductType(), input.getNodeType());

        List<Vendor> vendorList = new ArrayList<>();

        for (Entry<String, NeInfo> entry : neInfoMap.entrySet()) {
            String key = entry.getKey();
            if (!isDefaultResourceModelKey(key)) {
                continue;
            }
            if (key.contains(filterKey)) {
                NeInfo neInfo = entry.getValue();
                String cardClass = input.getCardType();//note: Input here actually should provide card class, e.g. WSS, OA
                if (cardClass != null) {
                    List<Card> supportCards = neInfo.getCardInfo().get(cardClass);
                    if (supportCards != null) {
                        for (Card supportCard : supportCards) {
                            //note: here the vendor is card vendor, e.g. OA25
                            String cardVendor = supportCard.getVendorType();
                            vendorList.add(new VendorBuilder().setKey(new VendorKey(cardVendor, input.getVendorName()))
                                    .setProductType(cardVendor)
                                    .setVendorName(input.getVendorName()).build());
                        }
                    } else if (neInfo.hasCardByCardType(cardClass)) {//note: here cardClass is cardType
                        vendorList.add(new VendorBuilder().setKey(new VendorKey(neInfo.getNe().getProductType(), neInfo.getVendor()))
                                .setNodeType(NodeType.valueOf(neInfo.getNodeType()))
                                .setProductType(neInfo.getNe().getProductType())
                                .setVendorName(neInfo.getVendor()).build());
                    }
                } else {
                    vendorList.add(new VendorBuilder().setKey(new VendorKey(neInfo.getNe().getProductType(), neInfo.getVendor()))
                            .setNodeType(NodeType.valueOf(neInfo.getNodeType()))
                            .setProductType(neInfo.getNe().getProductType())
                            .setVendorName(neInfo.getVendor()).build());
                }
            }
        }
        return new GetVendorListOutputBuilder().setVendor(vendorList).build();

    }

    public GetSupportedFrequencyOutput getSupportedFrequency(GetSupportedFrequencyInput input) throws NeDesignerException {

        String productType = ProductTypeResolver.resolveProductType(input.getVendorName(),
                input.getProductType());
        NeInfo neInfo = getNeInfo(input.getVendorName(), productType, NodeType.OD.name());
        List<String> frequency = (new ArrayList<>(neInfo.getSupportedFrequency()));

        return new GetSupportedFrequencyOutputBuilder().setFrequency(frequency).build();
    }

    public GetOtCardCapabilityOutput getOtCardCapability() throws NeDesignerException {
        String filterKey = getFilterKey(null, null, NodeType.TD);

        List<CardType> otCard = new ArrayList<>();
        for (Entry<String, NeInfo> entry : neInfoMap.entrySet()) {
            String key = entry.getKey();
            if (!isDefaultResourceModelKey(key)) {
                continue;
            }
            if (key.contains(filterKey)) {
                NeInfo neInfo = entry.getValue();
                otCard.addAll(neInfo.getCardCapability());

            }
        }
        return new GetOtCardCapabilityOutputBuilder().setCardType(otCard).build();
    }

    public WDM_Band getOtCardWdmBand(String cardType) throws NeDesignerException {
        return WDM_Band.fromString(getOtCardCapability().getCardType().stream().filter(card -> card.getCardType().equals(cardType)).findAny().get().getWDMBand());
    }

    public GetSiteLinkProtectionTypeOutput getSiteLinkProtectionType(GetSiteLinkProtectionTypeInput input) throws NeDesignerException {
//        NeInfo neInfo = getNeInfo(DEFAULT_VENDOR_NAME, input.getProductType(), NodeType.OD.name());
//        int totalSlots = neInfo.getEmptyCard().getPossibleSlot().size();
        List<String> protectionType;

        switch (input.getGrid()) {
            case _0:
                if (input.isIsWss()) {
                    protectionType = SITE_LINK_PROTECTION_TYPES_FLEX_WSS;
                } else {
                    protectionType = SITE_LINK_PROTECTION_TYPES_FLEX;
                }

                break;
            default:
                protectionType = SITE_LINK_PROTECTION_TYPES_Fix;
                break;
        }

        return new GetSiteLinkProtectionTypeOutputBuilder().setProtectionType(protectionType).build();
    }


    public GetOchLinkProtectionTypeOutput getOchLinkProtectionType(GetOchLinkProtectionTypeInput input) {

        return new GetOchLinkProtectionTypeOutputBuilder().setProtectionType(OCH_LINK_PROTECTION_TYPES_wss).build();
    }

    public GetCardCapabilityOutput getCardCapability(GetCardCapabilityInput input) {
        if (input.getVendorOccupationRate().size() > 1) {
            //when multiVendor, no card should be provided
            new GetCardCapabilityOutputBuilder().setCardVendor(Collections.EMPTY_LIST).build();
        }
        VendorOccupationRate vendorOccupation = input.getVendorOccupationRate().get(0);
        String filterKey = getFilterKey(vendorOccupation.getVendorName(), vendorOccupation.getProductType(), vendorOccupation.getNodeType());

        List<CardVendor> cardVendors = new ArrayList<>();
        for (Entry<String, NeInfo> entry : neInfoMap.entrySet()) {
            String key = entry.getKey();
            if (!isDefaultResourceModelKey(key)) {
                continue;
            }
            if (key.contains(filterKey)) {
                NeInfo neInfo = entry.getValue();
                String cardClass = input.getCardClass();//note: Input here actually should provide card class, e.g. WSS, OA

                List<Card> supportCards = neInfo.getCardInfo().get(cardClass);
                if (supportCards != null) {
                    for (Card supportCard : supportCards) {
                        //note: here the vendor is card vendor, e.g. OP1
                        String cardVendor = supportCard.getVendorType();
                        String comments = supportCard.getComments() == null ? StringUtils.EMPTY : supportCard.getComments();
                        cardVendors.add(new CardVendorBuilder()
                                .setCardVendor(cardVendor)
                                .setComments(comments).build());
                    }
                }

            }
        }
        return new GetCardCapabilityOutputBuilder().setCardVendor(cardVendors).build();
    }
}
