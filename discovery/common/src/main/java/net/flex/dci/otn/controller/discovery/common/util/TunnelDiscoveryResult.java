package net.flex.dci.otn.controller.discovery.common.util;

import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;

import java.util.List;

@Builder
@Data
public class TunnelDiscoveryResult {
  String tunnelId;
  String friendlyName;
  ImplementState finalState;

  List<NodeResult> nodesCompareResult;

  @Builder
  @Data
  public static class NodeResult {
    TunnelDiscoveryResult.ObjectResult nodeBasic;
    List<TunnelDiscoveryResult.ObjectResult> eqList;
    List<TunnelDiscoveryResult.ObjectResult> xcList;
    List<TunnelDiscoveryResult.ObjectResult> tpList;
  }


  @Builder
  @Data
  public static class ObjectResult {
    String id;
    String friendlyName;  //node will include ip
    boolean conflict;
    String detailMessage;
  }
}
