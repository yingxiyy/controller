/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.ArrayList;
import java.util.List;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.component.resource.frequency.FrequencyMap;
import net.flex.dci.otn.controller.nms.nms.dto.FrequencyMapDto;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OmsLinkOtsLinkInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.tunnel.TunnelBetweenSitePagedInfo;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetBoardLldpInfoInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardPortsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardTypeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetDesignSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEnvPropertyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetObjectDetailsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOpsConnectionsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOtsLinksUnderOmsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRackPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRealMpoPortInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRelatedSiteLinksInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSchedulePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSchedulePagedOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSimpleRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkByNodeIpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkOtsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkRelatedTunnelsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinksBetweenTwoSitesInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelBetweenSitePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelSiteRouteOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetVersionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkGroupbyPlaneOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ListViewPlaneOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RemoveResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResynchronizeNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResynchronizeNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RouteDisplayInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncEquipmentInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncEquipmentOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncTerminationPointInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncTerminationPointOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateSystemInfoOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.connection.client.server.relation.Tunnel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.board.lldp.info.output.LldpInfos;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.ops.connections.output.OpsConnections;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.real.mpo.port.output.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.related.site.links.output.RelatedSiteLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.link.related.tunnels.output.RelatedTunnels;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.links.between.two.sites.output.LinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.output.RouteDisplayInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;

/**
 * @author: xinyzhao
 * @date: 2021/4/8
 */
public interface IBaseNMSOperations {


    /**
     * get-route
     *
     * @return
     * @throws Exception
     */
    default List<RouteInfo> getRoute(GetRouteInput input)
            throws Exception {
        return null;
    }

    /**
     * get-version
     *
     * @return
     */
    default GetVersionOutput getVersion() throws Exception {
        return null;
    }

    /**
     * get tunnel
     *
     * @return
     * @throws Exception
     */
    default List<Tunnel> getTunnel(GetTunnelInput input) throws Exception {
        return null;
    }

    /**
     * get Tunnel paged
     *
     * @param input
     * @return
     * @throws Exception
     */
    default PageResult<Tunnel> getTunnelPaged(GetTunnelPagedInput input) throws Exception {
        return null;
    }


    /**
     * get phy link
     *
     * @param input
     * @return
     */
    default List<Link> getPhyLink(GetPhyLinkInput input) throws Exception {
        return null;
    }


    /**
     * @param input
     * @return
     * @throws Exception
     */
    default PageResult<Link> getPhyLinkPaged(GetPhyLinkPagedInput input) throws Exception {
        return null;
    }


    /**
     * get-rack
     *
     * @return
     */
    default List<Node> getRacks(GetRackInput input) throws Exception {
        return null;
    }

    /**
     * get rack paged
     *
     * @param input
     * @return
     */
    default PagedList getRackPaged(GetRackPagedInput input) throws Exception {
        return null;
    }

    /**
     * get site link
     *
     * @return
     * @throws Exception
     */
    default List<Link> getSiteLink(GetSiteLinkInput input) throws Exception {
        return null;
    }


    /**
     * get site link paged
     *
     * @param input
     * @return
     * @throws Exception
     */
    default PageResult<Link> getSiteLinkPaged(GetSiteLinkPagedInput input) throws Exception {
        return null;
    }

    /**
     * update system info
     *
     * @return
     */
    default UpdateSystemInfoOutput updateSystemInfo() throws Exception {
        return null;
    }

    /**
     * get och link
     *
     * @param input
     * @return
     * @throws Exception
     */
    default List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> getOchLinks(
            GetOchLinkInput input) throws Exception {
        return null;
    }

    /**
     * get och links paged
     *
     * @param input
     * @return
     */
    default PagedList getOchLinksPaged(GetOchLinkPagedInput input) throws Exception {
        return null;
    }


    default PageResult<Link> getOchLinksPagedNew(GetOchLinkPagedInput input) throws Exception {
        return null;
    }

