/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CardClass;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.ExternalLink;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;
import net.flex.dci.otn.controller.allocate.ne.FixEquipModel;
import net.flex.dci.otn.controller.allocate.ne.Ne;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.CardType;

@Slf4j
@Getter
public class NeInfo {

    public static final String EMPTY_CARD_TYPE = "BLANK";
    public static final String PANEL_CARD_TYPE = "PANEL";
    public static final String OP_CARD_TYPE = "OP";
    public static final String OA_CARD_TYPE = "OA";
    public static final String OT_CARD_CLASS = "OT";
    public static final String MUX64_CARD_CLASS = "MUX64";
    public static final String MUX_CARD_CLASS = "MUX";
    private static final String MUX_C64_CARD_TYPE = "MUX_C64";
    private static final String MUX_32_CARD_TYPE = "MUX_32";
    public static final String CMUX_CARD_CLASS = "CMUX";
    public static final String MUXPANEL_CARD_CLASS = "MUXPANEL";
    public static final String RAMAN_CARD_CLASS = "RAMAN";
    public static final String RAMAN_CARD_TYPE = "RAMAN_CL";
    public static final String RAMAN_LINE_PORT = "LINE";
    public static final Set<String> OP_PORT_SIG = Stream.of("SIG", "APSC").collect(Collectors.toSet());
    public static final String CARD_TYPE_VENDOR_SEPERATOR = "-";

    private final Ne ne;
    private final String jsonFileName;
    private Set<String> supportedFrequency = new HashSet<>();

    //<CardType,<portName,port>>
    private Map<String, Map<String, Port>> portInfo = new HashMap<>();//assume the cardType is unique for all the cardClass
    //<fromCardType,<toCardType,<fromName, ExternalLinkTo>>>
    private Map<String, Map<String, Map<String, List<ExternalLinkTo>>>> externalLinkInfo = new HashMap<>();//只存固定的

    //<cardClass,card>, key is card class, expect MUX, for MUX, key is cardType, e.g. MUX48, MUX96
    private Map<String, List<Card>> cardInfo = new HashMap<>();

    // <cardVendor,card>
    private Map<String, Card> cardVendorCardMap = new HashMap<>();
    // <cardType,card>
    private Map<String, List<Card>> cardTypeCardMap = new HashMap<>();

    //<cardVendor,cardClass>
    private Map<String, String> cardVendorTypeClassMap = new HashMap<>();
    private Set<String> tunnelXcPortNames = new HashSet<>();//The portName which need to create XC during tunnel creation.


    public NeInfo(Ne ne, String jsonFileName) throws NeDesignerException {
        this.ne = ne;
        this.jsonFileName = jsonFileName;
        init();
    }

    /**
     * @param centralFrequency e.g. "196050000,191300000,50000"
     * @param portName e.g. m1d1,ad135
     * @return e.g. /frequency=196025000,19675000
     */
    public static String getSlotByCentralFrequency(String centralFrequency, String portName, int index)
            throws NeDesignerException {

        try {
            String[] centralFrequencyConfig = centralFrequency.split(",");
            long start = Long.parseLong(centralFrequencyConfig[0]);
            long end = Long.parseLong(centralFrequencyConfig[1]);
            long step = Long.parseLong(centralFrequencyConfig[2]);
            long portCentralFrequency = start - step * index;
            long lowerFrequency = portCentralFrequency - step / 2;
            long upperFrequency = portCentralFrequency + step / 2;
            if (lowerFrequency < end - step) {
                throw new NeDesignerException(String.format(
                        "Failed to get frequency by portName: %s, centralFrequency: %s. Because lowerFrequency: %s crossed the minimum value.",
                        portName, centralFrequency, lowerFrequency));
            }
            if (upperFrequency > start + step) {
                throw new NeDesignerException(String.format(
                        "Failed to get frequency by portName: %s, centralFrequency: %s. Because upperFrequency: %s crossed the maximum value.",
                        portName, centralFrequency, upperFrequency));
            }

            return String.format("/frequency=%s,%s", lowerFrequency, upperFrequency);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            throw new NeDesignerException(String.format(
                    "Failed to get frequency by portName: %s, centralFrequency: %s.",
                    portName, centralFrequency), e);
        }
    }


    private void init() throws NeDesignerException {
        for (CardClass cardClass : ne.getCardClass()) {
            String cardClassName = cardClass.getName();
//            if (cardClassName.equals("CMUX")) {
//                supportedFrequency.add(NeInfoUtil.getSupportedFrequency(cardClass.getName()));
//            }
            initCard(cardClassName, cardClass.getCard());
        }
    }

