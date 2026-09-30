package net.flex.dci.otn.controller.nms.nms.component.retrieve;

import java.util.ArrayList;
import java.util.List;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.tunnel.TunnelBetweenSitePageResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * @version 1.0
 * @date 2022/3/7 16:21
 */
public interface INMSRetrieveOperations {

    /**
     * retrieve all ref phy link paged
     *
     * retrieve all phy link
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    default PageResult<Link> retrieveAllLinkPaged(Integer pageNum, Integer pageSize) {
        return new PageResult<>();
    }

    default PageResult<Link> retrieveAllLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        return new PageResult<>();
    }

    /**
     * retrieve all ref tunnel paged
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    default PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
        return new PageResult<>();
    }

    default PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        return new PageResult<>();
    }


    /**
     * retrieve all site link ref from topology  phy node and so on
     *
     * @param pageNum page number
     * @param pageSize page size
     * @return pageResult
     */
    default PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
        return new PageResult<>();
    }


    default PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        return new PageResult<>();
    }

    /**
     * retrieve all ref och link from topology or phy node and so on
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    default PageResult<Link> retrieveAllOchLinkPaged(Integer pageNum, Integer pageSize) {
        return new PageResult<>();
    }

    default PageResult<Link> retrieveAllOchLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        return new PageResult<>();
    }

    /**
     * retrieve all node paged based on topology  node id and so on
     *
     * @param pageNum
     * @param pageSize
     * @return
     */
    default PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
        return new PageResult<>();
    }


    default PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        return new PageResult<>();
    }

    default TunnelBetweenSitePageResult retrieveAllTunnelBetweenTwoSitePaged(String sourceSiteId,
            String destSiteId, RetrieveTopologyDto retrieveTopologyDto) {
        return TunnelBetweenSitePageResult.builder().build();
    }

    default List<Tunnel> retrieveAllTunnel(RetrieveTopologyDto retrieveTopologyDto) {
        return new ArrayList<>();
    }


    default List<Node> retrieveAllNode(RetrieveTopologyDto retrieveTopologyDto) {
        return new ArrayList<>();
    }

}