    /**
     * get phy node
     *
     * @return
     */
    default List<Node> getPhyNode(GetPhyNodeInput input) throws Exception {
        return null;
    }

    /**
     * get phy node paged
     *
     * @param input
     * @return
     */
    default PagedList getPhyNodePaged(GetPhyNodePagedInput input) throws Exception {
        return null;
    }


    /**
     * @param retrieveTopologyDto
     * @return
     * @throws Exception
     */
    default PageResult<Node> getPhyNodePagedNew(RetrieveTopologyDto retrieveTopologyDto)
            throws Exception {
        return null;
    }


    default List<Node> getSiteNode(RetrieveTopologyDto retrieveTopologyDto) throws Exception {
        return null;
    }

    /**
     * get site node
     *
     * @param input
     * @return
     * @throws Exception
     */
    default List<Node> getSiteNode(GetSiteNodeInput input) throws Exception {
        return null;
    }

    /**
     * get site node paged
     *
     * @param input
     * @return
     */
    default PagedList getSiteNodePaged(GetSiteNodePagedInput input) throws Exception {
        return null;
    }

    /**
     * get site node paged
     *
     * @param input
     * @return
     */
    default PageResult<Node> getSiteNodePagedNew(RetrieveTopologyDto input) throws Exception {
        return null;
    }

    /**
     * get site link by node ip
     *
     * @param input
     * @return
     * @throws Exception
     */
    default GetSiteLinkByNodeIpOutputBuilder getSiteLinkByNodeIp(GetSiteLinkByNodeIpInput input)
            throws Exception {
        return null;
    }


    /**
     * get unstuffed phy node
     *
     * @param siteNodeId
     * @return
     * @throws Exception
     */
    default List<Node> getUnStuffedPhyNode(String siteNodeId) throws Exception {
        return null;
    }

    /**
     * get site topo
     *
     * @return
     */
    default List<Topology> getSiteTopo() throws Exception {
        return null;
    }

    /**
     * get ops connections
     *
     * @return
     */
    default List<OpsConnections> getOpsConnections(GetOpsConnectionsInput input) throws Exception {
        return null;
    }


    /**
     * get equipment
     *
     * @param input
     * @return
     */
    default List<Node> getEquipment(GetEquipmentInput input) {

        return null;
    }

    /**
     * get equipment paged
     *
     * @param input
     * @return
     */
    default PagedList getEquipmentPaged(GetEquipmentPagedInput input) throws Exception {
        return null;
    }


    /**
     * get-phyTp
     *
     * @param input
     * @return
     */
    default List<Node> getPhyTp(GetPhyTpInput input) {
        return null;
    }

    /**
     * get-phyTp=paged
     *
     * @param input
     * @return
     */
    default List<Node> getPhyTpPaged(GetPhyTpPagedInput input) {
        return null;
    }

    /**
     * get-card-type
     *
     * @return
     */
    default List<String> getCardType(GetCardTypeInput input) {
        return null;
    }

    /**
     * get-ot-card-capability
     *
     * @return
     */
    default List<?> getOtCardCapability() {
        return null;
    }

    /**
     * get Frequency map
     *
     * @param input
     * @return
     */
    default FrequencyMap getFrequencyMap(GetFrequencyMapInput input) throws Exception {
        return null;
    }

    default FrequencyMapDto getFrequencyMapDto(GetFrequencyMapInput input) throws Exception {
        return null;
    }

    default FrequencyMapDto getFrequencyMapBySiteLink(GetFrequencyMapSiteLinkInput input)
            throws Exception {
        return null;
    }

    /**
     * get ne unit list
     *
     * @param input
     * @return
     */
//    default List<String> getNeUnitList(GetNeUnitListInput input) {
//        return null;
//    }

    /**
     * get unit list
     *
     * @param input
     * @return
     */
//    default List<UnitInfo> getUnitList(GetUnitListInput input) {
//        return null;
//    }