    private void initCard(String cardClassName, List<Card> cards)
            throws NeDesignerException {
        Boolean isFrequencyRelated = cardClassName.equals(MUX64_CARD_CLASS) || cardClassName.equals(MUX_CARD_CLASS) || cardClassName.equals(CMUX_CARD_CLASS) || cardClassName.equals(MUXPANEL_CARD_CLASS);
//        if (!isFrequencyRelated || cardClassName.equals(MUXPANEL_CARD_CLASS)) {
        this.cardInfo.put(cardClassName, cards);
//        }
        if (isFrequencyRelated) {
            if (cardClassName.equals(MUX_CARD_CLASS)) {
                // ByteDance2.0 groups MUX64/MUX32 variants under card class MUX, so expose
                // capability per concrete card type instead of assigning one grid to the class.
                for (Card card : cards) {
                    supportedFrequency.add(NeInfoUtil.getSupportedFrequency(card.getCardType()));
                }
            } else {
                supportedFrequency.add(NeInfoUtil.getSupportedFrequency(cardClassName));
            }

        }
        for (Card card : cards) {
            String cardType = card.getCardType();
//            if (isFrequencyRelated) {
//                //FOR MUX card, use cardType as the key in cardInfo, instead cardClass
//                List<Card> existedMuxCards = this.cardInfo.get(cardType);
//                if (existedMuxCards == null) {
//                    existedMuxCards = new ArrayList<>();
//                }
//                existedMuxCards.add(card);
//                this.cardInfo.put(cardType, existedMuxCards);
//            }
            this.cardVendorTypeClassMap.put(card.getVendorType(), cardClassName);
            this.cardVendorCardMap.put(card.getVendorType(), card);
            List<Card> existsCardsByCardType = this.cardTypeCardMap.get(cardType);
            if (existsCardsByCardType == null) {
                existsCardsByCardType = new ArrayList<>();
            }
            existsCardsByCardType.add(card);
            this.cardTypeCardMap.put(cardType, existsCardsByCardType);

            //port
            Map<String, Port> portMap = new HashMap<>();
            for (Port port : card.getPorts()) {
                portMap.put(port.getName(), port);
            }
            portInfo.put(cardType, portMap);

            //externalLink: <toCardType,<fromName, ExternalLinkTo>>
            Map<String, Map<String, List<ExternalLinkTo>>> externalToMap = new HashMap<>();

            for (ExternalLink externalLink : card.getExternalLinks()) {
                if (!externalLink.getInitiated()) {//只存固定的
                    continue;
                }

                for (ExternalLinkTo externalLinkTo : externalLink.getTo()) {
                    Map<String, List<ExternalLinkTo>> fromToMap = externalToMap
                            .get(externalLinkTo.getCardType());
                    if (fromToMap == null) {
                        fromToMap = new LinkedHashMap<>();
                    }

                    List<ExternalLinkTo> externalLinkToList = fromToMap.get(externalLink.getFrom());
                    if (externalLinkToList == null) {
                        externalLinkToList = new ArrayList<>();
                    }
                    externalLinkToList.add(externalLinkTo);
                    fromToMap.put(externalLink.getFrom(), externalLinkToList);
                    externalToMap.put(externalLinkTo.getCardType(), fromToMap);

                  /*  if (externalLink.getBiDirection()) {
                        EnumMap<CardType, Map<String, ExternalLinkTo>> reverseMap = this.externalLinkInfo
                                .get(externalLinkTo.getCardType());
                        if (reverseMap == null) {
                            reverseMap = new EnumMap<>(CardType.class);
                        }
                        Map<String, ExternalLinkTo> reverseFromToMap = reverseMap
                                .get(card.getCardType());
                        if (reverseFromToMap == null) {
                            reverseFromToMap = new LinkedHashMap<>();
                        }

                        ExternalLinkTo reverseExternalLinkTo = new ExternalLinkTo();
                        reverseExternalLinkTo.setCardType(card.getCardType());
                        reverseExternalLinkTo.setPort(externalLink.getFrom());
                        reverseExternalLinkTo.setNeType(NeInfoUtil.getNeType(equipmentType));
                        reverseFromToMap.put(externalLinkTo.getPort(), reverseExternalLinkTo);

                        reverseMap.put(card.getCardType(), reverseFromToMap);
                        this.externalLinkInfo.put(externalLinkTo.getCardType(), reverseMap);
                    }*/
                }
            }
            this.externalLinkInfo.put(cardType, externalToMap);

            for (CrossConnection crossConnection : card.getCrossConnections()) {
                if (!crossConnection.getInitiated()) {
                    this.tunnelXcPortNames.addAll(NeInfoUtil.getNameList(crossConnection.getFrom().getPort()));
                    this.tunnelXcPortNames.addAll(NeInfoUtil.getNameList(crossConnection.getTo().getPort()));
                }
            }

            for (CrossConnection crossConnection : card.getExternalCrossConnections()) {
                if (!crossConnection.getInitiated()) {
                    this.tunnelXcPortNames.addAll(NeInfoUtil.getNameList(crossConnection.getFrom().getPort()));
//                    this.tunnelXcPortNames.addAll(NeInfoUtil.getNameList(crossConnection.getTo().getPort()));
                }
            }
        }

    }

