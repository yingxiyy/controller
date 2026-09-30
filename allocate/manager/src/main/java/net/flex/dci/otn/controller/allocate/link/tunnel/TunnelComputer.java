/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TopologyDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.Prot100GE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnelsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnelsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfoKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequenceKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result.Result;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result.ResultBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result.ResultKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info.RouteBundleInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info.RouteBundleInfoKey;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


@Slf4j
public class TunnelComputer {

  private OchLinkDao ochLinkDao;
  private SiteNodeDao siteNodeDao;
  private SiteLinkDao siteLinkDao;
  private TopologyDao topologyDao;

  //==================key value for doIt tunnel
  private ParamCompute param;
  //===================


  public TunnelComputer() {
    siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
    topologyDao = SpringBeanFinder.getBean(TopologyDao.class);
    siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
    ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
  }

  public ComputeTunnelsOutput doIt(ComputeTunnelsInput input) throws CommonException {
    param = new ParamCompute();
    param.parser(input);

    ComputeTunnelsOutput output = pointToPoint();
    return output;
  }


  private ComputeTunnelsOutput pointToPoint() throws CommonException {
    List<Map<String, List<Link>>> solution = getPossibleSiteLinks();

    ComputeTunnelsOutput output = buildOutput(solution);

    return output;
  }

  /**
   * 参数的含义参考方法getPossibleSiteLinks 中的解释
   *
   * @param solutionList
   * @return
   */
  private ComputeTunnelsOutput buildOutput(List<Map<String, List<Link>>> solutionList) {
    ComputeTunnelsOutput output = null;

    if (solutionList.isEmpty()) {
      output = new ComputeTunnelsOutputBuilder()
              .setReturnCode(RpcResultType.NoEnoughResources)
              .build();
    } else {
      output = new ComputeTunnelsOutputBuilder()
              .setResult(new LinkedList<>())
              .build();
    }
    short index = 1;
    for (Map<String, List<Link>> solution : solutionList) {
      Result result = new ResultBuilder()
              .setGroup(index)
              .setKey(new ResultKey(index))
              .setRouteBundleInfo(new LinkedList<>()).build();

      for (String vendor : solution.keySet()) {
        RouteBundleInfoBuilder rb = new RouteBundleInfoBuilder(param.getVendorOccupationRate(vendor))
                .setBundleNumber(param.getVendorTunnelNumber(vendor))
                .setNumber(null)
                .setRouteInfo(new LinkedList<>())
                .setRiskGroupName(param.getRiskGroupName())
                .setPlaneName(param.getPlaneName());
        rb.setKey(new RouteBundleInfoKey(rb.getProductType(), rb.getVendorName()));
        result.getRouteBundleInfo().add(rb.build());

        RouteInfo routeInfo = new RouteInfoBuilder()
                .setIndex((short) 1)
                .setKey(new RouteInfoKey((short) 1))
                .setPrimary(new PrimaryBuilder()
                        .setRouteSequence(new LinkedList<>())
                        .build())
                .build();

        updateRouteSequence(routeInfo, solution.get(vendor));
        rb.getRouteInfo().add(routeInfo);
      }
      output.getResult().add(result);
    }

    return output;
  }

  private void updateRouteSequence(RouteInfo routeInfo, List<Link> siteLinkList) {

    long seq = 0;
    for (Link ntSiteLink : siteLinkList) {
      seq++;

      ntSiteLink.getSupportingLink();
      Site siteLink = ntSiteLink.getAugmentation(Link1.class).getSite();
      routeInfo.getPrimary().getRouteSequence().add(new RouteSequenceBuilder()
              .setResourceType(new LinkBuilder()
                      .setLinkHop(new LinkHopBuilder()
                              .setDestination(ntSiteLink.getDestination())
                              .setPhysical(new PhysicalBuilder()
                                      .setFriendlyName(siteLink.getFriendlyName())
                                      .build())
                              .setLinkId(ntSiteLink.getLinkId())
                              .setSource(ntSiteLink.getSource())
//                              .setSupportingLink(ntSiteLink.getSupportingLink())
                              .build())
                      .build())
              .setSequence(seq)
              .setTopologyRef(new TopologyId(TopoNameConstants.Site_Topo_Key))
              .setKey(new RouteSequenceKey(seq))
              .build());
    }
  }

