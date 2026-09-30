/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.site.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.AseSiteLinkSupport;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 */
@Slf4j
@Component
public class AseRebuild {

    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private OchLinkDao ochLinkDao;


    //===================================================
    //
    // init env
    //
    //===================================================
    public AseRebuildOutput start(AseRebuildInput input, String who) throws CommonException {
        log.info("start to rebuild ASE on sitelink");

        Link siteLink = checkParam(input);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        LifeCycleSevice lifeService = new LifeCycleSevice();
        lifeService.logStartLinkImpl(siteLink.getLinkId().getValue(),
                TaskInfoMessage.ResourceType.siteLink,
                siteLinkAttr.getFriendlyName(),
                ActionType.aseRebuild,
                who, null);

        AseRebuildImpl implementor = new AseRebuildImpl(siteLink, lifeService);

        //由于耗时, 把这个同步命令改为异步
        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                log.debug("async ase rebuild working on siteLink {}", siteLink.getLinkId().getValue());
                implementor.startSyncAction();
            }
        });

        return new AseRebuildOutputBuilder()
                .setReturnCode(RpcResultType.AcceptAndStartAsync)
                .setReturnMessage("start working")
                .build();
    }

    /**
     * @param input
     * @return which siteLink will be operated
     * @throws CommonException
     */
    private Link checkParam(AseRebuildInput input) throws CommonException {
        if (input.getLinkId() == null || input.getLinkId().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "sitelinkId is mandatory");
        }

        Link siteLink = siteLinkDao.getSiteLinkById(input.getLinkId());
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required siteLink " + input.getLinkId());
        }
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        if (!supportingAseInject(siteLinkAttr)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "not support ASE inject " + input.getLinkId());
        }

        String friendlyName = siteLinkAttr.getFriendlyName();
        if (!siteLinkAttr.getImplementState().equals(ImplementState.Implement)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the siteLink must be implemented " + friendlyName);
        }
        if (siteLinkAttr.getDummyLink() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "this siteLink has not ASE inject yet" + friendlyName);
        }

        return siteLink;
    }

    private boolean supportingAseInject(Site siteLinkAttr) {
        // Rebuild must use the same capability rule as the initial injection RPC.
        return AseSiteLinkSupport.supportsAse(siteLinkAttr);
    }

}
