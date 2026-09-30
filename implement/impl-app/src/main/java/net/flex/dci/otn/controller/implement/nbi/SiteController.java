/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.nbi;


import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.implement.site.nbi.impl.ApplyNodeOnSiteLinkService;
import net.flex.dci.otn.controller.implement.site.nbi.impl.AseRebuild;
import net.flex.dci.otn.controller.implement.site.nbi.impl.InjectAse;
import net.flex.dci.otn.controller.implement.site.nbi.impl.RemoveNodeOnSiteLinkService;
import net.flex.dci.otn.controller.implement.site.nbi.impl.SiteLinkImplSyncService;
import net.flex.dci.otn.controller.implement.site.nbi.impl.SiteLinkService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateSitelinkSyncInput;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author YYX
 * @version 1.0
 *
 *         provide site-topology.yang api
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class SiteController extends BaseController {

    private final SiteLinkService siteLinkService;

    private final SiteLinkImplSyncService siteLinkImplSyncService;

    private final InjectAse injectAse;

    private final AseRebuild aseRebuild;

    private final ApplyNodeOnSiteLinkService applyNodeOnSiteLinkService;

    private final RemoveNodeOnSiteLinkService removeNodeOnSiteLinkService;

//    @Autowired
//    private JsonUtil jsonUtil;

//    @GetMapping(value = "/restconf/config/test")
//    public void migration() throws Exception {
//        MongoMigrator migration = SpringBeanFinder.getBean(MongoMigrator.class);
//        migration.migrate();
//    }

    @Deprecated
    @PostMapping(value = "/restconf/operations/tunnel:update-sitelink-sync", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateSiteLinkSync(@RequestBody String json) throws CommonException {
        log.info("updateSiteLinkSync starts");
        UpdateSitelinkSyncInput input = formRpcInput(json, UpdateSitelinkSyncInput.class);
        log.info("updateSiteLinkSync {}", input);

//        UpdateSitelinkSync impl = SpringBeanFinder.getBean(UpdateSitelinkSync.class);
//        UpdateSitelinkSyncOutput output = impl.doIt(input);
//
//        String result = formRpcOutput(output);

        UpdateLinkOutput output = new UpdateLinkOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:update-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateLink(@RequestBody String json, HttpServletRequest request)
            throws CommonException {//just update friendly name
//        UpdateLinkInput input = formRpcInput(json, UpdateLinkInput.class);
        log.info("updateLink: {}", json);


        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for update-link");

            UpdateLinkOutput output = new UpdateLinkOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        UpdateLinkOutput output = siteLinkService.updateLink(json, request);

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:apply-node-on-site-link",
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String applyNodeOnSiteLink(@RequestBody String json, HttpServletRequest request)
            throws CommonException {
        ApplyNodeOnSiteLinkInput input = formRpcInput(json, ApplyNodeOnSiteLinkInput.class);
        log.info("applyNodeOnSiteLink {}", input);

        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for apply-node-on-site-link");

            ApplyNodeOnSiteLinkOutput output = new ApplyNodeOnSiteLinkOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .build();
            return formRpcOutput(output);
        }

        // insert-node-in-site-link only changes DB. This RPC performs the device write for
        // the newly inserted NE and fixes A/Z internalLink state on the real devices.
        ApplyNodeOnSiteLinkOutput output = applyNodeOnSiteLinkService.start(input,
                request.getHeader(AuthConstant.USER_TOKEN_HEADER));
        return formRpcOutput(output);
    }

    @PostMapping(value = "/restconf/operations/site-topology:remove-node-in-site-link",
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeNodeInSiteLink(@RequestBody String json, HttpServletRequest request)
            throws CommonException {
        RemoveNodeInSiteLinkInput input = formRpcInput(json, RemoveNodeInSiteLinkInput.class);
        log.info("removeNodeInSiteLink {}", input);

        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for remove-node-in-site-link");

            RemoveNodeInSiteLinkOutput output = new RemoveNodeInSiteLinkOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .build();
            return formRpcOutput(output);
        }

        // Remove must touch devices before DB is changed: delete split A/B internalLinks,
        // create merged A/B internalLinks, then persist the prepared topology rollback.
        RemoveNodeInSiteLinkOutput output = removeNodeOnSiteLinkService.start(input,
                request.getHeader(AuthConstant.USER_TOKEN_HEADER));
        return formRpcOutput(output);
    }


    @PostMapping(value = "/restconf/operations/site-topology:inject-ase", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String injectAse(@RequestBody String json, HttpServletRequest request)
            throws CommonException {
        InjectAseInput input = formRpcInput(json, InjectAseInput.class);
        log.info("injectAse {}", input);


        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for inject-ase");

            InjectAseOutput output = new InjectAseOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        InjectAseOutput output = injectAse.start(input,
                (String) (request.getHeader(AuthConstant.USER_TOKEN_HEADER)));
        String result = formRpcOutput(output);
        return result;
    }


    @PostMapping(value = "/restconf/operations/site-topology:ase-rebuild", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String aseRebuild(@RequestBody String json, HttpServletRequest request)
            throws CommonException {
        AseRebuildInput input = formRpcInput(json, AseRebuildInput.class);
        log.info("ase rebuild {}", input);
        AseRebuildOutput output = aseRebuild.start(input,
                (String) (request.getHeader(AuthConstant.USER_TOKEN_HEADER)));
        String result = formRpcOutput(output);
        return result;
    }

}