    public Map<String, Port> getPort(String cardType)
            throws NeDesignerException {
        Map<String, Port> portInfoMap = this.portInfo.get(cardType);

        if (portInfoMap != null) {
            return portInfoMap;
        }
        throw new NeDesignerException(String.format(
                "Failed to get port info for %s, from the json file %s",
                cardType, jsonFileName));
    }


/*    public CrossConnection getMuxCardXC(Grid grid) throws NeDesignerException {
        Card muxCard = this.getMuxCard(grid);
        try {
            return muxCard.getCrossConnections()
                    .get(0);//for mux card, only have one kind xc, then get the first one.
        } catch (IndexOutOfBoundsException e) {
            throw new NeDesignerException(
                    "Failed to get the crossConnection information from  json file: %s"
                            + this.jsonFileName);
        }

    }*/


    /**
     * 当UI没有指定cardVendor时，默认用当前cardClass的第一个
     *
     * @param
     * @param wdmBand
     * @return
     * @throws NeDesignerException
     */
    public Card getDefaultCardByCardClass(String cardClass, WDM_Band wdmBand) throws NeDesignerException {
        List<Card> cards = this.cardInfo.get(cardClass);
        if (wdmBand != null) {

            cards = cards.stream().filter(card -> isSameWdm(card.getWdmBand(), wdmBand)).collect(Collectors.toList());
        }

        if (cards == null || cards.isEmpty()) {
            throw new NeDesignerException(String.format(
                    "Failed to get the card definition by card class : %s, please check the json: %s", cardClass, jsonFileName));
        }
        return cards.get(0);
    }

    public Card getDefaultCardByCardClass(String cardClass, WDM_Band wdmBand, Integer grid)
            throws NeDesignerException {
        if (!MUX_CARD_CLASS.equals(cardClass) || grid == null) {
            return getDefaultCardByCardClass(cardClass, wdmBand);
        }

        // Bone2.0 stores both fixed-grid MUX variants under one card class.
        String expectedCardType = grid == 75 ? MUX_C64_CARD_TYPE
                : grid == 150 ? MUX_32_CARD_TYPE : null;
        if (expectedCardType == null) {
            return getDefaultCardByCardClass(cardClass, wdmBand);
        }
        return this.cardInfo.get(cardClass).stream()
                .filter(card -> isSameWdm(card.getWdmBand(), wdmBand))
                .filter(card -> expectedCardType.equals(card.getCardType()))
                .findFirst()
                .orElseThrow(() -> new NeDesignerException(String.format(
                        "Failed to get MUX card for grid %s from json: %s", grid, jsonFileName)));
    }

    private Boolean isSameWdm(String wdmCard, WDM_Band wdmBand) {
        if (wdmCard == null || wdmCard.isEmpty()) {
            return true;
        }
        return WDM_Band.fromString(wdmCard).equals(wdmBand);
    }

    public List<Integer> geEmptyEquipPossibleSlots() throws NeDesignerException {
        return getEmptyCard().getPossibleSlot();
    }

