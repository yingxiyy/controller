package net.flex.dci.otn.controller.discovery.common.impl;

import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OcmGroupMachine {
    private Node cfgNode;
    private Node opNode;

    public OcmGroupMachine(Node cfgNode, Node opNode) {
        this.cfgNode = cfgNode;
        this.opNode = opNode;
    }

    //replace real NE's OCM info
    public Node start() {
        log.trace("make OcmGroupMachine to impl {}", cfgNode.getNodeId().getValue());
        List<OCMGripGroups> cfgOcmGroupList = cfgNode.getAugmentation(Node1.class).getPhysical().getOCMGripGroups();
        List<OCMGripGroups> opOcmGroupList = opNode.getAugmentation(Node1.class).getPhysical().getOCMGripGroups();

        if (cfgOcmGroupList == null || opOcmGroupList == null) {
            //this is TPC ne
            return cfgNode;
        }
        List<OCMGripGroups> newOcmGroupList = new ArrayList<>();
        for (OCMGripGroups opGroup : opOcmGroupList) {
            int opIndex = opGroup.getIndex();
            if (opIndex == 1 || opIndex == 2) {
                //网元上固有的，无需替换处理
                //1:  grid=50, 96波固定频率
                //2:  grid=75，64波固定频率
                continue;
            }
            //3,4 flex 轮换使用， cfg树总是3， op树基于网元（3 or 4）
            boolean found = false;

            for (OCMGripGroups cfgGroup : cfgOcmGroupList) {
                int cfgIndex = cfgGroup.getIndex();
                if (cfgIndex == cfgIndex || cfgIndex == opIndex + 1) {
                    updateCfgGroup(newOcmGroupList, cfgGroup, opGroup);
                    found = true;
                }
            }
            if (!found) {
                //cfg树没有，直接用op树的
                updateCfgGroup(newOcmGroupList, null, opGroup);
            }
        }
        return newCfgNode(cfgNode, newOcmGroupList);
    }

    private void updateCfgGroup(List<OCMGripGroups> newOcmGroupList, OCMGripGroups cfgGroup, OCMGripGroups opGroup) {
        int index;
        String slot;
        if (cfgGroup != null) {
            index = cfgGroup.getIndex();
            slot = cfgGroup.getSlot();
        } else {
            index = opGroup.getIndex();
            slot = opGroup.getSlot();
        }
        OCMGripGroupsBuilder groupsBuilder = new OCMGripGroupsBuilder().setIndex(index).setSlot(slot)
                .setKey(new OCMGripGroupsKey(index, slot));
        if (cfgGroup == null) {
            groupsBuilder.setChannels(opGroup.getChannels());
        } else {
            //no mrege required. ocmGroup based on cfg tree.
//            Iterator<Channels> iter = cfgGroup.getChannels().iterator();
//            while (iter.hasNext()) {
//                Channels cfgChannel = iter.next();
//                boolean found = false;
//                for (Channels opChannel : opGroup.getChannels()) {
//                    if (cfgChannel.getLowerFrequency() == opChannel.getLowerFrequency()) {
//                    }
//                }
//            }
        }

    }

    private Node newCfgNode(Node cfgNode, List<OCMGripGroups> newOcmGroupList) {
        return new NodeBuilder(cfgNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(cfgNode.getAugmentation(Node1.class).getPhysical())
                                .setOCMGripGroups(newOcmGroupList)
                                .build())
                        .build())
                .build();
    }
}
