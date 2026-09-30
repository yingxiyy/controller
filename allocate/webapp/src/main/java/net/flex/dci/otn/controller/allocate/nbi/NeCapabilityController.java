/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.nbi;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
public class NeCapabilityController extends AbstractNBIController {

    @Autowired
    private NeDesigner neDesigner;

    @PostMapping(value = "/restconf/operations/ne-capability:get-vendor-list", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getVendorList(@RequestBody String json) {
        GetVendorListInput input = formRpcInput(json, GetVendorListInput.class);
        GetVendorListOutput output;
        try {
            output = neDesigner.getVendorList(input);
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/ne-capability:get-supported-frequency", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSupportedFrequency(@RequestBody String json) {
        GetSupportedFrequencyInput input;
        try {
            input = formRpcInput(json, GetSupportedFrequencyInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        GetSupportedFrequencyOutput output;
        try {
            output = neDesigner.getSupportedFrequency(input);
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/ne-capability:get-ot-card-capability", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getOtCardCapability() {

        GetOtCardCapabilityOutput output;
        try {
            output = neDesigner.getOtCardCapability();
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/ne-capability:get-card-capability", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getCardCapability(@RequestBody String json) {

        GetCardCapabilityInput input;
        try {
            input = formRpcInput(json, GetCardCapabilityInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }

        GetCardCapabilityOutput output;
        try {
            output = neDesigner.getCardCapability(input);
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/ne-capability:get-site-link-protection-type", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSiteLinkProtectionType(@RequestBody String json) {
        GetSiteLinkProtectionTypeInput input;
        try {
            input = formRpcInput(json, GetSiteLinkProtectionTypeInput.class);
        } catch (Exception e) {
            log.error("Failed to parse input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }

        GetSiteLinkProtectionTypeOutput output;

        try {
            output = neDesigner.getSiteLinkProtectionType(input);
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/ne-capability:get-och-link-protection-type", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getOchLinkProtectionType(@RequestBody String json) {

        GetOchLinkProtectionTypeInput input;
        try {
            input = formRpcInput(json, GetOchLinkProtectionTypeInput.class);
        } catch (Exception e) {
            log.error("Failed to parse input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }

        GetOchLinkProtectionTypeOutput output;
        try {
            output = neDesigner.getOchLinkProtectionType(input);
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }


    @PostMapping(value =
            "/restconf/operations/ne-capability:get-supported-protection-type", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSupportedProtectionType(HttpServletRequest request) {
        GetSupportedProtectionTypeOutput output;
        try {
            output = new GetSupportedProtectionTypeOutputBuilder().setProtectionType(neDesigner.getSupportedProtectionType()).build();
        } catch (CommonException ce) {
            throw ce;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        String result = formRpcOutput(output);
        return result;
    }

}