    public Card getEmptyCard() throws NeDesignerException {
        try {
            return getDefaultCardByCardClass(EMPTY_CARD_TYPE, null);
        } catch (IndexOutOfBoundsException e) {
            String msg = String.format("Failed to get BLANK card, because of invalid NE json file: %s", jsonFileName);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
    }


    public boolean isReplacedAsEmpty(String cardVendorType) throws NeDesignerException {
        Card card = getCardVendorCardMap().get(cardVendorType);
        if (card == null) {
            String errorMsg = String.format("Failed to get card by card vendor type:%s in json:%", cardVendorType, jsonFileName);
            throw new NeDesignerException(errorMsg);
        }
        return getEmptyCard().getPossibleSlot().containsAll(card.getPossibleSlot());
    }

    public Map<String, List<ExternalLinkTo>> getExternalLinkInfoFromTo(String fromCardType, String toCardType) throws NeDesignerException {

        Map<String, Map<String, List<ExternalLinkTo>>> toCardMap = this.externalLinkInfo.get(fromCardType);
        if (toCardMap == null) {
            String errorMessage = String.format("Failed to get the ExternalLink from cardType: %s, please check the json: %s", fromCardType, jsonFileName);
            log.error(errorMessage);
            throw new NeDesignerException(errorMessage);
        }

        Map<String, List<ExternalLinkTo>> result = toCardMap.get(toCardType);
        if (result == null) {
            String errorMessage = String.format("Failed to get the ExternalLink from cardType: %s to cardType: %s , please check the json: %s", fromCardType, toCardType, jsonFileName);
            log.error(errorMessage);
            throw new NeDesignerException(errorMessage);
        }

        return result;
    }

    public String getCardClassByCardVendor(String cardVendor) throws NeDesignerException {
        String cardClass = this.cardVendorTypeClassMap.get(cardVendor);

        if (cardClass == null) {
            throw new NeDesignerException(String.format("Failed to get the cardClass by cardVendor: %s, please check the json: %s", cardVendor, jsonFileName));
        }
        return cardClass;
    }

    public Card getPanel() throws NeDesignerException {
        try {
            return getDefaultCardByCardClass(PANEL_CARD_TYPE, null);
        } catch (IndexOutOfBoundsException | NeDesignerException e) {
            String msg = String.format("Failed to get PANEL card, because of invalid NE json file: %s", jsonFileName);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
    }

    public String getProductType() {
        return this.ne.getProductType();
    }

    public String getVendor() {
        return this.ne.getVendorName();
    }

    public String getNodeType() {
        return this.ne.getNodeType();
    }

    public boolean hasCardByCardType(String cardType) {
        return this.cardTypeCardMap.containsKey(cardType);
    }

    public Set<CardType> getCardCapability() throws NeDesignerException {
        Set<CardType> result = new HashSet<>();
        List<Card> otCards = this.cardInfo.get(OT_CARD_CLASS);
        if (otCards == null) {
            log.warn("Got no OT card for getCardCapability in :{}", this.jsonFileName);
            return result;
        }

        for (Card card : otCards) {
            result.add(NeInfoUtil.getOtCardCapability(card, this.getVendor(), this.getProductType()));
        }
        return result;
    }

    public List<FixEquipModel> getFixEquipModel() {
        return this.ne.getFixEquipModel();
    }

    /**
     * The cardTypeVendors example is: "OA25-1-1,TWS05OA25"
     *
     * @param cardTypeVendors
     * @return
     * @throws NeDesignerException
     */
    public Map<String, Card> getCardsByCardVendors(Set<String> cardTypeVendors) throws NeDesignerException {
        Map<String, Card> result = new HashMap<>();
        for (String cardTypeVendor : cardTypeVendors) {
            String pureCardTypeVendor = cardTypeVendor.contains(CARD_TYPE_VENDOR_SEPERATOR) ? cardTypeVendor.substring(0, cardTypeVendor.indexOf(CARD_TYPE_VENDOR_SEPERATOR)) : cardTypeVendor;
            if (!this.cardVendorCardMap.containsKey(pureCardTypeVendor)) {
                String errorMessage = String.format("Failed to get card by pureCardTypeVendor:%s,card vendor:%s, in :%s", pureCardTypeVendor, cardTypeVendor, this.jsonFileName);
                log.error(errorMessage);
                throw new NeDesignerException(errorMessage);
            }
            Card card = this.cardVendorCardMap.get(pureCardTypeVendor);
            String cardClass = getCardClassByCardVendor(pureCardTypeVendor);
            if (result.containsKey(cardClass)) {
                String errorMessage = String.format("Only support one card vendor type for each card class, but have both %s and % for card class %s, in NE:%s", cardTypeVendor,
                        result.get(cardClass).getVendorType(), cardClass, this.jsonFileName);
                log.error(errorMessage);
                throw new NeDesignerException(errorMessage);
            }
            result.put(cardClass, card);
        }
        return result;
    }

    public Card getCardByCardVendor(String cardVendorType) throws NeDesignerException {
        Card card = this.cardVendorCardMap.get(cardVendorType);
        if (card == null) {
            String errorMessage = String.format("Failed to get card by card vendor:%s in :%s", cardVendorType, this.jsonFileName);
            log.error(errorMessage);
            throw new NeDesignerException(errorMessage);
        }
        return card;
    }

    public Card getCardByCardType(String cardType) throws NeDesignerException {
        List<Card> cardList = this.cardTypeCardMap.get(cardType);
        if (cardList == null) {
            String errorMessage = String.format("Failed to get cardList by cardList type:%s in :%s", cardType, this.jsonFileName);
            log.error(errorMessage);
            throw new NeDesignerException(errorMessage);
        }
        if (cardList.size() != 1) {
            String errorMessage = String.format("Failed to get cardList by cardList type:%s in :%s, because this cardType requires only one cardList, but found:%d", cardType, this.jsonFileName,
                    cardList.size());
            log.error(errorMessage);
            throw new NeDesignerException(errorMessage);
        }
        return cardList.get(0);
    }
}
