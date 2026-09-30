package net.flex.dci.otn.controller.allocate.test;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

import java.util.List;

@Slf4j
public class TestEnv {

    public void clean() {
        cleanTunnel();
        cleanOchLink();
        cleanSiteLink();
        cleanViewLink();
        cleanPhyLink();
        cleanOchNode();
        cleanSiteNode();
        cleanViewNode();
        cleanPhyNode();
        cleanTopo();
    }

    private void cleanTopo() {
//    TopologyDao dao = SpringBeanFinder.getBean(TopologyDao.class);

    }

    private void cleanPhyNode() {
        long startTime, endTime;
        PhyNodeDao dao = SpringBeanFinder.getBean(PhyNodeDao.class);
        log.debug("start get all phyNode");
        startTime = System.currentTimeMillis();
        List<Node> nodeList = dao.listPhyNodes();
        endTime = System.currentTimeMillis();
        log.debug("get all phyNode done. phyNode size {}, and take {}", nodeList.size(), endTime-startTime);

        log.debug("start remove phyNode one by one");
        startTime = System.currentTimeMillis();
        for (Node node : nodeList) {
            dao.deletePhyNodeById(node.getNodeId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all phyNode has removed, take {} ", endTime-startTime);
    }

    private void cleanViewNode() {
        long startTime, endTime;
        ViewNodeDao dao = SpringBeanFinder.getBean(ViewNodeDao.class);
        log.debug("start get all viewNode");
        startTime = System.currentTimeMillis();
        List<Node> nodeList = dao.listViewNodes();
        endTime = System.currentTimeMillis();
        log.debug("get all viewNode done. viewNode size {}, and take {}", nodeList.size(), endTime-startTime);

        log.debug("start remove viewNode one by one");
        startTime = System.currentTimeMillis();
        for (Node node : nodeList) {
            dao.deleteViewNode(node.getNodeId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all viewNode has removed, take {} ", endTime-startTime);
    }

    private void cleanOchNode() {
        long startTime, endTime;
        OchNodeDao dao = SpringBeanFinder.getBean(OchNodeDao.class);
        log.debug("start get all ochNode");
        startTime = System.currentTimeMillis();
        List<Node> nodeList = dao.listOchNodes();
        endTime = System.currentTimeMillis();
        log.debug("get all ochNode done. ochNode size {}, and take {}", nodeList.size(), endTime-startTime);

        log.debug("start remove ochNode one by one");
        startTime = System.currentTimeMillis();
        for (Node node : nodeList) {
            dao.deleteOchNode(node.getNodeId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all ochNode has removed, take {} ", endTime-startTime);
    }

    private void cleanSiteNode() {
        long startTime, endTime;
        SiteNodeDao dao = SpringBeanFinder.getBean(SiteNodeDao.class);
        log.debug("start get all siteNode");
        startTime = System.currentTimeMillis();
        List<Node> nodeList = dao.listSiteNodes();
        endTime = System.currentTimeMillis();
        log.debug("get all siteNode done. siteNode size {}, and take {}", nodeList.size(), endTime-startTime);

        log.debug("start remove siteNode one by one");
        startTime = System.currentTimeMillis();
        for (Node node : nodeList) {
            dao.deleteSiteNode(node.getNodeId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all siteNode has removed, take {} ", endTime-startTime);
    }

    private void cleanPhyLink() {
        long startTime, endTime;
        PhyLinkDao dao = SpringBeanFinder.getBean(PhyLinkDao.class);
        log.debug("start get all phyLink");
        startTime = System.currentTimeMillis();
        List<Link> linkList = dao.listPhyLinks();
        endTime = System.currentTimeMillis();
        log.debug("get all phyLink done. phyLink size {}, and take {}", linkList.size(), endTime-startTime);

        log.debug("start remove phyLink one by one");
        startTime = System.currentTimeMillis();
        for (Link link : linkList) {
            dao.deletePhyLink(link.getLinkId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all phyLink has removed, take {} ", endTime-startTime);
    }

    private void cleanViewLink() {
        long startTime, endTime;
        ViewLinkDao dao = SpringBeanFinder.getBean(ViewLinkDao.class);
        log.debug("start get all viewLink");
        startTime = System.currentTimeMillis();
        List<Link> linkList = dao.listViewLinks();
        endTime = System.currentTimeMillis();
        log.debug("get all viewLink done. viewLink size {}, and take {}", linkList.size(), endTime-startTime);

        log.debug("start remove viewLink one by one");
        startTime = System.currentTimeMillis();
        for (Link link : linkList) {
            dao.deleteViewLink(link.getLinkId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all viewLink has removed, take {} ", endTime-startTime);
    }

    private void cleanSiteLink() {
        long startTime, endTime;
        SiteLinkDao dao = SpringBeanFinder.getBean(SiteLinkDao.class);
        log.debug("start get all siteLink");
        startTime = System.currentTimeMillis();
        List<Link> linkList = dao.getSiteLinks();
        endTime = System.currentTimeMillis();
        log.debug("get all siteLink done. siteLink size {}, and take {}", linkList.size(), endTime-startTime);

        log.debug("start remove siteLink one by one");
        startTime = System.currentTimeMillis();
        for (Link link : linkList) {
            dao.deleteSiteLinkById(link.getLinkId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all siteLink has removed, take {} ", endTime-startTime);
    }

    private void cleanOchLink() {
        long startTime, endTime;
        OchLinkDao dao = SpringBeanFinder.getBean(OchLinkDao.class);
        log.debug("start get all ochLink");
        startTime = System.currentTimeMillis();
        List<Link> linkList = dao.listOchLinks();
        endTime = System.currentTimeMillis();
        log.debug("get all ochLink done. ochLink size {}, and take {}", linkList.size(), endTime-startTime);

        log.debug("start remove ochLink one by one");
        startTime = System.currentTimeMillis();
        for (Link link : linkList) {
            dao.deleteOchLink(link.getLinkId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all ochLinks has removed, take {} ", endTime-startTime);
    }

    private void cleanTunnel() {
        long startTime, endTime;
        TunnelDao dao = SpringBeanFinder.getBean(TunnelDao.class);
        log.debug("start get all tunnel");
        startTime = System.currentTimeMillis();
        List<Tunnel> tunnelList = dao.listTunnels();
        endTime = System.currentTimeMillis();
        log.debug("get all tunnel done. tunnel size {}, and take {}", tunnelList.size(), endTime-startTime);

        log.debug("start remove tunnel one by one");
        startTime = System.currentTimeMillis();
        for (Tunnel tunnel : tunnelList) {
            dao.deleteTunnelByTunnelId(tunnel.getTunnelId().getValue());
        }
        endTime = System.currentTimeMillis();
        log.debug("all tunnels has removed, take {} ", endTime-startTime);
    }
}
