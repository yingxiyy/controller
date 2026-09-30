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
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.InjectAseInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.InjectAseOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.InjectAseOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @version 1.0
 */
@Slf4j
@Component
public class InjectAse {

    @Autowired
    private Checker checker;
    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private OchLinkDao ochLinkDao;


    //===================================================
    //
    // init env
    //
    //===================================================
    public InjectAseOutput start(InjectAseInput input, String who) throws CommonException {
        log.info("start to inject sitelink");

        List<Link> siteLinkList = new ArrayList<>();


        List<String> inputList = input.getLinkIds();
        List<String> matchedList = checker.filterAseSupportedSiteLinks(inputList);
        if (matchedList.size() != inputList.size()) {
            checker.errorBroadcastMessage("SiteLink adjust",
                    "part of site link does not support ASE, skipped");
        }

        if (matchedList.isEmpty()) {
            InjectAseOutput output = new InjectAseOutputBuilder()
                    .setReturnCode(RpcResultType.InvalidArgument)
                    .setReturnMessage(null)
                    .build();
            return output;
        }

        List<String> notSupportMsgList = new ArrayList<>();
        for (String linkId : input.getLinkIds()) {
            log.info("inject ase sitelink id {}", linkId);

            Link siteLink = siteLinkDao.getSiteLinkById(linkId);
            if (siteLink == null) {
                String errMsg = "cannot find required siteLink " + linkId;
                notSupportMsgList.add(errMsg);
            }
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
            try {
                siteLinkList.add(checkParam(siteLink));
            } catch (CommonException e) {
                String errMsg = String.format("%s:  %s", siteLinkAttr.getFriendlyName(), e.getDetail());
                log.warn(errMsg);
                notSupportMsgList.add(errMsg);
            }
        }

        //由于耗时, 把这个同步命令改为异步
        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        for (Link siteLink : siteLinkList) {
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

            LifeCycleSevice lifeService = new LifeCycleSevice();
            lifeService.logStartLinkImpl(siteLink.getLinkId().getValue(),
                    TaskInfoMessage.ResourceType.siteLink,
                    siteLinkAttr.getFriendlyName(),
                    ActionType.injectAse,
                    who, null);

            InjectAseImpl implementor = new InjectAseImpl(siteLink, lifeService);
            executor.lazyDo(new Runnable() {
                @Override
                public void run() {
                    log.debug("async inject ase working on siteLink {}", siteLink.getLinkId().getValue());
                    implementor.startSyncAction();
                }
            });
        }

        if (!notSupportMsgList.isEmpty()) {
            if (siteLinkList.isEmpty()) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("All selected siteLink CANNOT inject ASE: %s", notSupportMsgList.toString()));
            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("Part of selected siteLink CANNOT inject ASE on: %s", notSupportMsgList.toString()));
            }
        }
        return new InjectAseOutputBuilder()
                .setReturnCode(RpcResultType.AcceptAndStartAsync)
                .setReturnMessage("start working")
                .build();
    }

    /**
     * @param siteLink
     * @return which siteLink will be operated
     * @throws CommonException
     */
    private Link checkParam(Link siteLink) throws CommonException {
        String siteLinkId = siteLink.getLinkId().getValue();
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        if (!supportingAseInject(siteLinkAttr)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "not support ASE inject " + siteLinkAttr.getFriendlyName());
        }

        String friendlyName = siteLinkAttr.getFriendlyName();
        if (!siteLinkAttr.getImplementState().equals(ImplementState.Implement)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the siteLink must be implemented " + friendlyName);
        }
        if (siteLinkAttr.getDummyLink() != null && !siteLinkAttr.getDummyLink().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ASE has injected on this siteLink " + friendlyName);
        }

        //siteLik 上面不能有已经imple 了的业务och
        long number = ochLinkDao.countBySupportingLinkRefAndBusiness_NotAllocate(siteLinkId);
        if (number > 0) {
            printWhichOne(siteLinkId, siteLinkAttr.getFriendlyName());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "the siteLink has included implemented service " + friendlyName);
        }

        long allNumber = ochLinkDao.countBySupportingLinkRefAndNotAllocate(siteLinkId);
        if (allNumber - number > 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Find deprecate ase inject data, please clean them with deImplement siteLink at first " + friendlyName);
        }
        return siteLink;
    }

    private void printWhichOne(String siteLinkId, String friendlyName) {
        List<LinkStateDto> linkState = ochLinkDao.getAllBusinessOchLinkStatesUnderSiteLinkIds(Collections.singletonList(siteLinkId));
        List<String> notInAllocatedOchIdList = linkState.stream()
                .filter(ochState -> ochState.getImplement() != ImplementState.Allocate)
                .map(LinkStateDto::getId)
                .collect(Collectors.toList());

        log.warn("ochLink implement status is NOT ALLOCATE on siteLink {} \n{}",
                friendlyName,
                String.join("\n", notInAllocatedOchIdList));
    }

    private boolean supportingAseInject(Site siteLinkAttr) {
        // The persisted model distinguishes Bone2.0 Flex from commercial C and fixed-grid links.
        return AseSiteLinkSupport.supportsAse(siteLinkAttr);
    }

}
