/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.service.impl;


import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.ne.manager.service.AdapterManager;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.serialization.JsonUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.AdapterManagerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdapterManagerImpl implements AdapterManager {

    private final AdapterDao adapterDao;

    private final JsonUtil jsonUtil;

//    @Autowired
//    private NeManagerRpc neManagerRpc;
//
//    @Autowired
//    private PhyNodeDao phyNodeDao;

//    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);

//    @Override
//    public String createAdapter(String input) throws CommonException {
//        try {
//            log.info("start to create adapter,input is {}", input);
//            CreateAdapterInput createAdapterInput = (CreateAdapterInput) jsonUtil
//                    .fromJsonToDataObject(input, true);
//            createAdapter(createAdapterInput);
//            CreateAdapterOutputBuilder createAdapterOutputBuilder = new CreateAdapterOutputBuilder();
//            createAdapterOutputBuilder.setReturnCode(RpcResultType.Success);
//            return jsonUtil.fromDataObjectToJson(createAdapterOutputBuilder.build(), true);
//        } catch (Exception exception) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    exception.getMessage());
//        }
//    }

//    @Override
//    public void createAdapter(CreateAdapterInput input) throws CommonException {
//        log.info("start to create adapter,the output is {}", input);
//        //check the adapter is existed or not, avoid duplicate creation
//        String adapterName = input.getName().getValue();
//        Adapter adp = adapterDao.getAdapterById(adapterName);
//        if (adp != null) {
//            //the adapter has created.
//            return;
//        }
//
//        String ip = input.getIp();
//        String userName = input.getLoginName();
//        String password = input.getLoginPasswd();
//        Integer port = input.getPort().getValue();
//        String adapterApiVersion = input.getApiVersion();
//        String adapterVersion = input.getAdapterVersion();
//        List<String> supportedNeVersions = input.getSupportedNeApiVersion();
//
//        AdapterBuilder adapterBuilder = new AdapterBuilder();
//        adapterBuilder.setIp(new Ipv4Address(ip)).setLoginName(userName).setLoginPasswd(password)
//                .setName(new Uri(adapterName)).setPort(new PortNumber(port))
//                .setAdapterVersion(adapterVersion).setApiVersion(adapterApiVersion)
//                .setSupportedNeApiVersion(supportedNeVersions)
//                .setKey(new AdapterKey(new Uri(adapterName)));
////        boolean isMatch = ValidateUtils.validateSbiVersion(adapterApiVersion);
////        adapterBuilder.setVersionMatch(isMatch);
////        if (!isMatch) {
////            log.info("Adapter {} with api {} is not supported for the adapter manager", adapterName,
////                    adapterApiVersion);
////        }
//        adapterDao.saveAdapter(adapterBuilder.build());

    /// /        scheduler.schedule(new Runnable() { /            @Override /            public void
    /// run() { /                try { / adapterSynchronizer.synchronizeAdapter(adapterName); / }
    /// catch (Exception e) { /                    log.error("Failed to manage ne.", e); /
    ///      } / } /        }, 120, TimeUnit.SECONDS);
//    }

//    @Override
//    public String deleteAdapter(String input) throws CommonException {
//        try {
//            log.debug("start to delete the adapter, {}", input);
//            log.info("start to delete the adapter, {}", input);
//            DeleteAdapterInput deleteAdapterInput = (DeleteAdapterInput) jsonUtil
//                    .fromJsonToDataObject(input, true);
//            deleteAdapter(deleteAdapterInput);
//            DeleteAdapterOutputBuilder deleteAdapterOutputBuilder = new DeleteAdapterOutputBuilder();
//            deleteAdapterOutputBuilder.setReturnCode(RpcResultType.Success);
//            return jsonUtil.fromDataObjectToJson(deleteAdapterOutputBuilder.build(), true);
//        } catch (Exception exception) {
//            log.debug("failed to delete the adapter ,the exception is {}", exception.getMessage(),
//                    exception);
//            DeleteAdapterOutputBuilder deleteAdapterOutputBuilder = new DeleteAdapterOutputBuilder();
//            deleteAdapterOutputBuilder.setReturnCode(RpcResultType.SourceNotFound);
//            deleteAdapterOutputBuilder.setReturnMessage(exception.getMessage());
//            return jsonUtil.fromDataObjectToJson(deleteAdapterOutputBuilder.build(), true);
//        }
//    }
    @Override
    public String getAdapterByCondition(HttpServletRequest request) throws CommonException {
        String uri = request.getRequestURI();
        String identifier = jsonUtil.getIdentifier(uri);
        List<Adapter> adapters = adapterDao.getAdapters();
        AdapterManagerBuilder builder = new AdapterManagerBuilder();
        builder.setAdapter(adapters);
        return jsonUtil.fromDataObjectToJson(identifier, builder.build());
    }

//    public void deleteAdapter(DeleteAdapterInput deleteAdapterInput) throws Exception {
//        String adapterName = deleteAdapterInput.getName();
//        log.debug("delete adapter id is {}", adapterName);
//        Adapter adapter = adapterDao.getAdapterById(adapterName);
//        List<String> neIds = adapter.getNe().stream().map(ne -> ne.getNodeId().getValue()).collect(
//                Collectors.toList());
//        neIds.forEach(neId -> {
//            phyNodeDao.deletePhyNodeOPById(neId);
//        });
//        sendNeLossTrack(neIds);
//        adapterDao.deleteAdapter(adapterName);
//        neManagerRpc.manageNe();
//    }

//    private void sendNeLossTrack(List<String> neIds) {
//        List<NeStatus> neStatuses = neIds.stream().map(neId -> NeStatus.builder().neStatus(
//                NeStatusType.LOSS_TRACK).neId(neId).build()).collect(Collectors.toList());
//        StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(neStatuses).build());
//    }

//    public String getAdapterData(HttpServletRequest request) throws Exception {
//        String uri = request.getRequestURI();
//        String identifier = jsonUtil.getIdentifier(uri);
//        List<Adapter> adapters = adapterDao.getAdapters();
//        AdapterManagerBuilder builder = new AdapterManagerBuilder();
//        builder.setAdapter(adapters);
//        return jsonUtil.fromDataObjectToJson(identifier, builder.build());
//    }
}
