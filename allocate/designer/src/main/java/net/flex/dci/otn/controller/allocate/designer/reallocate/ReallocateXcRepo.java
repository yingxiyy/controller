package net.flex.dci.otn.controller.allocate.designer.reallocate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.Amplifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReallocateXcRepo implements ReallocateInterface<CrossConnections> {

    @Autowired
    private ReallocateTpRepo reallocateTpRepo;

    @Override
    public CrossConnections reallocate(CrossConnections xc, Set<String> oldEquipIds, Map<String, String> reallocateEquipMap) throws NeDesignerException {
        String newXcId = xc.getCrossConnectionId().getValue();

        if (oldEquipIds.size() == 1) { //对于allocate复用段时，所有的xc，都是板卡内部交叉，所有size只能为1

            String oldEquipId = oldEquipIds.iterator().next();
            String newEquipId = reallocateEquipMap.get(oldEquipId);
//            newXcId = newXcId.replace(oldEquipId, newEquipId);

            String newNodeId = PhysicalNodeIdNamingRule.getNodeId(newEquipId);

            //reallocate source
            List<SourceTp> sourceTps = xc.getSourceTp();
            List<SourceTp> newSourceTps = new ArrayList<>();
            for (SourceTp sourceTp : sourceTps) {
                String oldSrcTpId = sourceTp.getTpRef().getValue();
                String newSrcTpId = reallocateTpRepo.getNewTpId(oldSrcTpId, newEquipId);
                newSourceTps.add(new SourceTpBuilder().setTpRef(TpId.getDefaultInstance(newSrcTpId)).build());
                newXcId = newXcId.replace(oldSrcTpId, newSrcTpId);
            }

            //reallocate dest
            List<DestinationTp> destTps = xc.getDestinationTp();
            List<DestinationTp> newDestTps = new ArrayList<>();
            for (DestinationTp destTp : destTps) {
                String oldDestTpId = destTp.getTpRef().getValue();
                String newDestTpId = reallocateTpRepo.getNewTpId(oldDestTpId, newEquipId);
                newDestTps.add(new DestinationTpBuilder().setTpRef(TpId.getDefaultInstance(newDestTpId)).build());
                newXcId = newXcId.replace(oldDestTpId, newDestTpId);
            }

            String oldSlot = NEIdGenerator.getSlotFromTp(sourceTps.get(0).getTpRef());
            String newSlot = NEIdGenerator.getSlotFromTp(newSourceTps.get(0).getTpRef());
            Aps newAps = xc.getAps();
            if (newAps != null && !oldSlot.equals(newSlot)) {
                newAps = new ApsBuilder(newAps).setName(XCRepo.getApsName(newSlot)).build();
            }
            Amplifier amplifier = xc.getAmplifier();

            CrossConnections newXc = new CrossConnectionsBuilder(xc)
                    .setCrossConnectionId(new Uri(newXcId))
                    .setDescription(reallocateXCDescription(amplifier, newSlot, newAps, xc.getDescription()))
                    .setDestinationTp(newDestTps)
                    .setKey(new CrossConnectionsKey(new Uri(newXcId)))
                    .setNodeRef(new NodeId(newNodeId))
                    .setSourceTp(newSourceTps)
                    .setAps(newAps)
                    .build();

            return newXc;
        } else {
            //todo： 目前只有CMUX到OA的跨办卡交叉，这个交叉是创建业务时才会allocate，所有这里暂时不用考虑
            throw new NeDesignerException("Not supported for XCs in different cards.");

        }


    }

    @Override
    public String getId(CrossConnections item) {
        return item.getCrossConnectionId().getValue();
    }

    private String reallocateXCDescription(Amplifier amplifier, String newSlot, Aps newAps, String description) {
        if (newAps != null) {//e.g. "APS-1-3-1"
            return newAps.getName();
        }

        if (amplifier != null) { //e.g. "AMPLIFIER-1-2-BA"
            String[] split = description.split("-");
            try {
                return String.format("%s-%s-%s-%s", split[0], split[1], newSlot, split[3]);
            } catch (IndexOutOfBoundsException e) {
                return description; //有些amplifier不是null，但是是{}， 这种也是常规得description，比如： "XC-1644475356505-1644539423887"
            }
        }

        return description; //refer to xcRepo, 这种场景生成的description只和siteId有关，而siteId不变，所以description也保持。
    }
}
