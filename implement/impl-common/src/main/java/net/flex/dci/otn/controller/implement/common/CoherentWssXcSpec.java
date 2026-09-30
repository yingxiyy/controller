package net.flex.dci.otn.controller.implement.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.Channels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.ChannelsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Coherent WSS XC Spec Implementation
 * 正常计算都是100/150GHz 的间隔， 设备特殊需要，下发WSS/OCM 的时候左右偏移6250
 */
@Slf4j
@Deprecated
public class CoherentWssXcSpec {
    private List<Node> write2NeList;
    private ChangedObject changedObject;
    
    public CoherentWssXcSpec(List<Node> write2NeList, ChangedObject changedObject) {
        this.changedObject = changedObject;
        this.write2NeList = write2NeList;
    }

    public void process() {
        List<Node> updateList = write2NeList.stream().map(node -> {
            Node updated = processWssXc(node);
            updated = processOcm(updated);
            return updated;
        }).collect(Collectors.toList());
        
        write2NeList.clear();
        write2NeList.addAll(updateList);

        write2NeList.stream().forEach(x -> {
            Node node = changedObject.getChangedPhyNode(x.getNodeId().getValue());
            Node updated = processWssXc(node);
            updated = processOcm(updated);
            changedObject.addChangedPhyNode(updated);
        });
    }

    private Node processWssXc(Node node) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream().map(x -> {
            if (x.getWssChannel() != null) {
                // process WSS channel adjustment
                WssChannel wss = x.getWssChannel();

                return new CrossConnectionsBuilder(x)
                        .setWssChannel(new WssChannelBuilder()
                                .setLowerFrequency(newFrequency(wss.getLowerFrequency(), 6250))
                                .setUpperFrequency(newFrequency(wss.getUpperFrequency(), -6250))
                                .build())
                        .build();
            }
            return x;
        }).collect(Collectors.toList());

        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodeAttr)
                                        .setCrossConnections(newXcList)
                                        .build())
                                .build())
                .build();
    }

    private FrequencyType newFrequency(FrequencyType freq, long offset) {
        return new FrequencyType(BigInteger.valueOf(freq.getValue().longValue() + offset));
    }

    private Node processOcm(Node node) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<OCMGripGroups> newOcmGroups = nodeAttr.getOCMGripGroups().stream().map(group -> {
            List<Channels> newChannels = group.getChannels().stream().map(ch -> {
                return new ChannelsBuilder(ch)
                        .setLowerFrequency(newFrequency(ch.getLowerFrequency(), 6250))
                        .setUpperFrequency(newFrequency(ch.getUpperFrequency(), -6250))
                        .build();
            }).collect(Collectors.toList());

            return new OCMGripGroupsBuilder(group)
                    .setChannels(newChannels)
                    .build();
        }).collect(Collectors.toList());

        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setOCMGripGroups(newOcmGroups)
                                .build())
                        .build())
                .build();
    }
}