    /**
     * get wss channel
     *
     * @param input
     * @return
     * @throws Exception
     */
    default GetWssChannelOutput getWssChannel(GetWssChannelInput input) throws Exception {
        return null;
    }

    /**
     * get mux spectrum
     *
     * @param input
     * @return
     * @throws Exception
     */
    default GetMuxSpectrumOutput getMuxSpectrum(GetMuxSpectrumInput input) throws Exception {
        return null;
    }

    /**
     * get mux spectrum with site links
     *
     * @param input
     * @return
     * @throws Exception
     */
    default GetMuxSpectrumWithSiteLinkOutput getMuxSpectrumWithSiteLinks(
            GetMuxSpectrumWithSiteLinkInput input) throws Exception {
        return null;
    }

    /**
     * get tunnel id
     *
     * @return
     */
    default String getTunnelId(GetTunnelIdInput input) throws Exception {
        return null;
    }

    /**
     * get- transceiver by tp
     *
     * @param input
     * @return
     * @throws Exception
     */
    default GetTransceiverByTpOutput getTransceiverByTp(GetTransceiverByTpInput input)
            throws Exception {
        return null;
    }

    default GetScanTpOutput getScanTp(GetScanTpInput input) throws Exception {
        return null;
    }


    /**
     * get scan termination point
     *
     * @param input
     * @return
     * @throws Exception
     */
    default GetScanTpOutput getScanTerminationPoint(GetScanTpInput input) throws Exception {
        return null;
    }

    /**
     * get-friend name
     *
     * @param input
     * @return
     * @throws Exception
     */
    default List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object> getFriendName(
            GetFriendlyNameInput input) throws Exception {
        return null;
    }

    /**
     * get -fiber-affection
     *
     * @return
     */
    default GetFiberAffectionOutput getFiberAffection(GetFiberAffectionInput input)
            throws Exception {
        return null;
    }

    /**
     * get site link ots
     *
     * @param input
     * @return
     */
    default GetSiteLinkOtsOutput getSiteLinkOts(GetSiteLinkOtsInput input) throws Exception {
        return null;
    }

//    /**
//     * get idc id
//     *
//     * @param input
//     * @return
//     */
//    default GetIdcIdOutput getIdcId(GetIdcIdInput input) {
//        return null;
//    }
//
//    /**
//     * get idc info
//     */
//    default GetIdcInfoOutput getIdcInfo(GetIdcInfoInput input) {
//        return null;
//    }

    /**
     * get site link between two sites
     *
     * @param input
     * @return
     * @throws Exception
     */
    default List<LinkInfo> getSiteLinksBetweenTwoSites(GetSiteLinksBetweenTwoSitesInput input)
            throws Exception {
        return null;
    }

    /**
     * get middle site between two sites
     *
     * @param input
     * @return
     */
    default GetMiddleSitesBetweenTwoSitesOutput getMiddleSitesBetweenTwoSites(
            GetMiddleSitesBetweenTwoSitesInput input) throws Exception {
        return null;
    }

    /**
     * get env property
     *
     * @return
     */
    default GetEnvPropertyOutput getEnvProperty() throws Exception {
        return null;
    }


    /**
     * route-display
     *
     * @param input
     * @return
     */
    default List<RouteDisplayInfo> routeDisplay(RouteDisplayInput input) throws Exception {
        return null;
    }

    /**
     * remove resource by order id
     *
     * @param input
     */
    default void removeResourceByOrderId(RemoveResourceByOrderIdInput input) throws Exception {

    }

    /**
     * recycle resource by order id
     *
     * @param input
     * @return
     * @throws Exception
     */

    default RecycleResourceByOrderIdOutput recycleResourceByOrderId(
            RecycleResourceByOrderIdInput input) throws Exception {
        return null;
    }

    /**
     * get resource by order id
     *
     * @param input
     * @return
     * @throws Exception
     */

    default GetResourceByOrderIdOutput getResourceByOrderId(GetResourceByOrderIdInput input)
            throws Exception {
        return null;
    }


