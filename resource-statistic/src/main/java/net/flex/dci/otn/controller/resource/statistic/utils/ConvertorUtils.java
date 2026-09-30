package net.flex.dci.otn.controller.resource.statistic.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.mdoel.card.Card;
import net.flex.dci.otc.mongo.mdoel.card.Transceiver;
import net.flex.dci.otc.mongo.mdoel.lldp.Lldp;
import net.flex.dci.otn.controller.resource.statistic.export.CardCsv;
import net.flex.dci.otn.controller.resource.statistic.export.TransceiverCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.CardInfo;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.TransceiverInfo;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;
import org.springframework.beans.BeanUtils;

/**
 * @version 1.0
 * @date 12/24/2025 4:52 PM
 */
public class ConvertorUtils {

    public static Page<LLDPInfo> convert2LLdPInfoPaged(PageResult<Lldp> pageResult) {
        Page<LLDPInfo> result = Page.<LLDPInfo>builder().pageNo(pageResult.getPageNum())
                .pageSize(pageResult.getPageSize()).totalPages(
                        pageResult.getPages()).total(pageResult.getTotal())
                .records(convert2LlDPInfos(pageResult.getList())).build();
        return result;
    }

    private static List<LLDPInfo> convert2LlDPInfos(List<Lldp> lldps) {
//        List<LLDPInfo> lldpInfos = lldps.stream().map(ConvertorUtils::convert2LldpInfo)
//                .collect(Collectors.toList());
//        return lldpInfos;
        return new ArrayList<>();
    }

//    private static LLDPInfo convert2LldpInfo(Lldp lldp) {
//        return LLDPInfo.builder().age(lldp.getAge())
//                .chassisIdType(lldp.getChassisIdType())
//                .neSubType(lldp.getNeSubType())
//                .neType(lldp.getNeType())
//                .neighborId(lldp.getNeighborId())
//                .neName(lldp.getNeName())
//                .neVendor(lldp.getVendor())
//                .siteName(lldp.getSiteName())
//                .subnet(lldp.getSubnet())
//                .tpName(lldp.getTpName())
//                .tpType(lldp.getPortType())
//                .neighborPortDescription(lldp.getPortDescription())
//                .neighborPortIdType(lldp.getPortIdType())
//                .neighborPortId(lldp.getPortId())
//                .chassisId(lldp.getChassisId())
//                .systemDescription(lldp.getSystemDescription())
//                .systemName(lldp.getSystemName())
//                .capability(lldp.getCapability())
//                .neIp(lldp.getIp())
//                .age(lldp.getAge())
//                .lastUpdate(String.valueOf(lldp.getLastUpdate()))
//                .managementAddressType(lldp.getManagementAddressType())
//                .managementAddress(lldp.getManagementAddress())
//                .build();
//    }


    public static Page<CardInfo> convert2CardInfoPaged(PageResult<Card> pageResult) {
        Page<CardInfo> result = Page.<CardInfo>builder().pageNo(pageResult.getPageNum())
                .pageSize(pageResult.getPageSize()).totalPages(
                        pageResult.getPages()).total(pageResult.getTotal())
                .records(convert2CardInfos(pageResult.getList())).build();
        return result;
    }

    private static List<CardInfo> convert2CardInfos(List<Card> cards) {
        List<CardInfo> cardInfos = cards.stream().map(ConvertorUtils::convert2CardInfo)
                .collect(Collectors.toList());
        return cardInfos;
    }

    private static CardInfo convert2CardInfo(Card card) {
        return CardInfo.builder()
                .cardId(card.getCardId())
                .cardName(card.getCardName())
                .cardType(card.getCardType())
                .vendorName(card.getVendorName())
                .pn(card.getPN())
                .sn(card.getSN())
                .fwVersion(card.getFwVersion())
                .hwVersion(card.getHwVersion())
                .partName(card.getPartName())
                .swVersion(card.getSwVersion())
                .subnet(card.getSubnet())
                .neName(card.getNeName())
                .neIp(card.getNeIp())
                .neVendor(card.getNeVendor())
                .neSubType(card.getNeSubType())
                .siteName(card.getSiteName())
                .shelf(card.getShelf())
                .slot(card.getSlot())
                .asset_management_code(card.getAsset_management_code())
                .description(card.getDescription())
                .mfgDate(card.getMfgDate())
                .build();
    }

