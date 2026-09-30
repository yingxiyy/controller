package net.flex.dci.otn.controller.implement.common.ase.ne;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
public class ReadyResource {
    List<TerminationPoint> tpList;
    List<CrossConnections> xcList;
    List<Equipments> eqList;
    List<InternalLinks> ilList;

    public ReadyResource() {
        tpList = new ArrayList<>();
        xcList = new ArrayList<>();
        eqList = new ArrayList<>();
        ilList = new ArrayList<>();
    }
}