    /**
     * get schedule paged
     *
     * @param input
     * @return
     */
    default GetSchedulePagedOutput getSchedulePaged(GetSchedulePagedInput input) throws Exception {
        return null;
    }
//
//    /**
//     * delete schedules
//     *
//     * @param input
//     * @return
//     */
//    default DeleteSchedulesOutput deleteSchedules(DeleteSchedulesInput input) {
//        return null;
//    }

    /**
     * sync equipment
     *
     * @return
     */
    default SyncEquipmentOutputBuilder syncEquipment(SyncEquipmentInput input) {
        return null;
    }

    /**
     * sync-termination-point
     *
     * @param input
     * @return
     */
    default SyncTerminationPointOutput syncTerminationPoint(SyncTerminationPointInput input) {
        return null;
    }

    /**
     * resynchronize ne
     *
     * @param input
     * @return
     */

    default ResynchronizeNeOutput resynchronizeNe(ResynchronizeNeInput input) {
        return null;
    }

    /**
     * update node location
     *
     * @param input
     * @return
     */
    default UpdateNodeLocationOutput updateNodeLocation(UpdateNodeLocationInput input)
            throws Exception {
        return null;
    }

    /**
     * list all phy node
     *
     * @param
     * @return
     */
    default GetSimpleRouteOutput getSimpleRoute(GetSimpleRouteInput input) {
        return null;
    }

    /**
     * get network tunnel site node route info
     *
     * @param input
     * @return
     */
    default GetTunnelSiteRouteOutput getTunnelSiteNodeRoute(GetTunnelSiteRouteInput input) {
        return null;
    }

    /**
     * list all phy node
     *
     * @param retrieveDto
     * @return
     */
    default List<Node> listAllPhyNodes(RetrieveTopologyDto retrieveDto) {
        return null;
    }

    default GetDesignSimpleRouteOutput getDesignThumbnailRoute(GetDesignSimpleRouteInput input) {
        return null;
    }

    default List<TerminationPoint> getRealMpoTpByVisualTp(GetRealMpoPortInput input) {
        return null;
    }

    default List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPoint> getCardPhysicalPort(
            GetCardPortsInput input) {
        return new ArrayList<>();
    }

    default LocateResourcesByAlarmOutput locateResourceByAlarm(LocateResourcesByAlarmInput input) {
        return null;
    }

    default OmsLinkOtsLinkInfoDto getOtsLinkByOMSLink(GetOtsLinksUnderOmsInput input) {
        return null;
    }

    default GetViewLinkByPlaneOutput getViewLinkByPlane(GetViewLinkByPlaneInput input) {
        return null;
    }

    default GetViewLinkByPlaneStartwithOutput getViewLinkByPlaneStartwith(
            GetViewLinkByPlaneStartwithInput input) {
        return null;
    }

    default GetViewLinkGroupbyPlaneOutput getViewLinkGroupedByPlane() {
        return null;
    }

    default ListViewPlaneOutput listAllViewPlane() {
        return null;
    }

    default GetSiteViewTopologyOutput getSiteViewTopology(GetSiteViewTopologyInput input) {
        return null;
    }

    default List<LldpInfos> getBoardLldpInfos(GetBoardLldpInfoInput input) {
        return new ArrayList<>();
    }

    default List<ObjectDetail> getObjectsDetails(GetObjectDetailsInput input) {
        return new ArrayList<>();
    }

    default List<RelatedTunnels> getSiteLinksRelativeTunnels(GetSiteLinkRelatedTunnelsInput input) {
        return new ArrayList();
    }

    default List<RelatedSiteLink> getRelatedSiteLink(GetRelatedSiteLinksInput input) {
        return new ArrayList<>();
    }

    default TunnelBetweenSitePagedInfo getTunnelBetweenSitePaged(
            GetTunnelBetweenSitePagedInput input) {

        return TunnelBetweenSitePagedInfo.builder().build();
    }
}
