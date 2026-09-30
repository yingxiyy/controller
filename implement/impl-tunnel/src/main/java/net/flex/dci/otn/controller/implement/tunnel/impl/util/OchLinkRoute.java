package net.flex.dci.otn.controller.implement.tunnel.impl.util;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otn.controller.implement.tunnel.impl.frequency.Frequency;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;

import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
public class OchLinkRoute {
  private Link ochLink;

  public OchLinkRoute(Link ochLink) {
    this.ochLink = ochLink;
  }

  public static List<String> getSiteLinkIds(Link ochLink)  throws CommonException {
    List <String> siteLinkIds = new ArrayList<>();
    for (Route route : ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute()) {
      if (route.getPrimary() == null) {
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("OchLink[%s] route info error", ochLink.getLinkId().getValue()));
      }
      siteLinkIds.addAll(getAllSiteLinksWithEro(route.getPrimary().getExplicitRouteObjects()));
      if (route.getSecondary() != null) {
        siteLinkIds.addAll(getAllSiteLinksWithEro(route.getSecondary().getExplicitRouteObjects()));
      }
      if (route.getThird() != null) {
          for (Third tr : route.getThird()) {
              siteLinkIds.addAll(getAllSiteLinksWithEro(tr.getExplicitRouteObjects()));
          }
      }
    }

