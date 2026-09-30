package net.flex.dci.otn.controller.allocate.network;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.common.Utils;
import net.flex.dci.otn.controller.allocate.network.bytedance.ase.ByteDanceSpec;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * Reapplies the ByteDance default-parameter calculation used at the end of
 * network creation to every SiteLink that is still in allocate state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AllocatedSiteLinkDefaultParamUpdater {

    private final SiteLinkDao siteLinkDao;
    private final AllocatorConfig allocatorConfig;

    public int update() {
        // Keep this maintenance endpoint aligned with SummaryNetwork for both
        // the legacy chassis and the Bone2.0 chassis model.
        if (!isByteDanceFamily(allocatorConfig.getYangModel())) {
            log.info("Skip default-parameter update because yang-model is {}",
                    allocatorConfig.getYangModel());
            return 0;
        }

        List<String> candidateIds = findAllocatedSiteLinkIds();
        if (candidateIds.isEmpty()) {
            log.info("No site link with implement-state=allocate needs default-parameter update");
            return 0;
        }

        ZkResourceLock locker = new ZkResourceLock();
        candidateIds.forEach(locker::addResource);
        try {
            locker.getLock();
            return updateLockedSiteLinks(candidateIds);
        } finally {
            locker.unlock();
        }
    }

    private int updateLockedSiteLinks(List<String> candidateIds) {
        ChangedObject changedObject = new ChangedObject();
        int updatedCount = 0;

        for (String siteLinkId : candidateIds) {
            // Re-read after acquiring the lock because implement-state may have
            // changed while this request was waiting for another operation.
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            if (!isAllocated(siteLink)) {
                log.info("Skip site link {} because it is no longer in allocate state",
                        siteLinkId);
                continue;
            }

            log.info("Update ByteDance default parameters for site link {}", siteLinkId);
            changedObject.addChangedSiteLink(siteLink);
            new ByteDanceSpec(siteLinkId, changedObject).start();
            updatedCount++;
        }

        // Preserve SummaryNetwork's save boundary: no calculated change is
        // persisted until every selected SiteLink has completed successfully.
        if (updatedCount > 0) {
            Utils.store2DB(changedObject);
        }
        log.info("Updated default parameters for {} allocate-state site links", updatedCount);
        return updatedCount;
    }

    private boolean isByteDanceFamily(NeYangModel yangModel) {
        return NeYangModel.ByteDance.equals(yangModel) || NeYangModel.Chassis20.equals(yangModel);
    }

    private List<String> findAllocatedSiteLinkIds() {
        return siteLinkDao.getSiteLinks().stream()
                .filter(Objects::nonNull)
                .filter(this::isAllocated)
                .map(link -> link.getLinkId().getValue())
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }

    private boolean isAllocated(Link siteLink) {
        if (siteLink == null) {
            return false;
        }
        Link1 augmentation = siteLink.getAugmentation(Link1.class);
        Site site = augmentation == null ? null : augmentation.getSite();
        return site != null && ImplementState.Allocate.equals(site.getImplementState());
    }
}
