package net.flex.dci.otn.controller.implement.common.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroupsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.Channels;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.ChannelsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.grip.group.ChannelsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Getter
public class OcmDataBuilder {

    private Map<String, List<OCMGripGroups>> updatedOcmGroupMap;  //string is nodeId

    public OcmDataBuilder() {
        updatedOcmGroupMap = new HashMap<>();
    }

    public void buildOcmGroup(List<Available> bandScopes, List<CrossConnectionAttributes> amplifierXcList, List<Link> workingOchLinkList) {
        log.info("start build ocm group based on working och list size: {}", workingOchLinkList.size());
        List<Channels> newChannels_C = new ArrayList<>();
        List<Channels> newChannels_L = new ArrayList<>();

        fetchOcmChannels(bandScopes, workingOchLinkList, newChannels_C, newChannels_L);

        Map<String, Set<String>> nodeIdOcmIdMap = new HashMap<>();
        amplifierXcList.forEach(xc-> {
            String ocmId = CrossConnectionSlotNamingRule.getOcmNameOverAmplifierXc(xc);
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue());

            if (!nodeIdOcmIdMap.containsKey(nodeId)) {
                nodeIdOcmIdMap.put(nodeId, new HashSet<>());
            }
            nodeIdOcmIdMap.get(nodeId).add(ocmId);
        });
        //一个板卡只有一个ocmId, 复用段的只可能每个网元提取出一块光放板卡

        nodeIdOcmIdMap.keySet().forEach(nodeId-> {
            List<OCMGripGroups> ocmGripGroupsList = null;
            if (updatedOcmGroupMap.containsKey(nodeId))
                ocmGripGroupsList = updatedOcmGroupMap.get(nodeId);
            else {
                ocmGripGroupsList = new ArrayList<>();
            }

            //同一个网元可能有多个OA板卡，每个板卡都有自己的ocmId
            for (String ocmId: nodeIdOcmIdMap.get(nodeId)) {
                OCMGripGroups ocmGroupC = null;
                OCMGripGroups ocmGroupL = null;
                //this is C 波段 related
                if (!newChannels_C.isEmpty()) {
                    OCMGripGroupsKey ocmGroupKey = new OCMGripGroupsKey(3, ocmId); //3 代表 C 波段
                    ocmGroupC = new OCMGripGroupsBuilder().setKey(ocmGroupKey)
                            .setIndex(ocmGroupKey.getIndex())
                            .setSlot(ocmGroupKey.getSlot())
                            .setChannels(newChannels_C)
                            .build();
                }

                if (!newChannels_L.isEmpty()) {
                    OCMGripGroupsKey ocmGroupKey = new OCMGripGroupsKey(5, ocmId); //5 代表 L 波段
                    ocmGroupL = new OCMGripGroupsBuilder().setKey(ocmGroupKey)
                            .setIndex(ocmGroupKey.getIndex())
                            .setSlot(ocmGroupKey.getSlot())
                            .setChannels(newChannels_L)
                            .build();
                }

                if (ocmGroupC != null) {
                    ocmGripGroupsList.add(ocmGroupC);
                }
                if (ocmGroupL != null) {
                    ocmGripGroupsList.add(ocmGroupL);
                }

                updatedOcmGroupMap.put(nodeId, ocmGripGroupsList);
            }
        });
    }

    private void fetchOcmChannels(List<Available> bandScopes, List<Link> workingOchLinkList, List<Channels> newChannels_C, List<Channels> newChannels_L) {
        //bandScopes排序都是从大到小的 降序， L波段频率小， 在后面
        bandScopes.sort(Comparator.comparingLong((Available scope) -> {
            return scope.getLowerFrequency().getValue().longValue();
        }).reversed());

        //och频谱排序都是从小++到大的 升序
        workingOchLinkList.sort(Comparator.comparingLong(link -> {
            Och och = link.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
            FrequencyType freq = och.getLowerFrequency();
            return freq != null ? freq.getValue().longValue() : Long.MAX_VALUE;
        }));


        AtomicInteger indexC = new AtomicInteger(1);
        AtomicInteger indexL = new AtomicInteger(1);
        workingOchLinkList.forEach(ochLink-> {
            Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();

            if (inScope(ochLinkAttr.getLowerFrequency(), bandScopes.get(0))) {  // 0-->C 波段
                Channels channel = new ChannelsBuilder()
                    .setIndex(indexC.get())
                    .setKey(new ChannelsKey(indexC.get()))
                    .setLowerFrequency(ochLinkAttr.getLowerFrequency())
                    .setUpperFrequency(ochLinkAttr.getUpperFrequency())
                    .build();

                if (!isOverlappingWithExisting(newChannels_C, channel)) {
                    newChannels_C.add(channel);
                    indexC.getAndIncrement();
                } else {
                    log.warn("Channel overlap detected in C band: {}", channel);
                }
            } else {
                Channels channel = new ChannelsBuilder()
                    .setIndex(indexL.get())
                    .setKey(new ChannelsKey(indexL.get()))
                    .setLowerFrequency(ochLinkAttr.getLowerFrequency())
                    .setUpperFrequency(ochLinkAttr.getUpperFrequency())
                    .build();

                if (!isOverlappingWithExisting(newChannels_L, channel)) {
                    newChannels_L.add(channel);
                    indexL.getAndIncrement();
                } else {
                    log.warn("Channel overlap detected in L band: {}", channel);
                }
            }
        });
    }

    private boolean isOverlappingWithExisting(List<Channels> channels, Channels newChannel) {
        return channels.stream().anyMatch(existing -> isOverlapping(existing, newChannel));
    }

    private boolean isOverlapping(Channels ch1, Channels ch2) {
        long ch1Low = ch1.getLowerFrequency().getValue().longValue();
        long ch1High = ch1.getUpperFrequency().getValue().longValue();
        long ch2Low = ch2.getLowerFrequency().getValue().longValue();
        long ch2High = ch2.getUpperFrequency().getValue().longValue();

        // 注意这里使用严格的小于和大于判断，没有等号
        return ch1Low < ch2High && ch2Low < ch1High;
    }

    private boolean inScope(FrequencyType checking, Available scope) {
        long min = scope.getLowerFrequency().getValue().longValue();
        long max = scope.getUpperFrequency().getValue().longValue();
        long checkingValue = checking.getValue().longValue();

        return checkingValue >= min && checkingValue <= max;
    }
}