    public static Page<TransceiverInfo> convert2TransceiverInfoPaged(
            PageResult<Transceiver> transceiverPageResult) {
        Page<TransceiverInfo> transceiverInfoPage = Page.<TransceiverInfo>builder()
                .pageNo(transceiverPageResult.getPageNum())
                .pageSize(transceiverPageResult.getPageSize())
                .totalPages(transceiverPageResult.getPages())
                .total(transceiverPageResult.getTotal())
                .records(convert2TransceiverInfos(transceiverPageResult.getList()))
                .build();
        return transceiverInfoPage;
    }

    private static List<TransceiverInfo> convert2TransceiverInfos(List<Transceiver> transceivers) {
        List<TransceiverInfo> transceiverInfos = transceivers.stream()
                .map(ConvertorUtils::convert2TransceiverInfo)
                .collect(Collectors.toList());
        return transceiverInfos;
    }

    private static TransceiverInfo convert2TransceiverInfo(Transceiver transceiver) {
        if (transceiver == null) {
            return null;
        }

        TransceiverInfo transceiverInfo = new TransceiverInfo();
        BeanUtils.copyProperties(transceiver, transceiverInfo);
        return transceiverInfo;

    }


    public static CardCsv convertToCardCsv(Card card) {
        if (card == null) {
            return null;
        }
        return CardCsv.builder()
                .neId(card.getNeId())
                .neName(card.getNeName())
                .neVendorName(card.getNeVendor())
                .siteId(card.getSiteId())
                .siteName(card.getSiteName())
                .subnet(card.getSubnet())
                .neIp(card.getNeIp())
                .equipmentId(card.getCardId())
                .equipmentName(card.getCardName())
                .vendorName(card.getVendorName())
                .PN(card.getPN())
                .SN(card.getSN())
                .hwVersion(card.getHwVersion())
                .swVersion(card.getSwVersion())
                .fwVersion(card.getFwVersion())
                .mfgDate(card.getMfgDate())
                .assetManagementCode(card.getAsset_management_code())
                .neType(card.getNeType())
                .neSubType(card.getNeSubType())
                .equipmentType(card.getCardType())
                .description(card.getDescription())
                .build();
    }


    public static TransceiverCsv convertToTransceiverCsv(Transceiver transceiver) {
        if (transceiver == null) {
            return null;
        }
        return TransceiverCsv.builder()
                .neId(transceiver.getNeId())
                .neName(transceiver.getNeName())
                .neVendorName(transceiver.getNeVendor())
                .neType(transceiver.getNeType())
                .neSubType(transceiver.getNeSubType())
                .subnet(transceiver.getSubnet())
                .siteId(transceiver.getSiteId())
                .siteName(transceiver.getSiteName())
                .neIp(transceiver.getNeIp())
                .transceiverId(transceiver.getTransceiverId())
                .transceiverName(transceiver.getTransceiverName())
                .shelf(transceiver.getShelf())
                .slot(transceiver.getSlot())
                .serialNo(transceiver.getSerialNo())
                .vendorName(transceiver.getVendorName())
                .hwVersion(transceiver.getHwVersion())
                .swVersion(transceiver.getSwVersion())
                .fwVersion(transceiver.getFwVersion())
                .partNo(transceiver.getPartNo())
                .description(transceiver.getDescription())
                .connectorType(transceiver.getConnectorType())
                .formFactor(transceiver.getFormFactor())
                .ethernetPmd(transceiver.getEthernetPmd())
                .mfgDate(transceiver.getMfgDate())

                .minTemperature(transceiver.getMinTemperature())
                .maxTemperature(transceiver.getMaxTemperature())
                .minInputPower(transceiver.getMinInputPower())
                .maxInputPower(transceiver.getMaxInputPower())
                .minOutputPower(transceiver.getMinOutputPower())
                .maxOutputPower(transceiver.getMaxOutputPower())
                .minLaneInputPower(transceiver.getMinLaneInputPower())
                .maxLaneInputPower(transceiver.getMaxLaneInputPower())
                .minLaneOutputPower(transceiver.getMinLaneOutputPower())
                .maxLaneOutputPower(transceiver.getMaxLaneOutputPower())
                .minInputVoltage(transceiver.getMinInputVoltage())
                .maxInputVoltage(transceiver.getMaxInputVoltage())
                .minInputCurrent(transceiver.getMinInputCurrent())
                .maxInputCurrent(transceiver.getMaxInputCurrent())
                .allocatedPower(transceiver.getAllocatedPower())
                .minLaserBiasCurrent(transceiver.getMinLaserBiasCurrent())
                .maxLaserBiasCurrent(transceiver.getMaxLaserBiasCurrent())
                .minLaneLaserBiasCurrent(transceiver.getMinLaneLaserBiasCurrent())
                .maxLaneLaserBiasCurrent(transceiver.getMaxLaneLaserBiasCurrent())

                .build();
    }
}
