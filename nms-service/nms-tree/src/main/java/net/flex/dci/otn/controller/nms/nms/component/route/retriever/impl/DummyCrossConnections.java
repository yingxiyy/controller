package net.flex.dci.otn.controller.nms.nms.component.route.retriever.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class DummyCrossConnections {
    private static PhyNodeDao phyNodeDao;

    public List<CrossConnections> create(List<ExplicitRouteObjects> eroList, List<CrossConnections> realXcList) {
        AtomicInteger sequence = new AtomicInteger(100);

        List<CrossConnections> dummyXCList = new ArrayList<>();
        eroList.forEach(ero->{
            List<PathRouteObject> proList = ero.getPathRouteObject();

            Tp preTp = null;
            for (PathRouteObject pro : proList) {
                if (pro.getResourceType().getImplementedInterface().getName().equals(Tp.class.getName())) {
                    Tp curTp = (Tp) pro.getResourceType();
                    if (preTp != null) {
                        String curTpString = curTp.getTpHop().getTpRef().getValue();
                        String preTpString = preTp.getTpHop().getTpRef().getValue();

                        String curEqString = PhysicalTpIdNamingRule.getEquipId(curTpString);
                        String preEqString = PhysicalTpIdNamingRule.getEquipId(preTpString);
                        if (curEqString.equals(preEqString)) {
                            if (realXcList.stream().noneMatch(xc -> xc.getCrossConnectionId().getValue().contains(curTpString)
                                    && xc.getCrossConnectionId().getValue().contains(preTpString))) {
                                //现有真实交叉里面没有任何包含 当前TP， 前一个TP，且两个TP都在一个eq 的。 这种情况需要生成dummy XC
                                String nodeId = PhysicalTpIdNamingRule.getNodeId(curTpString);
                                Node node = phyNodeDao.getConfigPhyNodeById(nodeId);

                                List<Equipments> eqList = node.getAugmentation(Node1.class).getPhysical().getEquipments();
                                Optional<Equipments> eqOp = eqList.stream().filter(eq -> eq.getEquipmentId().equals(curEqString)).findAny();
                                if (eqOp.isPresent()) {
                                    if (eqOp.get().getEquipType().equals(EquipType.ILA) ||
                                            eqOp.get().getEquipType().equals(EquipType.DGE) ||
                                            eqOp.get().getEquipType().equals(EquipType.OA)) {
                                        //光放板卡准备两个单向交叉
                                        String xcId = PhysicalXcIdNamingRule.generateXcId(preTpString, curTpString) + "_Dummy";
                                        dummyXCList.add(new CrossConnectionsBuilder()
                                                .setCrossConnectionId(new Uri(xcId))
                                                .setSequence((long) sequence.getAndIncrement())
                                                .setDirection(LinkDirection.Unidirection)
                                                .build());

                                        xcId = PhysicalXcIdNamingRule.generateXcId(curTpString, preTpString) + "_Dummy";
                                        dummyXCList.add(new CrossConnectionsBuilder()
                                                .setCrossConnectionId(new Uri(xcId))
                                                .setSequence((long) sequence.getAndIncrement())
                                                .setDirection(LinkDirection.Unidirection)
                                                .build());
                                    } else {
                                        // 其他准备一个双向交叉
                                        String xcId = PhysicalXcIdNamingRule.generateXcId(preTpString, curTpString) + "_Dummy";
                                        dummyXCList.add(new CrossConnectionsBuilder()
                                                .setCrossConnectionId(new Uri(xcId))
                                                .setSequence((long) sequence.getAndIncrement())
                                                .setDirection(LinkDirection.Bidirection)
                                                .build());
                                    }
                                } else {
                                    log.error("impossible the EQ must existed in node {}", curEqString);
                                }
                            }
                        }
                    }

                    preTp = curTp;
                }
            }

        });
        return dummyXCList;
    }
}
