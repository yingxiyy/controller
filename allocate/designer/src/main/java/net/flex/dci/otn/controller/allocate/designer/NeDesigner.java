/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.ReallocateDataModel;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.WssNetworkInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.WssNetworkInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchOutput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchOutput;
import net.flex.dci.otn.controller.allocate.designer.reallocate.ReallocateRepo;
import net.flex.dci.otn.controller.allocate.designer.site.NetworkRepo;
import net.flex.dci.otn.controller.allocate.designer.site.SiteRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelRepo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;


/**
 * Note: 当前设计原则是： 整个NeDesigner不做任何数据库写操作。
 */
@Service
@Slf4j
public class NeDesigner {

    @Autowired
    private NEInfoConfig neInfoConfig;

    @Autowired
    private SiteRepo siteRepo;

    @Autowired
    private TunnelRepo tunnelRepo;

    @Autowired
    private ReallocateRepo reallocateRepo;

    @Autowired
    private NetworkRepo networkRepo;

    @Autowired
    private JsonOutputer jsonOutputer;

    public RouteInfo allocateSite(final SiteInput input) throws NeDesignerException {
        try {
            RouteInfo result = siteRepo.allocate(input);
            log.trace("Allocate site for input:\n{},\nOutput:{}", input, jsonOutputer.formatRouteInfo(result));
            return result;
        } catch (Exception e) {
            log.error("Failed to allocate site for :{}", input, e);
            throw new NeDesignerException("Failed to allocate site for :" + input, e);
        }
    }

    public WssNetworkInfo allocateWssConnection(WssNetworkInput input) throws NeDesignerException {
        try {
            WssNetworkInfo result = networkRepo.allocateWssConnection(input);
            return result;
        } catch (Exception e) {
            log.error("Failed to allocate WSS connection for: {}", input, e);
            throw new NeDesignerException("Failed to allocate WSS connection", e);
        }
    }

    public RouteInfo allocateTunnel(final TunnelInput input) throws NeDesignerException {

        try {
            RouteInfo result = tunnelRepo.allocate(input);
            log.trace("Allocate tunnel for input:\n{},\nOutput:{}", input, jsonOutputer.formatRouteInfo(result));
            return result;
        } catch (Exception e) {
            log.error("Failed to allocate tunnel for :{}", input, e);
            throw new NeDesignerException("Failed to allocate tunnel for :" + input, e);

        }
    }

    public GetVendorListOutput getVendorList(GetVendorListInput input) throws NeDesignerException {
        try {
            return neInfoConfig.getVendorList(input);
        } catch (Exception e) {
            log.error("Failed to get vendor list for input:{}", input, e);
            throw new NeDesignerException("Failed to get vendor list with input: " + input, e);
        }

    }

    public GetSupportedFrequencyOutput getSupportedFrequency(GetSupportedFrequencyInput input) throws NeDesignerException {
        try {
            return neInfoConfig.getSupportedFrequency(input);
        } catch (Exception e) {
            log.error("Failed to get supported frequency for input:{}", input, e);
            throw new NeDesignerException("Failed to get supported frequency for input: " + input, e);
        }

    }

    public GetOtCardCapabilityOutput getOtCardCapability() throws NeDesignerException {
        try {
            return neInfoConfig.getOtCardCapability();
        } catch (Exception e) {
            log.error("Failed to get get OT card capability", e);
            throw new NeDesignerException("Failed to get get OT card capability", e);
        }

    }


    public RouteInfo reallocateSite(RouteInfo routeInfo, ReallocateDataModel reallocateDataModel) throws NeDesignerException {
        try {
            RouteInfo result = reallocateRepo.reallocateSite(routeInfo, reallocateDataModel);
            log.trace("Allocate site for input:\n{}\n{}\nOutput:{}", jsonOutputer.formatRouteInfo(result), jsonOutputer.formatReallocateEquipments(reallocateDataModel),
                    jsonOutputer.formatRouteInfo(result));
            return result;
        } catch (Exception e) {
            log.error("Failed to reallocate site for :{}\n{}", jsonOutputer.formatRouteInfo(routeInfo), jsonOutputer.formatReallocateEquipments(reallocateDataModel), e);
            throw new NeDesignerException("Failed to reallocate site. ", e);
        }
    }

    public TunnelReuseOchOutput allocateTunnelReuseOch(TunnelReuseOchInput input) throws NeDesignerException {

        try {
            TunnelReuseOchOutput result = tunnelRepo.allocateReuseOch(input);
//            log.trace("Allocate tunnel for input:\n{},\nOutput:{}", input, jsonOutputer.formatRouteInfo(result));
            return result;
        } catch (Exception e) {
            log.error("Failed to allocate tunnel for :{}", input, e);
            throw new NeDesignerException("Failed to allocate tunnel for :" + input, e);

        }
    }

    public TunnelNewOchOutput allocateTunnelNewOch(TunnelNewOchInput input) throws NeDesignerException {
        try {
            TunnelNewOchOutput result = tunnelRepo.allocateNewOch(input);
//            log.trace("Allocate tunnel for input:\n{},\nOutput:{}", input, jsonOutputer.formatRouteInfo(result));
            return result;
        } catch (Exception e) {
            log.error("Failed to allocate tunnel for :{}", input, e);
            throw new NeDesignerException("Failed to allocate tunnel for :" + input, e);

        }
    }

    public GetSiteLinkProtectionTypeOutput getSiteLinkProtectionType(GetSiteLinkProtectionTypeInput input) throws NeDesignerException {
        try {
            return neInfoConfig.getSiteLinkProtectionType(input);
        } catch (Exception e) {
            log.error("Failed to get siteLink protection type for input:{}", input, e);
            throw new NeDesignerException("Failed to get siteLink protection type for input: " + input, e);
        }
    }

    public GetOchLinkProtectionTypeOutput getOchLinkProtectionType(GetOchLinkProtectionTypeInput input) throws NeDesignerException {
        try {
            return neInfoConfig.getOchLinkProtectionType(input);
        } catch (Exception e) {
            log.error("Failed to get och protection type for input:{}", input, e);
            throw new NeDesignerException("Failed to get och protection type for input: " + input, e);
        }
    }

    public NeInfo getNeInfo(String vendorName, String vendorType, String nodeType) throws NeDesignerException {
        return neInfoConfig.getNeInfo(vendorName, vendorType, nodeType);
    }

    public GetCardCapabilityOutput getCardCapability(GetCardCapabilityInput input) throws NeDesignerException {
        try {
            return neInfoConfig.getCardCapability(input);
        } catch (Exception e) {
            log.error("Failed to get get card capability by input:{}", input, e);
            throw new NeDesignerException("Failed to get get card capability", e);
        }
    }

    public List<Class<? extends ProtectionType>> getSupportedProtectionType() {
        return Arrays.asList(ProtectionUnprotected.class, ProtectionBidir1To1.class, ProtectionBidir1To2.class);
    }
}
