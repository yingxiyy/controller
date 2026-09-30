package net.flex.dci.otn.controller.nms.cache.manager;

import static net.flex.dci.otn.controller.nms.cache.utils.CacheConstants.OPC_TYPE;
import static net.flex.dci.otn.controller.nms.cache.utils.CacheConstants.SITE_LINK_PREFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otn.controller.nms.cache.model.PhyNodeCache;
import net.flex.dci.otn.controller.nms.cache.model.SiteLinkCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * phyNodeId => collector_id,adapter_id,friendlyName,siteLink,siteNode,yang_version,plane,sitelink,
 *
 * @version 1.0
 * @date 2022/3/20 10:58
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class PhyNodeCacheManager extends AbstractCacheManager<PhyNodeCache> {

    private final PhyNodeDao phyNodeDao;

    private final AdapterDao adapterDao;

    private final TelemetryDao telemetryDao;

    private final SiteNodeDao siteNodeDao;

    private final SiteLinkDao siteLinkDao;


    private final PhyLinkDao phyLinkDao;

    private final OchLinkDao ochLinkDao;


    @Override
    public PhyNodeCache getValue(String id) throws ExecutionException, InterruptedException {
        log.debug("get phy node detail info cache, phyNode id is :{}", id);

        String sign_key = id + SIGN_KEY;

        String sign = cacheOperation.get(sign_key);

        PhyNodeCache phyNodeCache = cacheOperation.getObject(id,
                PhyNodeCache.class);
        if (phyNodeCache == null) {
            return refreshCache(id);
        } else if (sign == null) {
            refreshCache(id);
            return phyNodeCache;
        } else {
            return phyNodeCache;
        }
    }


    @Override
    protected PhyNodeCache refreshCache(String id)
            throws ExecutionException, InterruptedException {
        log.debug("start to get or refresh the phy node cache ,id :{}", id);
        Callable<PhyNodeCache> refreshTask = () -> {
            PhyNodeCache phyNodeCache = null;
            if (RedisLockUtil.tryLock(generateLockKey(id), TimeUnit.SECONDS, 30, 30)) {
                log.debug("start to get ref phyNode :{}", id);
                Adapter adapter = adapterDao.getAdapterByNeId(id);
                TelemetryServer collector = telemetryDao.getTelemetryServerByPhyNodeId(
                        id);
                String refSiteNodeId = PhysicalNodeIdNamingRule.getSiteId(id);
                Node siteNode = siteNodeDao.getSiteNodeById(refSiteNodeId);
                Node phyNode = phyNodeDao.getConfigPhyNodeById(id);
                String phyNodeType = phyNode.getAugmentation(Node1.class).getPhysical()
                        .getNodeType().name();
                String phyNodeFriendlyName = phyNode.getAugmentation(Node1.class)
                        .getPhysical()
                        .getFriendlyName();
                String refSiteName = "--";
                String refSiteId = "--";
                List<SiteLinkCache> refSiteLink = new ArrayList<>();
                if (siteNode != null) {
                    refSiteName = siteNode.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                            .getSite().getFriendlyName();
                    refSiteId = siteNode.getNodeId().getValue();
//                    List<Link> siteLinks = getPhyNodeRefSiteLink(id);

                    refSiteLink = getPhyNodeRefSiteLinks(id, phyNodeType);
                }
                phyNodeCache = PhyNodeCache.builder().phyNodeId(id)
                        .phyNodeName(phyNodeFriendlyName)
                        .adapterName(adapter == null ? "--" : adapter.getName().getValue())
                        .refSiteId(refSiteId).refSiteName(refSiteName)
                        .siteLinkCaches(refSiteLink.size() != 0 ? refSiteLink.get(0) : null)
                        .neType(phyNodeType)
                        .collectorName(
                                collector == null ? "--" : collector.getName().getValue())
                        .build();
                cacheOperation.set(id + SIGN_KEY, 1, EXPIRE_TIME_OUT);
                cacheOperation.set(id, phyNodeCache, 2 * EXPIRE_TIME_OUT);
            }
            return phyNodeCache;
        };

        return refreshAndGetCache(id, refreshTask);
    }

    private List<SiteLinkCache> getPhyNodeRefSiteLinks(String id, String nodeType) {
        log.debug("get phy node ref site link ,node id :{},type is :{}", id, nodeType);
        List<Link> siteLinks = new ArrayList<>();
        if (nodeType.contains(OPC_TYPE)) {
            siteLinks = getPhyNodeRefSiteLink(id);

        } else {
            List<Link> ochLink = ochLinkDao.queryWithNode(id);
            List<String> refSiteLinkIds = getOchRefSiteLink(ochLink);
            siteLinks = siteLinkDao.listAllSiteLinkByIds(refSiteLinkIds);
        }
        return getRefSiteLinks(siteLinks);
    }


    private List<SiteLinkCache> getRefSiteLinks(List<Link> siteLinks) {
        return siteLinks.stream().map(siteLink -> {
            Site siteLinkPhysical = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();
            return SiteLinkCache.builder().siteLinkId(siteLink.getLinkId().getValue())
                    .siteLinkFriendlyName(siteLinkPhysical.getFriendlyName()).plane(
                            siteLinkPhysical.getPlaneName()).build();
        }).collect(Collectors.toList());
    }

    private List<Link> getPhyNodeRefSiteLink(String phyNodeId) {
        List<Link> phyLinks = phyLinkDao.getAllPhyLinksByNodeIds(
                Collections.singletonList(phyNodeId));
        List<String> siteLinkIds = new ArrayList<>();
        for (Link refPhyLink : phyLinks) {
            Link1 link1 = refPhyLink.getAugmentation(Link1.class);
            if (link1.getPhysical().getSupportedLink() != null) {
                List<String> refSiteLinkIds = getSlRefSiteLinkIds(
                        link1.getPhysical().getSupportedLink());
                siteLinkIds.addAll(refSiteLinkIds);
            }
        }
        List<Link> siteLink = siteLinkDao.listAllSiteLinkByIds(siteLinkIds);
        return siteLink;
    }

    private List<String> getSlRefSiteLinkIds(List<SupportedLink> supportedLinks) {
        return supportedLinks.stream()
                .filter(supportedLink -> supportedLink.getTopologyRef().getValue()
                        .equals(SITE_TOPO_KEY))
                .map(siteLink -> siteLink.getLinkRef().getValue()).collect(
                        Collectors.toList());
    }


    /**
     * get och link ref site link jd
     *
     * @param ochLinks
     * @return
     */
    private List<String> getOchRefSiteLink(List<Link> ochLinks) {
        List<String> siteLinkIds = new ArrayList<>();
        for (Link ochLink : ochLinks) {
            List<SupportingLink> supportLinks = ochLink.getSupportingLink();
            List<String> refSiteLinkIds = supportLinks.stream()
                    .filter(supportLink -> supportLink.getLinkRef().getValue()
                            .startsWith(SITE_LINK_PREFIX))
                    .map(supportLink -> supportLink.getLinkRef().getValue()).collect(
                            Collectors.toList());
            siteLinkIds.addAll(refSiteLinkIds);
        }
        return siteLinkIds;
    }
}
