package net.flex.dci.otn.controller.discovery.discovery1.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.discovery.common.impl.TunnelMachine;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DiscoveryOnTTI {

  private static DiscoveryOnTTI inst = new DiscoveryOnTTI();

  //will use thread pool next time
  List<String> processing = new ArrayList<>();
  TunnelDao tunnelDao;
  PhyNodeDao phyNodeDao;

  public DiscoveryOnTTI() {
    tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
    phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
  }


  public static DiscoveryOnTTI getInstance() {
    return inst;
  }

  private void recover_BasedOnTTI(String tunnelId) {
    Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
    if (tunnel == null) {
      log.debug("required tunnelId hasn't found in DB  {}", tunnelId);
      return;
    }

    String srcTpId = tunnel.getSourceTp().get(0).getTpRef().getValue();
    String srcNodeId = PhysicalTpIdNamingRule.getNodeId(srcTpId);
    String dstTpId = tunnel.getDestinationTp().get(0).getTpRef().getValue();
    String dstNodeId = PhysicalTpIdNamingRule.getNodeId(dstTpId);

    //for srcTP
    String sentTTI = getTTIValue(srcNodeId, srcTpId);
    String expectedTTI = getTTIValue(srcTpId, srcNodeId);

    NeManagerRpc neManagerRpc = SpringBeanFinder.getBean(NeManagerRpc.class);
    neManagerRpc.configNe(sentTTINe(srcNodeId, srcTpId, sentTTI, expectedTTI));
    neManagerRpc.configNe(sentTTINe(dstNodeId, dstTpId, expectedTTI, sentTTI));
    try {
      TimeUnit.SECONDS.sleep(10);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    //after 1 seconds, the destionation TP will receive the sent value
    //但是我们需要等待一段时间让程序完成网元到Adapter的同步，Adapter到Controller的同步
    //TODO
    // 应该可以直接要求访问Ne neManagerRpc.getNeData()
    boolean matched = doesMatch(dstNodeId, dstTpId, sentTTI);
    if (matched) {
      ChangedObject changedObject = new ChangedObject();
      new TunnelMachine(changedObject, tunnel).markImpl();
      MultipleTransaction multipleTransaction = SpringBeanFinder.getBean(
              MultipleTransaction.class);
      multipleTransaction.save(changedObject);
    }
  }

  /**
   * @param nodeId
   * @param tpId
   * @param receivedTTI
   * @return
   */
  private boolean doesMatch(String nodeId, String tpId, String receivedTTI) {
    log.debug("when to check received TTI {}", receivedTTI);
    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp = phyNodeDao.getOpPhyTpById(
            nodeId, tpId);
    Properties properties = tp.getAugmentation(TerminationPoint1.class).getPhysical()
            .getProperties();
    if (properties == null && properties.getProperty() == null) {
      return false;
    }

    for (Property prop : properties.getProperty()) {
      if (prop.getName().equals("tti-msg-recv")) {
        if (prop.getValue().equals(receivedTTI)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * TTI include sent and expected value, the sent and expected both based on TPID
   *
   * @return
   */

  private ConfigNeInput sentTTINe(String nodeId, String tpId, String sentTTI,
                                  String expectedTTI) {
    log.debug("setting TTI info on tp {}, sent {}, expected {}", tpId, sentTTI, expectedTTI);

    Properties properties = new PropertiesBuilder().setProperty(new ArrayList<>()).build();
    properties.getProperty().add(property("tti-msg-auto", "false"));
    properties.getProperty().add(property("tti-msg-transmit", sentTTI));
    properties.getProperty().add(property("tti-msg-expected", expectedTTI));

    TerminationPoint tp = new TerminationPointBuilder()
            .setTpId(new TpId(tpId))
            .setKey(new TerminationPointKey(new TpId(tpId)))
            .setPhysical(new PhysicalBuilder().setProperties(properties)
                    .build())
            .build();

    ConfigNeInput configNe = new ConfigNeInputBuilder()
            .setNodeId(new NodeId(nodeId)).setTerminationPoint(new ArrayList<>())
            .setPhysical(
                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                            .setNodeType(NodeType.TPC4)
                            .build())
            .build();
    configNe.getTerminationPoint().add(tp);
    return configNe;
  }

  private String getTTIValue(String nodeId, String tpId) {
    String[] tmp = tpId.split("-");
    String tpKey = "";
    for (int index = tmp.length - 3; index < tmp.length; index++) {
      tpKey += tmp[index];
    }
    return nodeId.substring(nodeId.length() - 5) + tpKey;
  }

  private Property property(String key, String value) {
    return new PropertyBuilder()
            .setName(key)
            .setValue(value)
            .setKey(new PropertyKey(key))
            .build();
  }

  private boolean hasPorcessing(String tunnelId) {
    for (String id : processing) {
      if (id.equals(tunnelId)) {
        return true;
      }
    }
    return false;
  }
}
