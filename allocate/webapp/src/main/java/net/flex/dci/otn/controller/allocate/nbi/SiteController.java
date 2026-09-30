/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.nbi;

import static java.util.Comparator.comparing;

import java.util.Comparator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.impl.SiteLinkFrequencyRepair;
import net.flex.dci.otn.controller.allocate.link.site.*;
import net.flex.dci.otn.controller.allocate.link.site.insertnode.InsertNodeInSiteLinkImpl;
import net.flex.dci.otn.controller.allocate.link.site.insertnode.InsertNodeInSiteLinkResult;
import net.flex.dci.otn.controller.allocate.link.site.removenode.RemoveNodeInSiteLinkImpl;
import net.flex.dci.otn.controller.allocate.nbi.impl.SiteLinkResource;
import net.flex.dci.otn.controller.allocate.network.AdditionalAllocator;
import net.flex.dci.otn.controller.allocate.network.AdditionalCreator;
import net.flex.dci.otn.controller.allocate.network.AllocatedSiteLinkDefaultParamUpdater;
import net.flex.dci.otn.controller.allocate.network.DefinedNetworkCreator;
import net.flex.dci.otn.controller.allocate.network.NetworkCreator;
import net.flex.dci.otn.controller.allocate.network.NetworkTemplateCreator;
import net.flex.dci.otn.controller.allocate.network.SiteNetworkAllocator;
import net.flex.dci.otn.controller.allocate.network.template.TemplateRequest;
import net.flex.dci.otn.controller.allocate.node.site.RoadmNode;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCreator;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeRemover;
import net.flex.dci.otn.controller.allocate.node.site.UpdateSite;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetAllNeResourcesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetAllNeResourcesOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.CreateLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.Roadms;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.insert.node.in.site.link.result.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class SiteController extends AbstractNBIController {

    @Autowired
    private NeDesigner neDesigner;
    @Autowired
    private SiteLinkAllocator siteLinkAllocator;
    @Autowired
    private SiteLinkComputer siteLinkComputer;
    @Autowired
    private SiteNetworkAllocator siteNetworkAllocator;
    @Autowired
    private AdditionalAllocator additionalAllocator;
    @Autowired
    private AdditionalCreator additionalCreator;
    @Autowired
    private NetworkTemplateCreator networkTemplateCreator;
    @Autowired
    private DefinedNetworkCreator definedNetworkCreator;

    @Autowired
    private AllocatedSiteLinkDefaultParamUpdater allocatedSiteLinkDefaultParamUpdater;

    @Autowired
    private UpdateSite updateSite;

    @Autowired
    private SiteLinkFrequencyRepair frequencyRepair;

    @Autowired
    private InsertNodeInSiteLinkImpl insertNodeInSiteLink;

    @Autowired
    private RemoveNodeInSiteLinkImpl removeNodeInSiteLink;

    @Autowired
    private JsonUtil jsonUtil;


    @PostMapping(value = "/restconf/operations/site-topology:create-sites", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createSites(@RequestBody String json, HttpServletRequest request) {
        try {
            log.info("createSites ");

            if (!createHashCodes.add(request.getServletPath(), json)) {
                log.info("duplicate request for createSites");

                CreateSitesOutput output = new CreateSitesOutputBuilder().setReturnCode(
                        RpcResultType.Success).build();
                return formRpcOutput(output);
            }
            log.info("create site start");
            CreateSitesInput input = formRpcInput(json, CreateSitesInput.class);

            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.site,
                    TaskInfoMessage.ActionType.create, json);

            SiteNodeCreator siteNodeCreator = new SiteNodeCreator();
            CreateSitesOutput output = siteNodeCreator.setTaskInfo(taskInfoMessage).doIt(input);

            String result = formRpcOutput(output);
            return result;
        } catch (Exception e) {
            log.error("failed to create site :{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    e.getMessage(), e);
        }
    }

    @PostMapping(value = "/restconf/operations/site-topology:update-site", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateSite(@RequestBody String json, HttpServletRequest request) {
        log.info("updateSite ");

        UpdateSiteInput input = formRpcInput(json, UpdateSiteInput.class);
        //only support change friendlyName

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.site,
                TaskInfoMessage.ActionType.changeFriendlyName, json);

        updateSite.setTaskInfo(taskInfoMessage);
        UpdateSiteOutput output = updateSite.updateSiteByInput(input);
        String result = formRpcOutput(output);
        return result;

    }

    @PostMapping(value = "/restconf/operations/site-topology:remove-sites", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeSites(@RequestBody String json, HttpServletRequest request) {
        log.info("removeSites ");

        RemoveSitesInput input = formRpcInput(json, RemoveSitesInput.class);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.site,
                TaskInfoMessage.ActionType.delete, json);

        SiteNodeRemover siteNodeRemover = new SiteNodeRemover();
        RemoveSitesOutput output = siteNodeRemover.setTaskInfo(taskInfoMessage).doIt(input);

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:compute-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String computeLink(@RequestBody String json) {
        log.info("computeLink ");

        ComputeLinkInput input;
        try {
            input = formRpcInput(json, ComputeLinkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        ComputeLinkOutput output;
        try {
            output = siteLinkComputer.doIt(input);
        } catch (Exception e) {
            log.error("Failed to computeLink by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:allocate-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String allocateLink(@RequestBody String json) {
        log.info("allocateLink ");

        AllocateLinkInput input;
        try {
            input = formRpcInput(json, AllocateLinkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        AllocateLinkOutput output;
        try {
            output = siteLinkAllocator.doIt(input);
        } catch (Exception e) {
            log.error("Failed to allocateNetwork by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

        String result = formRpcOutput(output);
        return result;
    }

    //network related siteLink one by one create in create-network
    @PostMapping(value = "/restconf/operations/site-topology:allocate-network", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String allocateNetwork(@RequestBody String json) {
        log.info("allocateNetwork ");

        AllocateNetworkInput input;
        try {
            input = formRpcInput(json, AllocateNetworkInput.class);
            sortAllocateInput(input);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        AllocateNetworkOutput output;
        try {

            output = siteNetworkAllocator.doIt(input);
        } catch (Exception e) {
            log.error("Failed to allocateNetwork by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

        String result = formRpcOutput(output);
        return result;
    }

    private void sortAllocateInput(AllocateNetworkInput input) {
        input.getCreateLinks().sort(comparing(CreateLinks::getFriendlyName));
        input.getRoadms().sort(comparing(Roadms::getSiteId));
        input.getRoadms().forEach(roadm ->
                roadm.getSiteLinkRelation().sort(Comparator.comparing(SiteLinkRelation::getLinka)
                        .thenComparing(SiteLinkRelation::getLinkz))
        );
    }

    private void sortCreateInput(CreateNetworkInput input) {
        input.getCreateLinks().sort(comparing(CreateLinks::getFriendlyName));
        input.getRoadms().sort(comparing(Roadms::getSiteId));
        input.getRoadms().forEach(roadm ->
                roadm.getSiteLinkRelation().sort(Comparator.comparing(SiteLinkRelation::getLinka)
                        .thenComparing(SiteLinkRelation::getLinkz))
        );
    }

    @PostMapping(value = "/restconf/operations/site-topology:allocate-network-additional", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String allocateAdditional(@RequestBody String json) {
        log.info("allocateAdditional for:{} ", json);

        AllocateNetworkAdditionalInput input;
        try {
            input = formRpcInput(json, AllocateNetworkAdditionalInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        AllocateNetworkAdditionalOutput output;
        try {

            output = additionalAllocator.doIt(input);
        } catch (Exception e) {
            log.error("Failed to allocateAdditional by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:design-network", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String designNetwork(@RequestBody String json, HttpServletRequest request) {
        DesignNetworkInput inputDesign;
        try {
            inputDesign = formRpcInput(json, DesignNetworkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }

        try {
            String planeName = inputDesign.getCreateLinks().get(0).getPlaneName();
            Short grid = inputDesign.getCreateLinks().get(0).getFrequencyGrid();
            String msg = String.format("Design create siteLinks (%s) %s--%s ",
                    inputDesign.getCreateLinks().size(), planeName,
                    grid == 0 ? "Flex" : grid + "GHz");

            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.network,
                    TaskInfoMessage.ActionType.design,
                    json);
            taskInfoMessage.setResourceId(msg + System.currentTimeMillis());
            taskInfoMessage.setResourceName(msg);
            taskInfoMessage.setSuccessfully(false);
            taskInfoMessage.setErrorReason("To be confirmed");

            TaskInfoMessager.sendMessage(taskInfoMessage);
            return formRpcOutput(
                    new DesignNetworkOutputBuilder().setReturnCode(RpcResultType.Success).build());
        } catch (Exception e) {
            log.error("Failed to design network from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }


    @PostMapping(value = "/restconf/operations/site-topology:create-network", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createNetwork(@RequestBody String json, HttpServletRequest request) {
        log.info("createNetwork ");

        if (!createHashCodes.add(request.getServletPath(), json)) {
            log.info("duplicate request for createNetwork");

            CreateNetworkOutput output = new CreateNetworkOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        CreateNetworkInput input;
        try {
            input = formRpcInput(json, CreateNetworkInput.class);
            sortCreateInput(input);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.network,
                TaskInfoMessage.ActionType.create,
                json);

        NetworkCreator networkCreator = new NetworkCreator();
        CreateNetworkOutput output = networkCreator.setTaskInfo(taskInfoMessage).doIt(input);

        String result = formRpcOutput(output);
        return result;

    }

    @PostMapping(value = "/restconf/operations/site-topology:update-allocated-site-link-default-params",
            produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String updateAllocatedSiteLinkDefaultParams() {
        try {
            int updatedCount = allocatedSiteLinkDefaultParamUpdater.update();
            // This maintenance operation has no input model; return its execution
            // result directly instead of passing through YANG JSON conversion.
            return "success, updated-site-link-count=" + updatedCount;
        } catch (Exception e) {
            log.error("Failed to update default parameters for allocate-state site links", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    e.getMessage(), e);
        }
    }

    @PostMapping(value = "/restconf/operations/site-topology:create-network-additional", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createAdditional(@RequestBody String json, HttpServletRequest request) {
        log.info("createAdditional");

        CreateNetworkAdditionalInput input;
        try {
            input = formRpcInput(json, CreateNetworkAdditionalInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.network,
                ActionType.additional,
                json);

        try {
            CreateNetworkAdditionalOutput output = additionalCreator.doIt(input, taskInfoMessage);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to createAdditional by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }


    @Deprecated
    @PostMapping(value = "/restconf/operations/site-topology:create-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createLink(@RequestBody String json, HttpServletRequest request) {
        CreateLinkInput input;
        log.info("createLink {}");

        try {
            input = formRpcInput(json, CreateLinkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.siteLink,
                TaskInfoMessage.ActionType.create,
                json);

        SiteLinkCreator<CreateLinkInput> siteLinkCreator = new SiteLinkCreator();
        CreateLinkOutput output = siteLinkCreator.setTaskInfo(taskInfoMessage).doIt(input);

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:create-link-2", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createLink2(@RequestBody String json, HttpServletRequest request) {
        log.info("createLink2 {}");

        CreateLink2Input input;
        try {
            input = formRpcInput(json, CreateLink2Input.class);

            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.siteLink,
                    TaskInfoMessage.ActionType.create,
                    json);

            SiteLinkCreator<CreateLink2Input> siteLinkCreator = new SiteLinkCreator();
            CreateLinkOutput output = siteLinkCreator.setTaskInfo(taskInfoMessage).doIt(input);

            String result = formRpcOutput(output);
            return result;
        } catch (CommonException ex) {
            log.error("create siteLink error", ex);
            throw ex;
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);
        }
    }

    @PostMapping(value = "/restconf/operations/site-topology:remove-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeLink(@RequestBody String json, HttpServletRequest request) {
        log.info("removeLink ");

        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for remove-link");

            RemoveLinkOutput output = new RemoveLinkOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        RemoveLinkInput input = formRpcInput(json, RemoveLinkInput.class);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.siteLink,
                TaskInfoMessage.ActionType.delete,
                null);

        SiteLinkRemover siteLinkRemover = new SiteLinkRemover();
        RemoveLinkOutput output = new RemoveLinkOutputBuilder()
                .setReturnCode(siteLinkRemover.setTaskInfo(taskInfoMessage).doIt(input))
                .build();

        String result = formRpcOutput(output);
        return result;
    }

//  remove to impl model
//    @PostMapping(value = "/restconf/operations/site-topology:update-link", produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public String updateLink(@RequestBody String json, HttpServletRequest request) {
//        log.info("updateLink ");
//
//        //just update friendly name
//        UpdateLinkInput input = formRpcInput(json, UpdateLinkInput.class);
//
//        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
//                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
//                TaskInfoMessage.ResourceType.siteLink,
//                TaskInfoMessage.ActionType.delete,
//                null);
//
//        SiteLinkFriendlyNameUpdater updateSiteLink = new SiteLinkFriendlyNameUpdater();
//        UpdateLinkOutput output = updateSiteLink.setTaskInfo(taskInfoMessage).updateFriendlyName(input.getLinkId(),
//                input.getFriendlyName());
//
//        String result = formRpcOutput(output);
//        return result;
//    }

    @PostMapping(value = "/restconf/operations/site-topology:get-roadm-info", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getRoadmInfo(@RequestBody String json, HttpServletRequest request) {
        log.info("getRoadmInfo ");

        GetRoadmInfoInput input = formRpcInput(json, GetRoadmInfoInput.class);
        GetRoadmInfoOutput output = new RoadmNode(input.getNodeId(),
                input.getTunnelId()).getLinkInfo();

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:get-connectable-network", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getConnectableNetwork(@RequestBody String json) {
        GetConnectableNetworkInput input;
        try {
            input = formRpcInput(json, GetConnectableNetworkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        GetConnectableNetworkOutput output;
        try {
            output = siteNetworkAllocator.getConnectableNetwork(input);
        } catch (Exception e) {
            log.error("Failed to getConnectableNetwork by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:create-network-template")
    public void createNetworkTemplate(@RequestBody String json, HttpServletResponse response) {
        CreateNetworkInput input;
        try {
            input = formRpcInput(json, CreateNetworkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        try {
            networkTemplateCreator.doIt(input, response);
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            HttpHeaders header = new HttpHeaders();
//            header.setContentType(new MediaType("application", "force-download"));
//            header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=ProductTemplate.xlsx");
//            ByteArrayOutputStream stream =networkTemplateCreator.doIt(input);
//            return new ResponseEntity<>(new ByteArrayResource(stream.toByteArray()),
//                    header, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("Failed to createNetworkTemplate by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

    }

    @RequestMapping(value = "/restconf/operations/site-topology:create-network-template", method = RequestMethod.POST, consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createNetworkByTemplate(@ModelAttribute TemplateRequest templateRequest,
            HttpServletRequest request) {
        log.info("createNetworkByTemplate ");

        CreateNetworkInput input;
        /*try {
            input = formRpcInput(json, CreateNetworkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.network,
                TaskInfoMessage.ActionType.create,
                json);*/

        CreateNetworkOutput output = definedNetworkCreator.doIt(templateRequest);

        String result = formRpcOutput(output);
        return result;
    }


    @PostMapping(value = "/restconf/operations/site-topology:get-all-ne-resources", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getAllNeResources(@RequestBody String json) {
        log.debug("getAllNeResources ");
        GetAllNeResourcesInput input;
        try {
            input = formRpcInput(json, GetAllNeResourcesInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        GetAllNeResourcesOutput output;
        try {
            output = new SiteLinkResource().getNes(input);
        } catch (Exception e) {
            log.error("Failed to getAllNeResources by input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/site-topology:insert-node-in-site-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String insertNodeInSiteLink(@RequestBody String json, HttpServletRequest request) {
        log.info("insertNodeInSiteLink {} ", json);
        InsertNodeInSiteLinkOutput output = new InsertNodeInSiteLinkOutputBuilder().build();

        if (!createHashCodes.add(request.getServletPath(), json)) {
            log.info("duplicate request for insertNodeInSiteLink");
            return formRpcOutput(output);
        }

        InsertNodeInSiteLinkInput input;
        try {
            input = formRpcInput(json, InsertNodeInSiteLinkInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }

        TaskInfoMessage taskInfo = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.siteLink,
                TaskInfoMessage.ActionType.insertNode, json);
        taskInfo.setSuccessfully(false);
        // insert-node-in-site-link 是在指定 OTS phyLink 上插入指定 siteNode，入口必须把新 YANG 字段透传到 manager。
        InsertNodeInSiteLinkResult insertResult = insertNodeInSiteLink.doIt(taskInfo, input.getSiteLinkId(), input.getPhyLinkId(), input.getSiteNodeId(),
                input.getNodeType());
        output = new InsertNodeInSiteLinkOutputBuilder(output)
                .setNewNodeId(insertResult.getNewNodeId())
                .setSiteLinkId(insertResult.getSiteLinkId())
                .setNodeType(input.getNodeType())
                .setBomInfo(new BomInfoBuilder(insertResult.getBomInfo()).build())
                .build();
        taskInfo.setDetail(jsonUtil.fromDataObjectToJson(output,true));
        if (insertResult.isApplyRequired()) {
            taskInfo.setErrorReason("To be confirmed");
        } else {
            taskInfo.setSuccessfully(true);
            taskInfo.setErrorReason("confirmed");
            taskInfo.setEndTime(System.currentTimeMillis());
        }
        TaskInfoMessager.sendMessage(taskInfo);
        return formRpcOutput(output);
    }

//    @PostMapping(value = "/restconf/operations/site-topology:remove-node-in-site-link", produces = "application/json;charset=UTF-8")
//    @ResponseBody
//    public String removeNodeInSiteLink(@RequestBody String json, HttpServletRequest request) {
//        log.info("removeNodeInSiteLink {} ", json);
//        RemoveNodeInSiteLinkOutput output = new RemoveNodeInSiteLinkOutputBuilder()
//                .setReturnCode(RpcResultType.Success)
//                .setReturnMessage("success")
//                .build();
//
//        if (!createHashCodes.add(request.getServletPath(), json)) {
//            log.info("duplicate request for removeNodeInSiteLink");
//            return formRpcOutput(output);
//        }
//
//        RemoveNodeInSiteLinkInput input;
//        try {
//            input = formRpcInput(json, RemoveNodeInSiteLinkInput.class);
//        } catch (Exception e) {
//            log.error("Failed to parse json input:{}", json, e);
//            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);
//        }
//
//        TaskInfoMessage taskInfo = new TaskInfoMessage(
//                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
//                TaskInfoMessage.ResourceType.siteLink,
//                ActionType.removeNode, json);
//
//        // remove-node-in-site-link 是 insert 的逆操作：只恢复 siteLink 资源关系，不产生 BOM。
//        removeNodeInSiteLink.doIt(taskInfo, input.getSiteLinkId(), input.getNodeId());
//        taskInfo.setSuccessfully(true);
//        taskInfo.setEndTime(System.currentTimeMillis());
//        TaskInfoMessager.sendMessage(taskInfo);
//        return formRpcOutput(output);
//    }

    @GetMapping(value = "/sitelink/repairSiteLinkAvailable/{siteLinkId}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String repairSiteLinkAva(@PathVariable String siteLinkId) {
        return frequencyRepair.autoHeal(siteLinkId);
    }

    @GetMapping(value = "/sitelink/repairSiteLinkAvailable/all", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String repairAllSiteLinkAva() {
        return frequencyRepair.autoHealAll();
    }
}
