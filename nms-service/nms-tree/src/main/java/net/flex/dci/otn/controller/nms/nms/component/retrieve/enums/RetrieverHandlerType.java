package net.flex.dci.otn.controller.nms.nms.component.retrieve.enums;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.function.BiFunction;
import java.util.function.Function;
import lombok.Getter;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.INMSRetrieveOperations;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.ochlink.OchLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.EquipRefPhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.NodeRefPhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.PhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.RackRefPhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.SiteLinkRefPhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.TpRefPhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.phylink.TunnelRefPhyLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.EquipRefSiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.LinkRefSiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.NodeRefSiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.RackRefSiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.SiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.TpRefSiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink.TunnelRefSiteLinkRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.EquipRefTunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.LinkRefTunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.NodeRefTunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.RackRefTunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.TpRefTunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel.TunnelRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.EquipRefNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.LinkRefNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.PhyNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.RackRefNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.SiteNodeRefNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.TpRefNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.phynode.TunnelRefNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode.LinkRefSiteNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode.SiteNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode.TunnelRefSiteNodeRetrieveHandler;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

/**
 * 2026/4/19
 *
 * @author musa
 * @version 1.0
 **/
public enum RetrieverHandlerType {
    PHY_NODE_ALL(RetrieveType.PHY_NODE, 100, (dto, ctx) ->
            StringUtils.isNoneBlank(dto.getTopologyRef()) &&
                    dto.getTopologyRef().equals(PHY_TOPO_KEY)
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(PhyNodeRetrieveHandler.class)),
    PHY_NODE_BY_SITE_NODE(RetrieveType.PHY_NODE, 200, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && dto.getTopologyRef().equals(SITE_TOPO_KEY)
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(SiteNodeRefNodeRetrieveHandler.class)),
    PHY_NODE_BY_RACK(RetrieveType.PHY_NODE, 300, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isNotBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(RackRefNodeRetrieveHandler.class)),
    PHY_NODE_BY_EQUIPMENT(RetrieveType.PHY_NODE,
            400,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && PHY_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isNotBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(EquipRefNodeRetrieveHandler.class)),
    PHY_NODE_BY_TP(
            RetrieveType.PHY_NODE,
            500,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isNotBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(TpRefNodeRetrieveHandler.class)
    ),
    PHY_NODE_BY_LINK(RetrieveType.PHY_NODE, 600, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isNotBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()), ctx -> ctx.getBean(
            LinkRefNodeRetrieveHandler.class)),
    PHY_NODE_BY_TUNNEL(RetrieveType.PHY_NODE, 700, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isNotBlank(dto.getTunnelRef()), ctx -> ctx.getBean(
            TunnelRefNodeRetrieveHandler.class)
    ),
    PHY_LINK_ALL(RetrieveType.PHY_LINK,
            100,
            (dto, ctx) ->
                    StringUtils.isNoneBlank(dto.getTopologyRef()) &&
                            dto.getTopologyRef().equals(PHY_TOPO_KEY)
                            && StringUtils.isBlank(dto.getLinkRef())
                            && StringUtils.isBlank(dto.getEquipRef())
                            && StringUtils.isBlank(dto.getRackRef())
                            && StringUtils.isBlank(dto.getNodeRef())
                            && StringUtils.isBlank(dto.getTpRef())
                            && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(PhyLinkRetrieveHandler.class)),
    PHY_LINK_BY_NODE(RetrieveType.PHY_LINK, 200,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(NodeRefPhyLinkRetrieveHandler.class)),
    PHY_LINK_BY_RACK(RetrieveType.PHY_LINK,
            300,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isNotBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(RackRefPhyLinkRetrieveHandler.class)),

    PHY_LINK_BY_EQUIP(RetrieveType.PHY_LINK, 400,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && PHY_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isNotBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(EquipRefPhyLinkRetrieveHandler.class)),

    PHY_LINK_BY_TP(RetrieveType.PHY_LINK, 500,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isNotBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(TpRefPhyLinkRetrieveHandler.class)),
    PHY_LINK_BY_SITE_LINK(RetrieveType.PHY_LINK, 600,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isNotBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(SiteLinkRefPhyLinkRetrieveHandler.class)),
    PHY_LINK_BY_TUNNEL(RetrieveType.PHY_LINK, 700, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isNotBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(TunnelRefPhyLinkRetrieveHandler.class)),
    SITE_NODE_ALL(RetrieveType.SITE_NODE, 100, (dto, ctx) ->
            StringUtils.isNoneBlank(dto.getTopologyRef()) &&
                    dto.getTopologyRef().equals(SITE_TOPO_KEY)
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(SiteNodeRetrieveHandler.class)),
    SITE_NODE_BY_LINK(
            RetrieveType.SITE_NODE,
            200,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isNotBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(LinkRefSiteNodeRetrieveHandler.class)
    ),
    SITE_NODE_BY_TUNNEL(RetrieveType.SITE_NODE, 300, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isNotBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(TunnelRefSiteNodeRetrieveHandler.class)),

    SITE_LINK_ALL(RetrieveType.SITE_LINK, 100,
            (dto, ctx) -> SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(SiteLinkRetrieveHandler.class)),
    SITE_LINK_BY_TP(RetrieveType.SITE_LINK, 300, (dto, ctx) ->
            StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isNotBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef())
            ,
            ctx -> ctx.getBean(TpRefSiteLinkRetrieveHandler.class)
    ),
    SITE_LINK_BY_EQUIP(RetrieveType.SITE_LINK, 400,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && PHY_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isNotBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(EquipRefSiteLinkRetrieveHandler.class)),
    SITE_LINK_BY_RACK(RetrieveType.SITE_LINK, 500,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isNotBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(RackRefSiteLinkRetrieveHandler.class)),
    SITE_LINK_BY_NODE(RetrieveType.SITE_LINK,
            200,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(NodeRefSiteLinkRetrieveHandler.class)),
    SITE_LINK_BY_LINK(
            RetrieveType.SITE_LINK,
            600,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isNotBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(LinkRefSiteLinkRetrieveHandler.class)
    ),

    SITE_LINK_BY_TUNNEL(
            RetrieveType.SITE_LINK,
            700,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isNotBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(TunnelRefSiteLinkRetrieveHandler.class)
    ),
    TUNNEL_ALL(RetrieveType.TUNNEL, 100, (dto, ctx) ->
            StringUtils.isNoneBlank(dto.getTopologyRef()) &&
                    dto.getTopologyRef().equals(SITE_TOPO_KEY)
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(TunnelRetrieveHandler.class)),
    TUNNEL_BY_NODE(
            RetrieveType.TUNNEL,
            200,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(NodeRefTunnelRetrieveHandler.class)
    ),

    TUNNEL_BY_RACK(
            RetrieveType.TUNNEL,
            300,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && SITE_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isNotBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(RackRefTunnelRetrieveHandler.class)
    ),

    TUNNEL_BY_EQUIP(
            RetrieveType.TUNNEL,
            400,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && PHY_TOPO_KEY.equals(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isNotBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(EquipRefTunnelRetrieveHandler.class)
    ),

    TUNNEL_BY_TP(
            RetrieveType.TUNNEL,
            500,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isNotBlank(dto.getNodeRef())
                    && StringUtils.isNotBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(TpRefTunnelRetrieveHandler.class)
    ),

    TUNNEL_BY_LINK(
            RetrieveType.TUNNEL,
            600,
            (dto, ctx) -> StringUtils.isNotBlank(dto.getTopologyRef())
                    && StringUtils.isNotBlank(dto.getLinkRef())
                    && StringUtils.isBlank(dto.getEquipRef())
                    && StringUtils.isBlank(dto.getRackRef())
                    && StringUtils.isBlank(dto.getNodeRef())
                    && StringUtils.isBlank(dto.getTpRef())
                    && StringUtils.isBlank(dto.getTunnelRef()),
            ctx -> ctx.getBean(LinkRefTunnelRetrieveHandler.class)
    ),
    OCH_LINK_ALL(
            RetrieveType.OCH_LINK,
            100,
            (dto, ctx) ->
                    StringUtils.isNoneBlank(dto.getTopologyRef()) &&
                            dto.getTopologyRef().equals(OCH_TOPO_KEY)
                            && StringUtils.isBlank(dto.getLinkRef())
                            && StringUtils.isBlank(dto.getEquipRef())
                            && StringUtils.isBlank(dto.getRackRef())
                            && StringUtils.isBlank(dto.getNodeRef())
                            && StringUtils.isBlank(dto.getTpRef())
                            && StringUtils.isBlank(dto.getTunnelRef())
            , ctx -> ctx.getBean(OchLinkRetrieveHandler.class)
    );
    @Getter
    private final RetrieveType retrieveType;

    @Getter
    private final int order;

    private final BiFunction<RetrieveTopologyDto, ApplicationContext, Boolean> matcher;

    private final Function<ApplicationContext, INMSRetrieveOperations> factory;

    RetrieverHandlerType(RetrieveType retrieveType, int order,
            BiFunction<RetrieveTopologyDto, ApplicationContext, Boolean> matcher,
            Function<ApplicationContext, INMSRetrieveOperations> factory) {
        this.retrieveType = retrieveType;
        this.order = order;
        this.matcher = matcher;
        this.factory = factory;
    }

    public boolean match(RetrieveTopologyDto dto, ApplicationContext context) {
        return matcher.apply(dto, context);
    }

    public INMSRetrieveOperations getHandler(ApplicationContext ctx) {
        return factory.apply(ctx);
    }
}
