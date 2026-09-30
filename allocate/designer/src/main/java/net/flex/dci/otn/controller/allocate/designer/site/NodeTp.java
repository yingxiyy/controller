/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;


import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;

@Builder
@Data
class NodeTp {

    /*
        @NonNull
        private Node node;*/
    @NonNull
    private String nodeId;
    @NonNull
    private List<TerminationPoint> nodeTpList;
    @NonNull
    private List<Equipments> equipments;

    @NonNull
    private List<CardTps> cardTps;// must be in order
    private List<CardTps> slaveCardTps;// must be in order
    private List<CardTps> thirdCardTps;// must be in order
    @NonNull
    private SiteResourceLayout resourceLayout;
    private RoutingType protectionPeerRole;

    public CardTps getRightPeer() {
        CardTps result = getCardTps().get(getCardTps().size() - 1);
        if (result.getCard().getCardType().equals(NeInfo.PANEL_CARD_TYPE)) {
            return getCardTps().get(getCardTps().size() - 2);
        }
        return result;
    }

    public CardTps getSlaveRightPeer() {
        if (getSlaveCardTps().isEmpty()) {
            if (hasLayoutProtectionPeer(RoutingType.Slave)) {
                return getLayoutProtectionPeer(RoutingType.Slave);
            }
            return getCardTps().stream()
                    .filter(card -> card.slavePortNameTpMap != null
                            && !card.slavePortNameTpMap.isEmpty())
                    .findFirst().orElse(null);
        }
        return getSlaveCardTps().get(getSlaveCardTps().size() - 1);
    }
    public CardTps getThirdRightPeer() {
        if (getThirdCardTps().isEmpty()) {
            if (hasLayoutProtectionPeer(RoutingType.Third)) {
                return getLayoutProtectionPeer(RoutingType.Third);
            }
            return getCardTps().stream()
                    .filter(card -> card.thirdPortNameTpMap != null
                            && !card.thirdPortNameTpMap.isEmpty())
                    .findFirst().orElse(null);
        }
        return getThirdCardTps().get(getThirdCardTps().size() - 1);
    }

    public CardTps getLeftPeer() {
        CardTps result = getCardTps().get(0);
        if (result.getCard().getCardType().equals(NeInfo.PANEL_CARD_TYPE)) {
            return getCardTps().get(1);
        }
        return result;
    }

    public CardTps getSlaveLeftPeer() {
        if (getSlaveCardTps().isEmpty()) {
            if (hasLayoutProtectionPeer(RoutingType.Slave)) {
                return getLayoutProtectionPeer(RoutingType.Slave);
            }
            return getCardTps().stream()
                    .filter(card -> card.slavePortNameTpMap != null
                            && !card.slavePortNameTpMap.isEmpty())
                    .findFirst().orElse(null);
        }
        return getSlaveCardTps().get(0);
    }

    public CardTps getThirdLeftPeer() {
        if (getThirdCardTps().isEmpty()) {
            if (hasLayoutProtectionPeer(RoutingType.Third)) {
                return getLayoutProtectionPeer(RoutingType.Third);
            }
            return getCardTps().stream()
                    .filter(card -> card.thirdPortNameTpMap != null
                            && !card.thirdPortNameTpMap.isEmpty())
                    .findFirst().orElse(null);
        }
        return getThirdCardTps().get(0);
    }

    private boolean hasLayoutProtectionPeer(RoutingType role) {
        return resourceLayout.getOlpLinePort(role) != null;
    }

    private CardTps getLayoutProtectionPeer(RoutingType role) {
        String layoutPort = resourceLayout.getOlpLinePort(role);
        return getCardTps().stream()
                .filter(card -> {
                    if (role == RoutingType.Slave) {
                        return card.slavePortNameTpMap != null
                                && card.slavePortNameTpMap.containsKey(layoutPort);
                    }
                    return card.thirdPortNameTpMap != null
                            && card.thirdPortNameTpMap.containsKey(layoutPort);
                })
                .findFirst().orElse(null);
    }
}