  /**
   * 先简单计算所有的已经符合riskGroup/plan, 必经、比不经资源的siteLinks中 粗略检查，只要siteLink中剩余的OCH数量>=需要的OCH数量即可， (不对，因为厂家不同，同一个OCH只能承载一个厂家， 所以改为下面的算法） 是否有足够的ODU完成创建需求，需要考虑： OCH只能是相同厂家，板卡型号，L口光模块配置
   *
   * @return 返回值是某条siteLink支持多少业务 的集合  //vendor, siteLink
   *
   *     外层的list表示有几种创建的可能性的路由， 只输出3种
   *     第二层的Map表示每个厂家（key）， List<Link> 表示路由构成
   *     在点到点模式，List成员只有一个
   *     在ROADM模式下，List的成员可能是多个siteLink, siteLink与siteLink间需要创建OCH XC
   *
   */
  private List<Map<String, List<Link>>> getPossibleSiteLinks()  throws CommonException {
    log.debug("get possible site link");
    List<Map<String, List<Link>>> result = new LinkedList<>();

    int clientLineRate = param.getClientLineRate();
    for (Link siteLink : param.getPossibleSiteLinks()) {
      //现在没有考虑ROADM的情况， OCH和siteLink是同样长度，中间没OCH XC 转接
      Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
      int siteLinkFreeOch = Integer.parseInt(siteLinkAttr.getBandwidth());

      boolean enoughResource = false;
      if (siteLinkFreeOch >= Math.ceil(param.getBundleNumber() / (double)clientLineRate)) {
        //新建的Tunnel全部用新OCH的情况下SiteLink上剩余的OCH 也是足够的
        enoughResource = true;
      } else {
        //求取离散资源
        //分散在各个已经创建的OCH上的符合要求的ODU数目
        Map<String, Integer> vendorFreeOduMap = getFreeOduFromOch(siteLinkAttr.getSupportedLink());
        for (String vendor : vendorFreeOduMap.keySet()) {
          int requiredOdu = param.getVendorTunnelNumber(vendor);
          int totalFree = siteLinkFreeOch * clientLineRate + vendorFreeOduMap.get(vendor);
          int diff = totalFree - requiredOdu;
          if (diff < 0) {
            //OCH上离散的ODU数目 和siteLink种可以创建的tunnel数目 都不够
            //Hasn't enough for required tunnel creation
            enoughResource = false;
            break;
          } else {
            enoughResource = true;
          }
        }
      }

      if (enoughResource) {
        Map<String, List<Link>> route = new HashMap<>();
        for (VendorOccupationRate occupationRate : param.getVendorOccupationRateList()) {
          if (!route.containsKey(occupationRate.getVendorName())) {
            route.put(occupationRate.getVendorName(), new LinkedList<>());
          }
          route.get(occupationRate.getVendorName()).add(siteLink);
        }
        result.add(route);
      }

      if (result.size() == 3) {
        //只要3种情况
        break;
      }
    }

    if (result.isEmpty()) {
      throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE, "hasn't enough channel for required.");
    }
    return result;
  }

  private Map<String, Integer> getFreeOduFromOch(List<SupportedLink> supportedLink) {
    Map<String, Integer> vendorFreeOduMap= new HashMap<>();  //每个厂家对应已经创建的OCH上剩余ODU
    for (SupportedLink sl : supportedLink) {
      //siteLink支撑的客户层就是OCH Link （只统计板卡相同的）
      //这些已经创建的OCH link 如果板卡相同，检查上是否还有free odu
      Link ochLink = ochLinkDao.getOchLinkByLinkId(sl.getLinkRef().getValue());
      Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
      int freeOdus = getFreeOdu(ochLinkAttr);
      if (freeOdus == 0)
        continue;

      String vendor = getVendorWithTunnelCard(ochLinkAttr, param.getVendorOccupationRateList(), param.getCardType());
      if (vendor == null) {
        continue;  // 这条OCH 与要求的vendor， card Type不符
      }

      if (vendorFreeOduMap.containsKey(vendor)) {
        vendorFreeOduMap.put(vendor, vendorFreeOduMap.get(vendor) + freeOdus);
      } else {
        vendorFreeOduMap.put(vendor, freeOdus);
      }
    }
    return vendorFreeOduMap;
  }

  private int getFreeOdu(Och ochLink) {
    if (ochLink.getAvailable() == null)
      return 0;

    for (Available avaOdu : ochLink.getAvailable()) {
      if (avaOdu.getSupportedOduj().equals(param.getTunnelOdu())) {
        String available = avaOdu.getAvailableOdujSlot();
        if (available.isEmpty())
          return 0;
        else {
          String[] tmp = available.split("-");
          return  tmp.length;
        }
      }
    }

    return 0;
  }

  /**
   * 检查此OCH link只那个厂家的那种板卡 如果板卡类似不是创建Tunnel时指定的那一款，输出null 如果是同款，但是vendor信息不在创建Tunnel种的vendor列表种， 即没有要求创建这个vendor的tunnel，输出null
   *
   * 输出null表示，这条OCH的空闲资源没用
   *
   * @param ochLink
   * @param vendorOccupationRateList
   * @param requiredCardType
   * @return
   */
  public String getVendorWithTunnelCard(Och ochLink, List<VendorOccupationRate> vendorOccupationRateList, String requiredCardType) {
    String vendor = "";
    String card = "";
    if (ochLink.getProperties() == null)
      return null;

    for (Property pro : ochLink.getProperties().getProperty()) {
      if (pro.getName().equals("vendor-name")) {
        vendor = pro.getValue();
      } else if (pro.getName().equals("card-type")) {
        card = pro.getValue();
      }
    }

    for (VendorOccupationRate rate : vendorOccupationRateList) {
      if (card.equals(requiredCardType) && rate.getVendorName().equals(vendor)) {
        return vendor;
      }
    }
    return null;
  }
}