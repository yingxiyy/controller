/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.ProviderBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.springframework.stereotype.Component;

/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalLink extends BaseImpl {

//    private ZkResourceLock locker;

    private final MultipleTransaction mongoTransaction;

    /**
     * support change displayName and provider info
     *
     * @param input
     * @return
     * @throws CommonException
     */
    @Override
    public UpdateLinkOutput updatePhyLink(UpdateLinkInput input) throws CommonException {
        String linkId = input.getLinkId().getValue();
        log.debug("update link start,the link id is:{}", linkId);
        checkParam(input);
//        lockResource(ntLink);
        taskInfoMessage.setResourceId(linkId);
        Link phyLink = phyLinkDao.getPhyLinkById(input.getLinkId().getValue());
        Physical phyLinkAttr = phyLink.getAugmentation(Link1.class).getPhysical();
        String displayName = phyLinkAttr.getFriendlyName();

        try {
//            updatePhysical(input);
            updatePhysical(input.getPhysical(), phyLink);
            if (null != input.getPhysical().getAdminState() || null != input.getPhysical()
                    .getImplementState()) {
                //impl/deimpl 的lifeCycle 记录在impl模块实现
            } else {
                logMessage(BroadCastConstant.UPDATE_PHY_LINK, displayName, BLANK);
            }

        } catch (Exception e) {
            log.error("error happen", e);
            logMessage(BroadCastConstant.UPDATE_PHY_LINK, displayName, e.getMessage());
        }
        return super.updatePhyLink(input);
    }

//    private void lockResource(Link ntLink) throws CommonException {
//        String linkId = ntLink.getLinkId().getValue();
//        String aNode = PhysicalLinkIdNamingRule.getNodeAId(linkId);
//        String zNode = PhysicalLinkIdNamingRule.getNodeZId(linkId);
//        locker.addResource(aNode);
//        locker.addResource(zNode);
//
//        try {
//            locker.getLock();
//        } catch (Exception e) {
//            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE, "the related Node has been used by other, please wait");
//        }
//    }

    private void checkParam(UpdateLinkInput input) throws CommonException {
        log.info("Begin to update link {}", input.getLinkId().getValue());
        if (input.getLinkId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "link id cannot be null");
        }
        if (input.getPhysical() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "link configuration information cannot be null");
        }

        String friendlyNameDisplay = input.getPhysical().getFriendlyNameDisplay();
        if (friendlyNameDisplay != null && !StringUtils.isEmpty(friendlyNameDisplay)) {
            if (friendlyNameDisplay.length() > Constant.LinkfriendlyNameDisplayLength) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "link's friendly-name should be smaller than "
                                + Constant.LinkfriendlyNameDisplayLength + " characters");
            }
        }

        boolean existed = phyLinkDao.isExistedPhyLinkId(input.getLinkId().getValue());
        if (!existed) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find link " + input.getLinkId().getValue());
        }
    }

    private void updatePhysical(UpdateLinkInput input) throws CommonException {
//        Link1 link1 = ntLink.getAugmentation(Link1.class);
//        PhysicalBuilder phyBuilder = new PhysicalBuilder(link1.getPhysical());
//        if (null != phyBuilder.getAdminState() || null != phyBuilder.getImplementState()) {
//            PhysicalLinkImpl impl = new PhysicalLinkImpl(taskInfoMessage.getWho(), ntLink);
//            impl.start();
//        }
//        if (input.getPhysical().getProvider() != null) {
//            phyBuilder.setProvider(input.getPhysical().getProvider());
//        }
//
//        String friendlyNameDisplay = input.getPhysical().getFriendlyNameDisplay();
//        if (friendlyNameDisplay != null && !friendlyNameDisplay.equals("")) {
//            phyBuilder.setFriendlyNameDisplay(friendlyNameDisplay);
//        }
//
//        ChangedObject changedObject = new ChangedObject();
//        changedObject.addChangedPhyLink(new LinkBuilder(ntLink)
//                .addAugmentation(Link1.class,
//                        new Link1Builder(link1).setPhysical(phyBuilder.build()).build())
//                .build());
//        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
//        mongoTransaction.save(changedObject);
    }

    private void updatePhysical(Physical physical, Link phyLink) {
        log.debug("start to update the physical");
        if (null != physical.getAdminState() || null != physical.getImplementState()) {
            Link newLink = new LinkBuilder(phyLink).addAugmentation(Link1.class, new Link1Builder()
                            .setPhysical(
                                    new PhysicalBuilder(phyLink.getAugmentation(Link1.class).getPhysical())
                                            .setImplementState(physical.getImplementState())
                                            .setAdminState(physical.getAdminState())
                                            .build())
                            .build())
                    .build();
            PhysicalLinkImpl impl = new PhysicalLinkImpl(taskInfoMessage.getWho(), newLink);
            impl.start();
        }
        Link1 link1 = phyLink.getAugmentation(Link1.class);
        PhysicalBuilder phyBuilder = new PhysicalBuilder(link1.getPhysical());
        if (physical.getProvider() != null) {
            //todo update physical provider info
            Provider updateProvider = physical.getProvider();
            Provider oldProvider = phyBuilder.getProvider();
            Provider updateNewProvider = buildNewChangeProvider(updateProvider, oldProvider);
            phyBuilder.setProvider(updateNewProvider);
        }

        String friendlyNameDisplay = physical.getFriendlyName();
        if (!StringUtils.isEmpty(friendlyNameDisplay)) {
            phyBuilder.setFriendlyName(friendlyNameDisplay);
        }

        ChangedObject changedObject = new ChangedObject();
        changedObject.addChangedPhyLink(new LinkBuilder(phyLink)
                .addAugmentation(Link1.class,
                        new Link1Builder(link1).setPhysical(phyBuilder.build()).build())
                .build());

        mongoTransaction.save(changedObject);
    }

    /**
     *
     * @param updateProvider
     * @param oldProvider
     * @return
     */
    private Provider buildNewChangeProvider(Provider updateProvider, Provider oldProvider) {
        log.debug("build new change provider");
        ProviderBuilder providerBuilder = new ProviderBuilder(oldProvider);
        if (StringUtils.isNoneEmpty(updateProvider.getVendorName())) {
            providerBuilder.setVendorName(updateProvider.getVendorName());
        }
        if (StringUtils.isNoneEmpty(updateProvider.getVendorInfo())) {
            providerBuilder.setVendorInfo(updateProvider.getVendorInfo());
        }
        if (updateProvider.getAttenuation() != null) {
            providerBuilder.setAttenuation(updateProvider.getAttenuation());
        }
        if (updateProvider.getAttenuationAz() != null) {
            providerBuilder.setAttenuationAz(updateProvider.getAttenuationAz());
        }
        if (updateProvider.getAttenuationZa() != null) {
            providerBuilder.setAttenuationZa(updateProvider.getAttenuationZa());
        }
        if (updateProvider.getContractAttenuationAz() != null) {
            providerBuilder.setContractAttenuationAz(updateProvider.getContractAttenuationAz());
        }
        if (updateProvider.getContractAttenuationZa() != null) {
            providerBuilder.setContractAttenuationZa(updateProvider.getContractAttenuationZa());
        }
        if (updateProvider.getDelay() != null) {
            providerBuilder.setDelay(updateProvider.getDelay());
        }
        if (updateProvider.getDelayAz() != null) {
            providerBuilder.setDelayAz(updateProvider.getDelayAz());
        }
        if (updateProvider.getDelayZa() != null) {
            providerBuilder.setDelayZa(updateProvider.getDelayZa());
        }
        if (updateProvider.getFiberType() != null) {
            providerBuilder.setFiberType(updateProvider.getFiberType());
        }
        if (updateProvider.getDistance() != null) {
            providerBuilder.setDistance(updateProvider.getDistance());
        }
        if (updateProvider.getDistanceAz() != null) {
            providerBuilder.setDistanceAz(updateProvider.getDistanceAz());
        }
        if (updateProvider.getDistanceZa() != null) {
            providerBuilder.setDistanceZa(updateProvider.getDistanceZa());
        }

        if (updateProvider.getBrokenNumber() != null) {
            providerBuilder.setBrokenNumber(updateProvider.getBrokenNumber());
        }
        if (updateProvider.getBrokenRate() != null) {
            providerBuilder.setBrokenRate(updateProvider.getBrokenRate());
        }
        if (updateProvider.getSecurityLevel() != null) {
            providerBuilder.setSecurityLevel(updateProvider.getSecurityLevel());
        }
        if (updateProvider.getCustomer() != null) {
            providerBuilder.setCustomer(updateProvider.getCustomer());
        }
        return providerBuilder.build();
    }
}