    return siteLinkIds;
  }

  public static List<String> getThirdSiteLinkIds(Link ochLink) {
    Set<String> siteLinkIds = new LinkedHashSet<>();
    for (Route route : ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute()) {
      if (route.getThird() == null) {
        continue;
      }
      for (Third third : route.getThird()) {
        siteLinkIds.addAll(getAllSiteLinksWithEro(third.getExplicitRouteObjects()));
      }
    }
    return new ArrayList<>(siteLinkIds);
  }

  private static Collection<String> getAllSiteLinksWithEro(List<ExplicitRouteObjects> explicitRouteObjects) {
    List <String> siteLinkIds = new ArrayList<>();
    for (ExplicitRouteObjects ero : explicitRouteObjects) {
      for (PathRouteObject pathRouteObject : ero.getPathRouteObject()) {
        if (pathRouteObject.getResourceType().getImplementedInterface().getName()
                .equals(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName())) {
          LinkHop hop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pathRouteObject
                  .getResourceType()).getLinkHop();
          if (hop.getTopologyRef().getValue().equals(SiteTopology.QNAME.getLocalName())) {
            siteLinkIds.add(hop.getLinkRef().getValue());
          }
        }
      }
    }
    return siteLinkIds;
  }

  public static List<String> getSupportedTunnel(Link ochLink) {
    List <String> tunnelIds = new ArrayList<>();

    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 supported = ochLink
            .getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
    for (SupportedTunnel supTunnel : supported.getSupportedTunnel()) {
      tunnelIds.add(supTunnel.getTunnelRef().getValue());
    }
    return tunnelIds;
  }

  /**
   * 通过ochLink找到对应有M?D?的端口
   * @param ochLink
   * @param frequency
   * @return
   */
  public static List<String> getAllMuxChannelTps(Link ochLink, Frequency frequency) {
    List<String> tpIds = new ArrayList<>();

    for (Route route : ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute()) {
      for (CrossConnections xc : route.getPrimary().getCrossConnections()) {
        addMuxChannelTp(tpIds, xc, frequency);
      }
      if (route.getSecondary() != null) {
        for (CrossConnections xc : route.getSecondary().getCrossConnections()) {
          addMuxChannelTp(tpIds, xc, frequency);
        }
      }
    }
    return tpIds;
  }

  private static void addMuxChannelTp(List<String> tpIds, CrossConnections xc, Frequency frequency) {
    if (frequency.hasMuxChannel(xc.getCrossConnectionId().getValue())) {
      for (SourceTp sTp : xc.getSourceTp()) {
        String tpId = sTp.getTpRef().getValue();
        if (frequency.hasMuxChannel(tpId)) {
          tpIds.add(tpId);
        }
      }
      for (DestinationTp dTp : xc.getDestinationTp()) {
        String tpId = dTp.getTpRef().getValue();
        if (frequency.hasMuxChannel(tpId)) {
          tpIds.add(tpId);
        }
      }
    }
  }

  public Link replaceXC(Link ochLink, BigInteger oldCentFrequency, Frequency frequency, boolean isFix, Map<String, CrossConnectionAttributes> newXCList) {
    Map<String, CrossConnectionAttributes> newPrimaryXCMap = null;
    Map<String, CrossConnectionAttributes> newSecondaryXCMap = null;
    Map<String, CrossConnectionAttributes> newThirdXCMap = null;

    Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
    List<Route> newRouteList = new ArrayList<>();

    Iterator<Route> iter = ochLinkAttr.getExplictRoute().getRoute().iterator();
    while (iter.hasNext()) {
      Route route = iter.next();
      if (!isFix) {
        newPrimaryXCMap = updateXcFrequency(route.getPrimary().getCrossConnections(), frequency);
        if (route.getSecondary() != null) {
          newSecondaryXCMap = updateXcFrequency(route.getSecondary().getCrossConnections(), frequency);
        }
        if (route.getThird() != null && !route.getThird().isEmpty()) {
          newThirdXCMap = updateXcFrequency(route.getThird().get(0).getCrossConnections(), frequency);
        }
      } else {
        newPrimaryXCMap = updateXcFrequencyAndTp(route.getPrimary().getCrossConnections(), frequency);
        if (route.getSecondary() != null) {
          newSecondaryXCMap = updateXcFrequencyAndTp(route.getSecondary().getCrossConnections(), frequency);
        }
        if (route.getThird() != null && !route.getThird().isEmpty()) {
          newThirdXCMap = updateXcFrequencyAndTp(route.getThird().get(0).getCrossConnections(), frequency);
        }
      }

      List<CrossConnections> newPrimaryXCList = null;
      List<CrossConnections> newSecondaryXCList = null;
      List<CrossConnections> newThirdXCList = null;
      if (newPrimaryXCMap != null) {
        AtomicLong index = new AtomicLong();
        newPrimaryXCList = newPrimaryXCMap.values().stream().map(xc -> {
          index.getAndIncrement();
          return new CrossConnectionsBuilder(xc)
                  .setSequence(index.get())
                  .setKey(new CrossConnectionsKey(index.get()))
                  .build();
        }).collect(Collectors.toList());
      }
      if (newSecondaryXCMap != null) {
        AtomicLong index = new AtomicLong();
        newSecondaryXCList = newSecondaryXCMap.values().stream().map(xc -> {
          index.getAndIncrement();
          return new CrossConnectionsBuilder(xc)
                  .setSequence(index.get())
                  .setKey(new CrossConnectionsKey(index.get()))
                  .build();
        }).collect(Collectors.toList());
      }
      if (newThirdXCMap != null) {
        AtomicLong index = new AtomicLong();
        newThirdXCList = newThirdXCMap.values().stream().map(xc -> {
          index.getAndIncrement();
          return new CrossConnectionsBuilder(xc)
                  .setSequence(index.get())
                  .setKey(new CrossConnectionsKey(index.get()))
                  .build();
        }).collect(Collectors.toList());
      }

      List<Third> thirds = new ArrayList<>();
      if (newThirdXCList != null) {
        thirds.add(new ThirdBuilder(route.getThird().get(0)).setCrossConnections(newThirdXCList).build());
      }

      Route newRoute = new RouteBuilder(route)
              .setPrimary(new PrimaryBuilder(route.getPrimary()).setCrossConnections(newPrimaryXCList).build())
              .setSecondary(newSecondaryXCList == null ? null : new SecondaryBuilder(route.getSecondary()).setCrossConnections(newSecondaryXCList).build())
              .setThird(newThirdXCList == null ? null : thirds)
              .build();

      newRouteList.add(newRoute);
    }

    //for update nodeXC
    if (newPrimaryXCMap != null)
      newXCList.putAll(newPrimaryXCMap);
    if (newSecondaryXCMap != null)
      newXCList.putAll(newSecondaryXCMap);
    if (newThirdXCMap != null)
      newXCList.putAll(newThirdXCMap);

    //update xc inside ochLink route
    return new LinkBuilder(ochLink).addAugmentation(Link1.class,
                    new Link1Builder()
                            .setOch(new OchBuilder(ochLinkAttr)
                                    .setExplictRoute(new ExplictRouteBuilder(
                                            ochLinkAttr.getExplictRoute())
                                            .setRoute(newRouteList)
                                            .build())
                                    .build())
                            .build())
            .build();

  }

  private Map<String, CrossConnectionAttributes> updateXcFrequencyAndTp(List<CrossConnections> oldXcList, Frequency frequency) {
    Map<String, CrossConnectionAttributes> newXCMap = new HashMap<>();

    for (CrossConnections oldXC : oldXcList) {
      String oldXcId = oldXC.getCrossConnectionId().getValue();
      if (frequency.hasMuxChannel(oldXcId)) {
        CrossConnectionAttributes newXc = getNewMuxXC(oldXC, frequency, true);
        newXCMap.put(oldXcId, new CrossConnectionsBuilder(newXc).setSequence(oldXC.getSequence()).setKey(oldXC.getKey()).build());
      } else {
        newXCMap.put(oldXcId, oldXC);
      }
    }
    return newXCMap;
  }

  private Map<String, CrossConnectionAttributes> updateXcFrequency(List<CrossConnections> crossConnections, Frequency frequency) {
    Map<String, CrossConnectionAttributes> newXcMap = new HashMap<>();

    long lower = Long.valueOf(frequency.getLowerFrequecy()).longValue();
    long upper = Long.valueOf(frequency.getUpperFrequecy()).longValue();
    long center = frequency.getCentFreq().longValue();

    crossConnections.forEach(oldXC-> {
      if (CrossConnectionSlotNamingRule.isFrequencyXc(oldXC)) {
        CrossConnectionAttributes newXC = CrossConnectionSlotNamingRule.setFrequencyScope(oldXC, lower, upper, center);
        newXcMap.put(oldXC.getCrossConnectionId().getValue(), newXC);
      } else {
        newXcMap.put(oldXC.getCrossConnectionId().getValue(), oldXC);
      }
    });

    return newXcMap;
  }

  private CrossConnectionAttributes getNewMuxXC(CrossConnections oldXC, Frequency frequency, boolean isFix) {
    String newXcId = frequency.replaceXCIdFrequency(oldXC.getCrossConnectionId().getValue());

    if (isFix) {
      //fix的情况下，TP点发生变化
      newXcId = frequency.replaceMuxChannelTpId(newXcId);
    }
    String tpSlot = "/frequency=" + frequency.getLowerFrequecy() + "," + frequency.getUpperFrequecy();

    List<DestinationTp> dTps = new ArrayList<>();
    for (DestinationTp dTp : oldXC.getDestinationTp()) {
      if (frequency.hasMuxChannel(dTp.getTpRef().getValue())) {
        DestinationTp new_dTp = new DestinationTpBuilder()
                .setTpRef(isFix ? new TpId(frequency.replaceMuxChannelTpId(dTp.getTpRef().getValue())) : dTp.getTpRef() )
                .setSlot(tpSlot)
                .build();
        dTps.add(new_dTp);
      } else {
        DestinationTp new_dTp = new DestinationTpBuilder()
                .setTpRef(dTp.getTpRef())
                .setSlot(tpSlot)
                .build();
        dTps.add(new_dTp);
      }
    }

    List<SourceTp> sTps = new ArrayList<>();
    for (SourceTp sTp : oldXC.getSourceTp()) {
      if (frequency.hasMuxChannel(sTp.getTpRef().getValue())) {
        SourceTp new_sTp = new SourceTpBuilder()
                .setTpRef(isFix ? new TpId(frequency.replaceMuxChannelTpId(sTp.getTpRef().getValue())) : sTp.getTpRef())
                .setSlot(tpSlot)
                .build();
        sTps.add(new_sTp);
      } else {
        SourceTp new_sTp = new SourceTpBuilder()
                .setTpRef(sTp.getTpRef())
                .setSlot(tpSlot)
                .build();
        sTps.add(new_sTp);
      }
    }

    //"description" : "MUXPANEL-1-50-M2D2/191400000"
    String[] tmp =  oldXC.getDescription().split("/");
    String description = tmp[0] + "/" + frequency.getCentFreq().toString();
    if (isFix) {
      //TP 点变化
      String newTpId = frequency.replaceMuxChannelTpId(tmp[0]);
      description = newTpId + "/" + frequency.getCentFreq().toString();
    }
    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder newXCBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder(oldXC)
            .setCrossConnectionId(new Uri(newXcId))
            .setDestinationTp(dTps)
            .setSourceTp(sTps)
            .setDescription(description);

    return newXCBuilder.build();
  }

  public Link replaceTpAndLink(Link ochLink, Frequency frequency) {
    Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
    List<Route> newRouteList = new ArrayList<>();

    Iterator<Route> iter = ochLinkAttr.getExplictRoute().getRoute().iterator();
    while (iter.hasNext()) {
      Route route = iter.next();
      List<ExplicitRouteObjects> newPrimaryEroList = new ArrayList<>();
      for (ExplicitRouteObjects ero : route.getPrimary().getExplicitRouteObjects()) {
        List<PathRouteObject> newProList = updatePathRoute(ero, frequency);
        newPrimaryEroList.add(new ExplicitRouteObjectsBuilder(ero).setPathRouteObject(newProList).build());
      }
      List<ExplicitRouteObjects> newSecondaryEroList = null;
      if (route.getSecondary() != null) {
        newSecondaryEroList = new ArrayList<>();
        for (ExplicitRouteObjects ero : route.getSecondary().getExplicitRouteObjects()) {
          List<PathRouteObject> newProList = updatePathRoute(ero, frequency);
          newSecondaryEroList.add(new ExplicitRouteObjectsBuilder(ero).setPathRouteObject(newProList).build());
        }
      }

      if (newSecondaryEroList != null) {
        newRouteList.add(new RouteBuilder(route)
                .setPrimary(new PrimaryBuilder(route.getPrimary())
                        .setExplicitRouteObjects(newPrimaryEroList)
                        .build())
                .setSecondary(new SecondaryBuilder(route.getSecondary())
                        .setExplicitRouteObjects(newSecondaryEroList)
                        .build())
                .build());
      } else {
        newRouteList.add(new RouteBuilder(route)
                .setPrimary(new PrimaryBuilder(route.getPrimary())
                        .setExplicitRouteObjects(newPrimaryEroList)
                        .build())
                .build());
      }
    }
    return new LinkBuilder(ochLink).addAugmentation(Link1.class,
                    new Link1Builder()
                            .setOch(new OchBuilder(ochLinkAttr)
                                    .setExplictRoute(new ExplictRouteBuilder(
                                            ochLinkAttr.getExplictRoute())
                                            .setRoute(newRouteList)
                                            .build())
                                    .build())
                            .build())
            .build();
  }

  private List<PathRouteObject> updatePathRoute(ExplicitRouteObjects ero, Frequency frequency) {

    List<PathRouteObject> newProList = new ArrayList<>();
    for (PathRouteObject pro : ero.getPathRouteObject()) {
      if (pro.getResourceType().getImplementedInterface().getName().equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName())) {
        LinkHop linkHop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro.getResourceType()).getLinkHop();
        String linkId = linkHop.getLinkRef().getValue();
        if (frequency.hasMuxChannel(linkId)) {
          LinkHop newLinkHop = new LinkHopBuilder(linkHop).setLinkRef(new LinkId(frequency.replaceMuxChannelTpId(linkId))).build();
          newProList.add(new PathRouteObjectBuilder(pro)
                  .setResourceType(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.LinkBuilder().setLinkHop(newLinkHop).build())
                  .build());
        } else {
          newProList.add(pro);
        }
      } else {
        if (pro.getResourceType().getImplementedInterface().getName().equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp.class.getName())) {
          TpHop tpHop = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) pro.getResourceType()).getTpHop();
          String tpId = tpHop.getTpRef().getValue();
          if (frequency.hasMuxChannel(tpId)) {
            TpHop newTpHop = new TpHopBuilder(tpHop).setTpRef(new TpId(frequency.replaceMuxChannelTpId(tpId))).build();
            newProList.add(new PathRouteObjectBuilder(pro)
                    .setResourceType(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.TpBuilder().setTpHop(newTpHop).build())
                    .build());
          } else {
            newProList.add(pro);
          }
        }
      }
    }
    return newProList;
  }


  public Link replaceSupportingLink(Link ochLink, Frequency frequency, List<String> oldLinkIdList) {
    List<SupportingLink> newSupportingList = new ArrayList<>();
    for (SupportingLink sl : ochLink.getSupportingLink()) {
      String linkId = sl.getLinkRef().getValue();
      if (frequency.hasMuxChannel(linkId)) {
        oldLinkIdList.add(linkId);
        LinkId newLinkId = new LinkId(frequency.replaceMuxChannelTpId(linkId));
        newSupportingList.add(new SupportingLinkBuilder(sl)
                .setLinkRef(newLinkId)
                .setKey(new SupportingLinkKey(newLinkId))
                .build());
      } else {
        newSupportingList.add(sl);
      }
    }
    return new LinkBuilder(ochLink).setSupportingLink(newSupportingList).build();
  }

}
