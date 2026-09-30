package net.flex.dci.otn.controller.allocate.node.phy;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class PhyNodeMerge {
  Node cfgNode;
  Node opNode;

  public PhyNodeMerge(Node cfgNode, Node opNode) {
    this.cfgNode = cfgNode;
    this.opNode= opNode;
  }

  /**
   * cfgNode after createTunnel, createSiteLink maybe import new eq/transceiver, these should merge into opNode
   * 添加新板卡、port,transceiver, xc, internalLink,
   * 现有板卡上修改usedInLink属性
   */
  @Deprecated
  public Node add() {
    addEq();
    addTp();
    addXc();
    addInternalLink();

    return opNode;
  }

  private void addInternalLink() {
    List<InternalLinks> cfgList = cfgNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
    List<InternalLinks> opList = opNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
    List<InternalLinks> newList = new ArrayList<>();

    for(InternalLinks cfg : cfgList) {
      boolean found = false;
      for (InternalLinks op : opList) {
        if (cfg.getLinkRef().equals(op.getLinkRef())) {
          found = true;
          break;
        }
      }
      if (found) {
        //数据已OP为准
        continue;
      } else {
        newList.add(cfg);
      }
    }
    opList.addAll(newList);

    opNode = new NodeBuilder(opNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(opList)
                .build())
            .build())
        .build();
  }

  private void addXc() {
    List<CrossConnections> cfgList = cfgNode.getAugmentation(Node1.class).getPhysical().getCrossConnections();
    List<CrossConnections> opList = opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections();
    List<CrossConnections> newList = new ArrayList<>();

    for(CrossConnections cfg : cfgList) {
      boolean found = false;
      for (CrossConnections op : opList) {
        if (cfg.getCrossConnectionId().getValue().equals(op.getCrossConnectionId().getValue())) {
          found = true;
          break;
        }
      }
      if (found) {
        //数据已OP为准
        continue;
      } else {
        newList.add(cfg);
      }
    }
    opList.addAll(newList);

    opNode = new NodeBuilder(opNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                .setCrossConnections(opList)
                .build())
            .build())
        .build();
  }

  private void addTp() {
    List<TerminationPoint> cfgList = cfgNode.getTerminationPoint();
    List<TerminationPoint> opList = opNode.getTerminationPoint();
    List<TerminationPoint> newList = new ArrayList<>();

    for (TerminationPoint cfg : cfgList) {
      boolean found = false;
      for (TerminationPoint op : opList) {
        if (cfg.getTpId().getValue().equals(op.getTpId().getValue())) {
          found = true;
          break;
        }
      }
      if (found) {
        //数据已OP为准
        continue;
      } else {
        newList.add(cfg);
      }
    }
    opNode.getTerminationPoint().addAll(newList);
  }

  private void addEq() {
    List<Equipments> cfgEqList = cfgNode.getAugmentation(Node1.class).getPhysical().getEquipments();
    Iterator<Equipments> opIter = opNode.getAugmentation(Node1.class).getPhysical().getEquipments().iterator();
    List<Equipments> newEqList = new ArrayList<>();

    AlignmentStatusType nodeAlignment = opNode.getAugmentation(Node1.class).getPhysical().getAlignmentStatus();

    for (Equipments cfgEq : cfgEqList) {
      boolean found = false;
      Equipments opEq = null;
      while (opIter.hasNext()) {
        opEq = opIter.next();
        if (cfgEq.getEquipmentId().equals(opEq.getEquipmentId())) {
          found = true;
          break;
        }
      }

      Equipments newEq;
      if (found) {
        opIter.remove();

        if (cfgEq.getEquipType().equals(opEq.getEquipType())) {
          //两边一样，就保持OP上生的
          EquipmentsBuilder newEqB = new EquipmentsBuilder(opEq)
              .setAlignmentStatus(AlignmentStatusType.Aligned);

          if (cfgEq.getEquipmentId().contains("LINECARD") || cfgEq.getEquipmentId().contains("MUX")) {
            newEq = newEqB.setUsedInLink(true).build();
          } else {
            //辅助板卡
            newEq = newEqB.build();
          }
        } else {
          if (opEq.getEquipType().equals(EquipType.EMPTY)) {
            //op树上是空，但是conf树上有，copy conf树上的内容到op
            newEq = new EquipmentsBuilder(cfgEq)
                .setAlignmentStatus(AlignmentStatusType.CardLack)
                .setUsedInLink(true)
                .build();
          } else {
            //op树上也有值但是上面的板卡信息和conf树的不一样，修改op树上的属性
            newEq = new EquipmentsBuilder(opEq)
                .setAlignmentStatus(AlignmentStatusType.CardMissMatch)
                .setUsedInLink(true)
                .build();
          }
        }
      } else {
        newEq = new EquipmentsBuilder(cfgEq)
            .setAlignmentStatus(cfgEq.getEquipmentId().contains("TRANSCEIVER") ? AlignmentStatusType.ModularLack : AlignmentStatusType.CardLack)
            .setUsedInLink(true)
            .build();
      }

      newEqList.add(newEq);
      if (nodeAlignment.getIntValue() < newEq.getAlignmentStatus().getIntValue()) {
        nodeAlignment = newEq.getAlignmentStatus();
      }
    }

    opNode = new NodeBuilder(opNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                .setAlignmentStatus(nodeAlignment)
                .setEquipments(newEqList)
                .build())
            .build())
        .build();
  }

  private List<TerminationPoint> getPortFromEq(Node cfgNode, String equipmentId) {
    List<TerminationPoint> tpList = new ArrayList<>();
    for (TerminationPoint tp : cfgNode.getTerminationPoint()) {
      if (tp.getTpId().getValue().contains(equipmentId)) {
        tpList.add(tp);
      }
    }

    return tpList;
  }

  /**
   * 通过比较需要吧cfg树上没有， 也在OP树上删除（如果设备上没有真实板卡）
   * @return
   */
  public Node del() {
    delEq();
    delTp();
    delXc();
    delInternalLink();
    return opNode;
  }

  private void delInternalLink() {
    List<InternalLinks> cfgList = cfgNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();
    List<InternalLinks> opList  = opNode.getAugmentation(Node1.class).getPhysical().getInternalLinks();

    if (opList == null)
      return;

    Iterator<InternalLinks> opIter = opList.iterator();
    while (opIter.hasNext()) {
      InternalLinks op = opIter.next();
      boolean found = false;
      for (InternalLinks cfg : cfgList) {
        if (cfg.getLinkRef().equals(op.getLinkRef())) {
          found = true;
          break;
        }
      }
      if (found) {
        //数据CFG/OP 一致
      } else {
        opIter.remove();
      }
    }

    opNode = new NodeBuilder(opNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(opList)
                .build())
            .build())
        .build();
  }

  private void delXc() {
    List<CrossConnections> cfgList = cfgNode.getAugmentation(Node1.class).getPhysical().getCrossConnections();
    List<CrossConnections> opList  = opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections();

    Iterator<CrossConnections> opIter = opList.iterator();
    while (opIter.hasNext()) {
      CrossConnections op = opIter.next();
      if (op.isFixed()) {
        continue;
      }

      boolean found = false;
      for (CrossConnections cfg : cfgList) {
        if (cfg.getCrossConnectionId().getValue().equals(op.getCrossConnectionId().getValue())) {
          found = true;
          break;
        }
      }
      if (found) {
        //config中还有，保持
      } else {
        //这种情况下config树上的交叉已经删除， op树也应该删除（正常deImpl成功后也应该消失了）
        opIter.remove();
        log.debug("remove xc in OP DB {}", op);
      }
    }

    opNode = new NodeBuilder(opNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                .setCrossConnections(opList)
                .build())
            .build())
        .build();
  }

  /**
   * TP点是不会被删除的，实际修改的只是connectionStatus, busy-->idle
   * 除非TP依赖的板卡已经删除了
   */
  private void delTp() {
    List<TerminationPoint> cfgList = cfgNode.getTerminationPoint();
    Iterator<TerminationPoint> opIter = opNode.getTerminationPoint().iterator();
    List<TerminationPoint> newTpList = new ArrayList<>();

    while (opIter.hasNext()) {
      TerminationPoint op = opIter.next();
      boolean found = false;
      TerminationPoint cfgTp = null;
      for (TerminationPoint cfg : cfgList) {
        if (cfg.getTpId().getValue().equals(op.getTpId().getValue())) {
          found = true;
          cfgTp = cfg;
          break;
        }
      }
      if (found) {
        //数据已OP为准
        if (cfgTp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus() != null &&
                cfgTp.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().equals(ConnectionStatus.Idle)) {
          newTpList.add(new TerminationPointBuilder(op)
                  .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                          .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(op.getAugmentation(TerminationPoint1.class).getPhysical())
                                  .setConnectionStatus(ConnectionStatus.Idle)
                                  .build())
                          .build())
                  .build());
        } else {
          newTpList.add(op);
        }
      } else {
        //remove this TP, (应该是对应的板块被删除了）
        log.debug("remove tp in OP-DB{}", op.getTpId());
      }
    }

    opNode = new NodeBuilder(opNode)
            .setTerminationPoint(newTpList)
            .build();
  }

  private boolean equipExistd(String equipId) {
    List<Equipments> opEqList = opNode.getAugmentation(Node1.class).getPhysical().getEquipments();
    for (Equipments eq : opEqList) {
      if (eq.getEquipmentId().equals(equipId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 比对OP数据库， 没有就不处理， 否则需要更新OP数据库， 因为UI显示的是OP数据库
   * . 网元上有真实板卡，不用删除
   * . 网元上没有真实板卡的，config消失的话，op也需要删除
   */
  private void delEq() {
    List<Equipments> cfgEqList = cfgNode.getAugmentation(Node1.class).getPhysical().getEquipments();
    List<Equipments> opEqList = opNode.getAugmentation(Node1.class).getPhysical().getEquipments();
    Iterator<Equipments> opIter = opEqList.iterator();

    AlignmentStatusType nodeAlignment = opNode.getAugmentation(Node1.class).getPhysical().getAlignmentStatus();

    boolean opChanged = false;
    while (opIter.hasNext()) {
      Equipments opEq = opIter.next();
      if (!opEq.isRemoveable()) {
        continue;
      }

      boolean found = false;
      for (Equipments equip : cfgEqList) {
        if (equip.getEquipmentId().equals(opEq.getEquipmentId())) {
          found = true;
          break;
        }
      }

      if (found) {
        //这种情况不用删除，因为config数据库还有这个板卡
        continue;
      } else {
        //需要删除，因为config数据库已经没有这个板卡了，并且这个板卡在网元上也没有 (removeable=true, serialNo=null)
        opChanged = true;
        if (opEq.getSerialNo() == null || opEq.getSerialNo().isEmpty()) {
          //remove it, thus doesn't add to newEqList
          opIter.remove();
          log.debug("remove eq in OP DB {}", opEq.getEquipmentId());
        } else {
          //不能删除，修改misMatch
          //TODO， misMatch 的值
        }
      }

    }

    if (opChanged) {
      opNode = new NodeBuilder(opNode)
              .addAugmentation(Node1.class, new Node1Builder()
                      .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                              .setAlignmentStatus(nodeAlignment)
                              .setEquipments(opEqList)
                              .build())
                      .build())
              .build();
    }
  }

  private Node removeResourceOnEq(String equipmentId) {
    Iterator<TerminationPoint> tpIter = opNode.getTerminationPoint().iterator();
    while (tpIter.hasNext()) {
      TerminationPoint tp = tpIter.next();
      if (tp.getTpId().getValue().contains(equipmentId)) {
        tpIter.remove();
      }
    }

    String transceiver = equipmentId.replaceFirst("LINECARD", "TRANSCEIVER");
    Iterator<Equipments> opIter = opNode.getAugmentation(Node1.class).getPhysical().getEquipments().iterator();
    while (opIter.hasNext()) {
      Equipments opEq = opIter.next();
      if (opEq.getEquipmentId().contains(transceiver)) {
        opIter.remove();
      }
    }


    Node newNode = new NodeBuilder(opNode)
        .addAugmentation(Node1.class, new Node1Builder()
            .setPhysical(new PhysicalBuilder(opNode.getAugmentation(Node1.class).getPhysical())
                .setEquipments(opNode.getAugmentation(Node1.class).getPhysical().getEquipments())
                .build())
            .build())
        .build();

    return newNode;
  }

}
