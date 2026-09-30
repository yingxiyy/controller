package net.flex.dci.otn.controller.implement.site.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.ase.AseInjector;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.utils.OpNodeMerger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

@Slf4j
public class InjectAseImpl {
    private Link siteLink;
    private LifeCycleSevice lifeService;

    public InjectAseImpl(Link siteLink, LifeCycleSevice lifeService) {
        this.siteLink = siteLink;
        this.lifeService = lifeService;
    }

    public void startSyncAction() {
        log.debug("startSync inject ASE action on sitelink");

        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        //找出要加锁的资源（rInfo)
        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());

        ZkResourceLock locker = new ZkResourceLock();
        String msg = null;
        try {
            lockResource(locker, rInfo);
            updateSiteLinkImplementState(siteLink, ImplementState.Doimplementing);

            new AseInjector(siteLink.getLinkId().getValue(), lifeService).inject();

            updateSiteLinkImplementState(siteLink, ImplementState.Implement);
        } catch (Exception e) {
            log.error("inject ase on siteLink error", e);
            updateSiteLinkImplementState(siteLink, ImplementState.PartialImplement);
            msg = ExceptionUtils.getRootCauseMessage(e);
        } finally {
            lifeService.logEndLinkImpl(msg);

            log.debug("after save to mongo, start merge to OP");
            OpNodeMerger opMerger = new OpNodeMerger();
            rInfo.getNodeIdList().parallelStream().forEach(opMerger::merge);
            log.debug("merge to OP done");

            locker.unlock();
        }
    }

    private void updateSiteLinkImplementState(Link siteLink, ImplementState targetState) {
        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        siteLinkDao.updateSiteLinkImplementState(siteLink.getLinkId().getValue(), targetState, AdminStatus.Up);
    }

    /**
     * lock siteLink and related node's
     *
     * @param
     * @param locker
     * @throws CommonException
     */
    private void lockResource(ZkResourceLock locker, RouteInfo rInfo) {
        locker.addResource(siteLink.getLinkId().getValue());
        for (String nodeId : rInfo.getNodeIdList()) {
            locker.addResource(nodeId);
        }

        locker.getLock();
        log.info("resource has locked");
    }

}
