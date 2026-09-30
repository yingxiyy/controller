package net.flex.dci.otn.controller.allocate.link.common;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ReusedNodesSnapshot;

/**
 * this used for siteLink resue node and create-tunnel-2
 */
public class ReuseNodeChecker {
  ChangedObject changedObject;

  public ReuseNodeChecker(ChangedObject changedObject) {
    this.changedObject = changedObject;
  }

  /**
   * snapshot中有两项，一个是config树的，一个是op树的。
   * 在分配资源的时候，如果有op树，实际是按照OP树上资源进行计算
   *    * 因为UI看到的数据就是OP树的（如果有的话）,
   *    * 一个equip(包括transceiver)被Link使用了由属性"used-in-link" = true 表达
   *    * 数据合法性需要检查 板卡类型相同，used-in-link=false;
   * @param reusedNodesSnapshot
   * @throws CommonException
   */
  public void checkInitialEnv(ReusedNodesSnapshot reusedNodesSnapshot) throws CommonException {
    if (reusedNodesSnapshot != null && reusedNodesSnapshot.getConfig() != null && reusedNodesSnapshot.getConfig().getNodes() != null) {
      for (Nodes reuseNode : reusedNodesSnapshot.getConfig().getNodes()) {
        Node dbNode = changedObject.getChangedPhyNode(reuseNode.getNodeId());
        checkResource(reuseNode, dbNode);
      }
      if (reusedNodesSnapshot.getOp() != null && reusedNodesSnapshot.getOp().getNodes() != null) {
        for (Nodes reuseNode : reusedNodesSnapshot.getOp().getNodes()) {
          Node dbNode = changedObject.getChangedPhyOpNode(reuseNode.getNodeId());
          checkResource(reuseNode, dbNode);
        }
      }
    }
  }

  private void checkResource(Nodes reuseNode, Node dbNode) throws CommonException {
    Physical phyAttr = dbNode.getAugmentation(Node1.class).getPhysical();
    for (PhyEquipAttributes dbEq : phyAttr.getEquipments()) {
      boolean found = true;
      for(PhyEquipAttributes reuseEq : reuseNode.getEquipments()) {
        if (reuseEq.getEquipType().equals(EquipType.EMPTY))
          continue;

        if (dbEq.getEquipmentId().equals(reuseEq.getEquipmentId())) {
          if (dbEq.getEquipType().equals(reuseEq.getEquipType()) && (dbEq.isUsedInLink() == null || !dbEq.isUsedInLink())) {
            found = true;
          }
        }

        break;
      }
      if (!found) {
        throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
            String.format("利旧设备 %s(%s) 已经被使用了", phyAttr.getFriendlyName(), dbEq.getFriendlyName()));
      }
    }
  }
}
