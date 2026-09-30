/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

@Builder
@Data
public class NodeConstructInfo {

    @NonNull
    private List<Equipments> nonEmptyEquipments;
    @NonNull
    private List<TerminationPoint> tpList;
    @NonNull
    private Set<Integer> lineCardUsedSlots;
    @NonNull
    private Map<String, List<Map<String, String>>> idleCardTps; //<cardType, List<portName,tpId>>
    @NonNull
    private Map<Integer, Equipments> availableEmptyEquipmentMap;
}
