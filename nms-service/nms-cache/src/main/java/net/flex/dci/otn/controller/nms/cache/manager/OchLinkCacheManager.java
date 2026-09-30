package net.flex.dci.otn.controller.nms.cache.manager;

import static net.flex.dci.otn.controller.nms.cache.utils.CacheConstants.OS_LINK_PREFIX;
import static net.flex.dci.otn.controller.nms.cache.utils.CacheConstants.SITE_LINK_PREFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otn.controller.nms.cache.model.OchLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/25 10:28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OchLinkCacheManager extends AbstractCacheManager<OchLinkCache> {

    private final OchLinkDao ochLinkDao;

    @Override
    public OchLinkCache getValue(String id) throws ExecutionException, InterruptedException {
        log.debug("start to get och link cache ,och link id is :{}", id);
        String sign = cacheOperation.get(generateSignKey(id));
        OchLinkCache ochLinkCache = cacheOperation.getObject(id, OchLinkCache.class);
        if (ochLinkCache == null) {
            return refreshCache(id);
        } else {
            if (sign == null) {
                refreshCache(id);
            }
            return ochLinkCache;
        }
    }

    @Override
    protected OchLinkCache refreshCache(String id) throws ExecutionException, InterruptedException {
        log.debug("reload och link :{} cache", id);
        Callable<OchLinkCache> callable = () -> {
            OchLinkCache ochLinkCache = null;
            if (RedisLockUtil.tryLock(generateLockKey(id), 30, 30)) {
                log.debug("refresh och link id:{}", id);

                Link ochLink = ochLinkDao.getOchLinkByLinkId(id);
                //physical properties
                Och ochLinkPhysical = ochLink.getAugmentation(
                        Link1.class).getOch();
                String friendlyName = ochLinkPhysical.getFriendlyName();
                Destination destination = ochLink.getDestination();
                Source source = ochLink.getSource();
                OchRefLinks ochRefLinks = OchRefLinks.getRefLinks(ochLink);
                ochLinkCache = OchLinkCache.builder()
                        .ochLinkId(id)
                        .friendName(friendlyName)
                        .planeName(ochLinkPhysical.getPlaneName())
                        .sourceNodeId(source.getSourceNode().getValue())
                        .destNodeId(destination.getDestNode().getValue())
                        .sourceTpId(source.getSourceTp().getValue())
                        .destTpId(destination.getDestTp().getValue())
                        .supportPhyLinkIds(ochRefLinks.getSupportTunnelIds())
                        .supportTunnelIds(ochRefLinks.getSupportTunnelIds())
                        .supportSiteLinkId(ochRefLinks.getSiteLink())
                        .build();
            }
            return ochLinkCache;
        };

        return refreshAndGetCache(id, callable);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class OchRefLinks {

        private List<String> supportTunnelIds;

        private String siteLink;

        private List<String> phyLinks;


        public static OchRefLinks getRefLinks(
                Link ochLink) {
            List<SupportingLink> supportLinks = ochLink.getSupportingLink();
            List<String> phyLinkIds = new ArrayList<>();
            String siteLink = null;
            //support link
            for (SupportingLink supportingLink : supportLinks) {
                String linkId = supportingLink.getLinkRef().getValue();
                if (linkId.startsWith(SITE_LINK_PREFIX)) {
                    siteLink = linkId;
                } else if (linkId.startsWith(OS_LINK_PREFIX)) {
                    phyLinkIds.add(linkId);
                }
            }

            //support tunnel
            List<SupportedTunnel> supportTunnels = ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                    .getSupportedTunnel();
            List<String> supportTunnelIds = supportTunnels.stream()
                    .map(supportedTunnel -> supportedTunnel.getTunnelRef().getValue()).collect(
                            Collectors.toList());
            return OchRefLinks.builder().phyLinks(phyLinkIds).siteLink(siteLink)
                    .supportTunnelIds(supportTunnelIds).build();
        }
    }
}
