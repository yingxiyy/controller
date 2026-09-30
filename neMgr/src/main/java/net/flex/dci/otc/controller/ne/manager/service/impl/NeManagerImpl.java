/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.service.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TIMEZONE;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.convert2TelemetryEmlInput;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.convert2TelemetryEmlOutPut;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.getFailObjInfo;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils.resolveNtpVersion;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.ne.manager.cache.RegisterNeCache;
import net.flex.dci.otc.controller.ne.manager.cache.TelemetryConfigCache;
import net.flex.dci.otc.controller.ne.manager.components.PhyNodeManager;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.communicateState.CommunicateStateUpdater;
import net.flex.dci.otc.controller.ne.manager.components.telemetry.TelemetryManager;
import net.flex.dci.otc.controller.ne.manager.components.telemetry.TelemetryManagerImpl.TelemetryInfo;
import net.flex.dci.otc.controller.ne.manager.components.validator.InputValidator;
import net.flex.dci.otc.controller.ne.manager.core.service.NeResourceService;
import net.flex.dci.otc.controller.ne.manager.core.service.NeService;
import net.flex.dci.otc.controller.ne.manager.dto.NeInfo;
import net.flex.dci.otc.controller.ne.manager.dto.OperationResult;
import net.flex.dci.otc.controller.ne.manager.enums.IpAddressType;
import net.flex.dci.otc.controller.ne.manager.enums.NtpVersion;
import net.flex.dci.otc.controller.ne.manager.model.SensorGroup;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.ne.manager.utils.NeInfoUtil;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryConfig;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.CompareNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.CompareNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.MergeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.upload.history.pm.input.RemoteServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ChannelAseRestoreOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.CompareNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeOperationLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SwitchCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ClockMode;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.SystemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Ntp;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.NtpBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.NtpKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Telemetry;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.TelemetryBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.TelemetryKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OPERATIONITEM;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OtsOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateNeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Slf4j
@Component
@RequiredArgsConstructor
public class NeManagerImpl implements NeManager {

//    private final NeDataSynchronizer neDataSynchronizer;


    private final PhyNodeDao phyNodeDao;

    private final AdapterRpc adapterRpc;

    private final EquipmentsDao equipmentsDao;

    private final AdapterBalancer adapterBalancer;

    private final NeService registerService;

    private final NeResourceService neResourceService;


    private final InputValidator inputValidator;

    private final PhyNodeManager phyNodeManager;

    private final TelemetryDao telemetryDao;

    private final TelemetryManager telemetryManager;

    private final Semaphore TELEMETRY_SEMAPHORE = new Semaphore(10);

    private final RegisterNeCache registerNeCache;

    private final TelemetryConfigCache telemetryConfigCache;

    private final CommunicateStateUpdater communicateStateUpdater;

    private final ThreadPoolExecutor neTelemetryExecutor = new ThreadPoolExecutor(
            6, 12, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactoryBuilder().setNameFormat("ne-manager-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    @Override
    public String registerNe(String input) throws CommonException {
        try {
            RegisteNeInput registeNeInput = SerializeUtil.parseRpcInput(input,
                    RegisteNeInput.class);
            String neId = registeNeInput.getNodeId().getValue();
            AsynchronousExecutor.execute(() -> {
                registerNeWithTimeout(neId, null);
            }, AsynchronousExecutor.getRegisterExecutor());
            RegisteNeOutputBuilder builder = new RegisteNeOutputBuilder();
            builder.setReturnCode(RpcResultType.Success);
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        } catch (Exception exception) {
            RegisteNeOutputBuilder builder = new RegisteNeOutputBuilder();
            builder.setReturnCode(RpcResultType.ResourceAccessFail);
            builder.setReturnMessage(exception.getMessage());
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        }

    }

    @Override
    public String createNe(String input) throws CommonException {
        log.info("start to create the ne,the input is :{}", input);
        try {
            CreateNeInput createNeInput = SerializeUtil.parseRpcInput(input, CreateNeInput.class);
            inputValidator.validateCreateNeInput(createNeInput);
            AsynchronousExecutor.execute(() -> {
                Node newNode = phyNodeManager.createNewNode(createNeInput);
                String neFriendlyName = newNode.getAugmentation(Node1.class).getPhysical()
                        .getFriendlyName();
                if (newNode != null) {
                    try {
                        String newNeId = newNode.getNodeId().getValue();
                        log.debug("generate new node id is:{}", newNeId);
                        phyNodeDao.saveConfigPhyNode(newNode);
                        phyNodeManager.mountNeToSite(newNeId);
                        registerService.reRegisterNe(newNeId, neFriendlyName);
                    } catch (Exception e) {
                        log.error("failed to register the ne :{}", e.getMessage(), e);
                        BroadcastMessager.publishKafkaMessage(
                                BroadcastMessage.builder().title(BroadCastConstant.CONNECT_NE)
                                        .message(
                                                String.format(
                                                        "fail to connect ne : %s,the reason is: %s",
                                                        createNeInput.getPhysical()
                                                                .getFriendlyName(), e.getMessage()))
                                        .error(true).build());
                    }
                }
            });
            CreateNeOutputBuilder outputBuilder = new CreateNeOutputBuilder();
            outputBuilder.setReturnCode(RpcResultType.Success);
            return SerializeUtil.serializeRpcOutput2Json(outputBuilder.build());
        } catch (Exception ex) {
            if (ex instanceof CommonException) {
                throw ex;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        ex.getMessage());
            }
        }

    }

    @Override
    public String reRegisterNe(String neId, String friendlyName) throws CommonException {
        log.info("re register the ne ne is {}", neId);
        try {
//            if (!phyNodeDao.nodeExistIp(neId)) {
//                log.debug("current ne:{} haven't set the ip,discard the register method", neId);
//                return "discard";
//            }
//            String friendlyName = phyNodeDao.getFriendlyName(neId);
            log.debug("reRegistering ne :{} id: {}", friendlyName, neId);
            reRegisterNeWithTimeout(neId, friendlyName);
            return "success";
        } catch (Exception exception) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to register ne :" + neId, exception);
        }

    }


    @Override
    public String unregisteredNe(String input) throws CommonException {
        log.debug("start to unregistered ne");
        try {
            UnregisteNeInput unregisteNeInput = SerializeUtil.parseRpcInput(
                    input, UnregisteNeInput.class);
            UnregisteNeOutputBuilder builder = new UnregisteNeOutputBuilder();
            String nodeId = unregisteNeInput.getNodeId().getValue();
            Boolean force = unregisteNeInput.isForce();
            AsynchronousExecutor.execute(() -> {
//                ZkResourceLock lock = new ZkResourceLock();
//                lock.addResource(nodeId);
////                DistributeLock lock = dciLockFactory.newLock(nodeId);
//                try {
//                    lock.getLock(30, TimeUnit.SECONDS);
//                    registerService.unregisterNe(nodeId,
//                            force);
//                } catch (Exception e) {
//                    log.error("failed to unregister ne ,the reason is:{}", e.getMessage(), e);
//                } finally {
//                    lock.unlock();
//                }
                unregisterNeSafely(nodeId);
            }, AsynchronousExecutor.getRegisterExecutor());
            builder.setReturnCode(RpcResultType.Success);
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    @Override
    public void unSuperviseNe(String neId) {
        log.debug("start to unregistered ne the neId is:{}", neId);
        try {

            unregisterNeSafely(neId);
            changeNeUnSuperviseState(neId);
        } catch (Exception ex) {
            log.error("failed to unSupervise ne,the reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    private void changeNeUnSuperviseState(String neId) {
        log.debug("change the ne unSupervise state:{}", neId);
        phyNodeDao.updateConfigPhyNodeSupervisionState(neId,
                SupervisionStatusType.Unmonitored);
//        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                CommunicationStatusType.Broken);
        communicateStateUpdater.broken(neId);
    }

    private void unregisterNeSafely(String neId) {
        log.info("unregister ne safely");

        try {
            boolean force = true;
            registerService.unregisterNe(neId, force);
            log.info("Successfully unregistered NE :{}", neId);
        } catch (Exception e) {
            log.error("Unexpected error during unregistration for NE: {},reason is:{}", neId,
                    e.getMessage(), e);
            handleRegistrationFailure(neId,
                    new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "Unregistration error: " + e.getMessage(), e));
        }
    }


    /**
     * manage ne
     *
     * @return
     * @throws CommonException
     */
    @Override
    public String manageNe() throws CommonException {
        try {
            log.debug("start to manage the ne ");
            _manageNe();
            ManageNeOutputBuilder builder = new ManageNeOutputBuilder();
            builder.setReturnCode(RpcResultType.Success);
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        } catch (CommonException e) {
            ManageNeOutputBuilder builder = new ManageNeOutputBuilder();
            builder.setReturnCode(RpcResultType.ResourceAccessFail);
            builder.setReturnMessage(e.getMessage());
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        }
    }

    public void _manageNe() throws CommonException {

        log.info("finished manage the ne");

    }

    private void doRegisterNe(String neId, String friendlyName) {
        try {
            registerNeWithTimeout(neId, friendlyName);
//            AsynchronousExecutor.execute(() -> {
//                ZkResourceLock lock = new ZkResourceLock();
//                lock.addResource(neId);
////                DistributeLock lock = dciLockFactory.newLock(neId);
//                try {
//                    lock.getLock(1, TimeUnit.MINUTES);
//                    registerService.registerNe(neId);
//                } catch (Exception e) {
//                    log.error("failed to register the ne,:{}", e.getMessage(), e);
//                    BroadcastMessager.publishKafkaMessage(
//                            BroadcastMessage.builder().title(BroadCastConstant.CONNECT_NE).message(
//                                            String.format("fail to connect ne : %s", neId))
//                                    .error(true).build());
//                } finally {
//                    lock.unlock();
//                }
//            });

        } catch (Exception e) {
            log.error("Failed to register ne {}", neId, e);
        }

    }

    private boolean isManagedNe(Node node) {
        if (node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null) {
            Physical phy = node.getAugmentation(Node1.class).getPhysical();
            return phy.getIp() != null && phy.getPort() != null && phy.getLoginName() != null
                    && phy.getLoginPasswd() != null && phy.getAdminState() != null
                    && phy.getAdminState() != AdminStatus.Down;
        }
        return false;
    }

    /**
     * config ne
     *
     * @param input
     * @return
     * @throws CommonException
     */
    public String configNe(String input) throws CommonException {
        log.info("start to config ne the input is :{}", input);
        ConfigNeInput configNeInput = SerializeUtil.parseRpcInput(input, ConfigNeInput.class);
        ConfigNeOutput output = configNe(configNeInput.getNodeId().getValue(),
                configNeInput.getPhysical(),
                configNeInput.getTerminationPoint());

        return SerializeUtil.serializeRpcOutput2Json(output);
    }

    /**
     * support to do rpc for config ne
     *
     * @param neId
     * @param physical
     * @param tps
     * @return
     * @throws CommonException
     */
    @Override
    public ConfigNeOutput configNe(String neId, Physical physical, List<TerminationPoint> tps)
            throws CommonException {
        Adapter adapter = adapterBalancer.getAdapterForNeWithException(neId);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeInput configNeInput = new ConfigNeInputBuilder()
                .setNodeId(new NodeId(neId))
                .setPhysical(physical)
                .setTerminationPoint(tps)
                .build();
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeOutput output = adapterRpc.configNe(
                adapter, configNeInput);
        log.info("finish to config ne the result is :{}", output);
        ConfigNeOutput result = null;
        result = new ConfigNeOutputBuilder().setSuccessObj(output.getSuccessObj())
                .setFailObj(output.getFailObj()).build();

        AsynchronousExecutor.execute(
                () -> {
                    StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(
                            Collections.singletonList(
                                    NeStatus.builder().neId(neId).neStatus(
                                            NeStatusType.CONFIG).build())).build());
//                    neSynchronizeService.handleStateChange(neId, true);
                });
        return result;
    }

    @Override
    public String uploadNe(String input) throws CommonException {
        UploadNeInput uploadNeInput = SerializeUtil.parseRpcInput(input, UploadNeInput.class);
        UploadNeOutputBuilder builder = new UploadNeOutputBuilder();
        uploadNeById(uploadNeInput.getNodeId().getValue());
        builder.setReturnCode(RpcResultType.Success);
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }

    @Override
    public OperationResult mergeData(String input) throws CommonException {
        log.info("Begin to merge ne ,the input is {}", input);
        MergeDataInput mergeDataInput = SerializeUtil.parseRpcInput(
                input, MergeDataInput.class);
        String neId = mergeDataInput.getNodeId().getValue();
        log.info("merge ne data the ne Id is :{}", neId);
        long startTime = System.currentTimeMillis();
        boolean isManaged = adapterBalancer.isManagedNeId(neId);
        if (isManaged) {
            neResourceService.mergeNe(neId);
            log.info("Finish to merge ne {} time taken: {}ms", neId,
                    (System.currentTimeMillis() - startTime));
            return OperationResult.builder().timestamp(startTime).success(true).neId(neId)
                    .message("Merge completed successfully").build();
        } else {
            log.warn("NE {} does not exist or has no IP, skip merge operation", neId);
            return OperationResult.builder()
                    .neId(neId)
                    .timestamp(startTime)
                    .success(false)
                    .message("NE does not exist or has no IP")
                    .build();
        }
    }

    private void uploadNeById(String neId) throws CommonException {
        log.info("Begin to upload ne {}", neId);
//
        boolean isManaged = adapterBalancer.isManagedNeId(neId);
        if (isManaged) {
            String neFriendlyName = phyNodeDao.getFriendlyName(neId);
            AsynchronousExecutor.execute(() -> {
                try {
                    long startTime = System.currentTimeMillis();
                    log.info(" synchronizing the ne :{}", neId);
                    neResourceService.SyncNe(neId, neFriendlyName);
                    log.info("Finish to upload ne {} timeSpned: {}ms", neId,
                            (System.currentTimeMillis() - startTime));
                } catch (Exception ex) {
                    log.error("failed to synchronizing the ne data reason is:{}", ex.getMessage());
                    handleSynchronizedFailure(neId, ex);
                }
            });
        }

    }


    /**
     * get ne Data
     *
     * @param input
     * @return
     */
    @Override
    public String getNeData(String input) throws CommonException {
        log.info("get ne data ,request body is {}", input);
        GetNeDataInput getNeDataInput = SerializeUtil.parseRpcInput(input, GetNeDataInput.class);
        String neId = getNeDataInput.getNodeId().getValue();
        long startTime = System.currentTimeMillis();
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter != null) {

            String result = adapterRpc.getNeData(adapter, getNeDataInput);
            log.info("end to get ne data ,cost :{} s",
                    (System.currentTimeMillis() - startTime) / 1000d);
            return result;
        }
        return null;
    }

    /**
     * execute the rpc remove resource
     *
     * @param input
     * @return
     */
    @Override
    public String removeResource(String input) {
        log.debug("remove resource :{}", input);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceInput removeResourceInput = SerializeUtil.parseRpcInput(
                input,
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceInput.class);
        RemoveResourceOutput removeResourceOutput = neResourceService.removeResource(
                new RemoveResourceInputBuilder()
                        .setNodeId(removeResourceInput.getNodeId())
                        .setPhysical(removeResourceInput.getPhysical())
                        .setTerminationPoint(removeResourceInput.getTerminationPoint())
                        .build());
        RemoveResourceOutputBuilder builder = new RemoveResourceOutputBuilder();
        builder.setSuccessObj(removeResourceOutput.getSuccessObj());
        builder.setFailObj(removeResourceOutput.getFailObj());
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }


    /**
     * graphics ne rpc
     *
     * @param input
     * @return
     */
    @Override
    public String compareNe(String input) {
        log.info("execute the graphics ne rpc command ");
        CompareNeInput compareNeInput = SerializeUtil.parseRpcInput(input, CompareNeInput.class);
        CompareNeOutput output = compareNe(compareNeInput);
        CompareNeOutputBuilder builder = new CompareNeOutputBuilder();
        builder.setMisAlignmentInfo(output.getMisAlignmentInfo());
        builder.setNodeId(compareNeInput.getNodeId());
        return SerializeUtil.serializeRpcOutput2Json(builder.build());

    }

    /**
     * compareNe
     *
     * @param compareNeInput
     * @return
     * @throws CommonException
     */
    private CompareNeOutput compareNe(CompareNeInput compareNeInput) throws CommonException {
        String neId = compareNeInput.getNodeId().getValue();
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter != null) {

            Node node = phyNodeDao.getConfigPhyNodeById(neId);
            List<TerminationPoint> tps = new ArrayList<>();
            if (node.getTerminationPoint() != null) {
                for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint t : node
                        .getTerminationPoint()) {
                    TerminationPointBuilder tBuilder = new TerminationPointBuilder();
                    tBuilder.setPhysical(t.getAugmentation(TerminationPoint1.class).getPhysical());
                    tBuilder.setKey(new TerminationPointKey(t.getTpId()));
                    tBuilder.setTpId(t.getTpId());
                    tps.add(tBuilder.build());
                }
            }
            return adapterRpc
                    .compareNe(adapter, neId, node.getAugmentation(Node1.class).getPhysical(), tps);
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Failed to graphics ne %s because no adapter for it.",
                            neId));
        }
    }

    /**
     * config ne
     *
     * @param input
     * @return
     * @throws CommonException
     */
    @Override
    public String report1524TelemetryData(String input) throws CommonException {
        log.info("start to report 1524 telemetry data :{}", input);
        Report1524TelemetryDataInput report1524TelemetryDataInput = SerializeUtil.parseRpcInput(
                input, Report1524TelemetryDataInput.class);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataInput reportInput = convert2TelemetryEmlInput(
                report1524TelemetryDataInput);
        Adapter adapter = adapterBalancer.getAdapterForNeWithException(
                report1524TelemetryDataInput.getNodeRef());
        Report1524TelemetryDataOutput output = adapterRpc.report1524TeleData(
                adapter, reportInput);
        log.debug("finish to report 1524 telemetry data the result is :{}", output);
        if (!output.getReturnCode().equals(RpcResultType.Success)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    output.getReturnMessage());
        }

        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutput emlOutput = convert2TelemetryEmlOutPut(
                output);
        return SerializeUtil.serializeRpcOutput2Json(emlOutput);
    }

    @Override
    public String neSoftwareOperate(String input) throws CommonException {
        log.debug("ne software operate start");
        NeSoftwareOperateInput neSoftwareOperateInput = SerializeUtil.parseRpcInput(input,
                NeSoftwareOperateInput.class);
        if (neSoftwareOperateInput.getNodeId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne id should not be null");
        }
        String neId = neSoftwareOperateInput.getNodeId().getValue();
        Adapter adapter = adapterBalancer.getAdapterForNeWithException(
                neId);
        NeSoftwareOperateOutput neSoftwareOperateOutput = adapterRpc.neSoftwareOperate(adapter,
                neSoftwareOperateInput);
        log.debug("finish to operate the ne software,the result is :{}", neSoftwareOperateOutput);

        return SerializeUtil.serializeRpcOutput2Json(neSoftwareOperateOutput);
    }

    @Override
    public String neDatabaseOperate(String input) throws CommonException {
        log.debug("ne database operate start");
        NeDatabaseOperateInput rpcInput = SerializeUtil.parseRpcInput(input,
                NeDatabaseOperateInput.class);
        if (rpcInput.getNodeId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne id should not be null");
        }
        String neId = rpcInput.getNodeId().getValue();
        Adapter adapter = adapterBalancer.getAdapterForNeWithException(
                neId);
        NeDatabaseOperateOutput rpcOutput = adapterRpc.neDatabaseOperate(adapter,
                rpcInput);
        log.debug("finish to operate the ne software,the result is :{}", rpcOutput);

        return SerializeUtil.serializeRpcOutput2Json(rpcOutput);
    }

    @Override
    public void assignTelemetry2NeByNeIds(Set<String> needConfigNeIds) {
        log.debug("assignTelemetry configuration to ne:{}", needConfigNeIds);
        if (CollectionUtils.isEmpty(needConfigNeIds)) {
            log.warn("there have no ne need assignTelemetryServer yet");
            return;
        }

        int batchSize = 100;
        List<String> neIdList = new ArrayList<>(needConfigNeIds);
        List<List<String>> batches = partitionList(neIdList, batchSize);

        for (List<String> batch : batches) {
            Map<String, String> batchGetTelemetryByNodeIds = telemetryDao.batchGetTelemetryByNodeIds(
                    batch);
            Set<String> configNeIds = batchGetTelemetryByNodeIds.keySet();
            Set<String> needConfigTelemetryNeIds = CommonUtil.getDifferenceSetByGuava(
                    new HashSet<>(batch), configNeIds);
            List<Node> batchNodes = phyNodeDao.listOperPhyNodeByIds(
                    new ArrayList<>(needConfigTelemetryNeIds));
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Node ne : batchNodes) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        TELEMETRY_SEMAPHORE.acquire();
                        try {
                            assignTelemetry2Ne(ne);
                        } catch (Exception ex) {
                            log.warn("assign telemetry to {} failed,skipped",
                                    ne.getNodeId().getValue(), ex);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        TELEMETRY_SEMAPHORE.release();
                    }
                }, neTelemetryExecutor);
                futures.add(future);
            }
            if (!futures.isEmpty()) {
                try {
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .get(5, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.error("Batch telemetry assignment failed", e);
                }
            }
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            batches.add(list.subList(i, Math.min(size, i + batchSize)));
        }
        return batches;
    }

    @Override
    public String operationLink(String input) {
        log.debug("ne operation link:{}", input);
        NeOperationLinkInput neOperationLinkInput = SerializeUtil.parseRpcInput(input,
                NeOperationLinkInput.class);
        inputValidator.validateOperationLink(neOperationLinkInput);
        String nodeId = neOperationLinkInput.getNodeId().getValue();
        String tpId = neOperationLinkInput.getTpId().getValue();
        OtsOperationType operation = neOperationLinkInput.getOperation();
        Class<? extends OPERATIONITEM> operationItem = neOperationLinkInput.getOperationItem();
        String nodeName = phyNodeDao.getFriendlyName(nodeId);
        Adapter adapter = adapterBalancer.getAdapterForNe(nodeId);
        if (adapter == null) {
            log.error("the ne :{} should be supervised first", nodeId);
            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR, String.format(
                    "the ne %s is not supervised,should be supervised before send command",
                    nodeName));
        }
        NeOperationLinkOutput operationOutPut = adapterRpc.operationLink(adapter, nodeId, tpId,
                operation, operationItem);

        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkOutputBuilder builder = new NeOperationLinkOutputBuilder();
        builder.setReturnCode(operationOutPut.getReturnCode());
        builder.setReturnMessage(operationOutPut.getReturnMessage());
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }

    @Override
    public String switchCUActiveStandby(String input) {
        log.debug("switch cu active to standby,the input is:{}", input);
        SwitchCuActiveStandbyInput activeStandbyInput = SerializeUtil.parseRpcInput(input,
                SwitchCuActiveStandbyInput.class);
        inputValidator.validateSwitchCuActiveStandby(activeStandbyInput);
        String neId = activeStandbyInput.getNodeId().getValue();
        String cuId = activeStandbyInput.getTargetCu();
        Equipments cu = equipmentsDao.getConfigEquipmentByNodeAndEqId(neId, cuId);
        String nodeName = phyNodeDao.getFriendlyName(neId);
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter == null) {
            log.error("the ne:{} should be supervised first before send the command", neId);
            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR, String.format(
                    "the ne %s is not supervised,should be supervised before send command",
                    nodeName));
        }
        String cuName = cu.getFriendlyName();
        SwitchCuActiveStandbyOutput output = adapterRpc.switchCuActiveStandby(adapter, neId,
                cuName);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyOutputBuilder builder = new SwitchCuActiveStandbyOutputBuilder();
        builder.setReturnCode(output.getReturnCode());
        builder.setReturnMessage(output.getReturnMessage());
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }

    @Override
    public String uploadHistoryPm(String input) {
        log.debug("start to upload history pm the input is:{}", input);
        UploadHistoryPmInput uploadHistoryPmInput = SerializeUtil.parseRpcInput(input,
                UploadHistoryPmInput.class);
        inputValidator.validateUploadHistoryPmInput(uploadHistoryPmInput);
        String neId = uploadHistoryPmInput.getNodeId().getValue();
        String neName = phyNodeDao.getFriendlyName(neId);
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter == null) {
            log.error("the ne:{} should be supervised first before send the command", neId);
            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR, String.format(
                    "the ne %s is not supervised,should be supervised before send command",
                    neName));
        }
        BigInteger startTime = uploadHistoryPmInput.getStartTimestamp();
        BigInteger endTime = uploadHistoryPmInput.getEndTimestamp();
        BigInteger interval = uploadHistoryPmInput.getInterval();
        RemoteServer remoteServer = uploadHistoryPmInput.getRemoteServer();
        UploadHistoryPmOutput output = adapterRpc.uploadHistoryPm(adapter, neId,
                startTime.longValue(), endTime.longValue(), interval.longValue(), remoteServer);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmOutputBuilder builder = new UploadHistoryPmOutputBuilder();
        builder.setReturnCode(output.getReturnCode());
        builder.setReturnMessage(output.getReturnMessage());
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }

    @Override
    public String channelAseRestore(String input) {
        log.debug("restore channel ase the input is:{}", input);
        ChannelAseRestoreInput channelAseRestoreInput = SerializeUtil.parseRpcInput(input,
                ChannelAseRestoreInput.class);
        inputValidator.validateChannelAseRestoreInput(channelAseRestoreInput);
        String neId = channelAseRestoreInput.getNodeId().getValue();
        String neName = phyNodeDao.getFriendlyName(neId);
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter == null) {
            log.error("the ne:{} should be supervised first before send the command", neId);
            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR, String.format(
                    "the ne %s is not supervised,should be supervised before send command",
                    neName));
        }
        String crossConnectionId = channelAseRestoreInput.getCrossConnectionId();
        ChannelAseRestoreOutput output = adapterRpc.channelAseRestore(adapter, neId,
                crossConnectionId);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreOutputBuilder builder = new ChannelAseRestoreOutputBuilder();
        builder.setReturnCode(output.getReturnCode());
        builder.setReturnMessage(output.getReturnMessage());
        return SerializeUtil.serializeRpcOutput2Json(builder.build());
    }

    @Override
    public String batchConfigNe(String input) {
        log.info("start to batch config ne the input is :{}", input);
        BatchConfigNeInput configNeInput = SerializeUtil.parseRpcInput(input,
                BatchConfigNeInput.class);

        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutput output = batchConfigNe(
                configNeInput.getNodeId().getValue(),
                configNeInput.getPhysical(),
                configNeInput.getTerminationPoint());

        return SerializeUtil.serializeRpcOutput2Json(output);
    }

    @Override
    public void rebalanceAllTelemetryServers() {
        log.info("rebalance All telemetry servers");
        List<String> monitoringNeIds = phyNodeDao.listAllExistIpAndImplementMonitoredNeIds();
        Map<String, String> assigned = telemetryDao.batchGetTelemetryByNodeIds(monitoringNeIds);
        Set<String> unassigned = monitoringNeIds.stream()
                .filter(id -> !assigned.containsKey(id))
                .collect(Collectors.toSet());

        if (unassigned.isEmpty()) {
            log.info("all NEs already have telemetry assigned, skip");
            return;
        }

        log.info("assign telemetry to {} unassigned ne(s)", unassigned.size());
        assignTelemetry2NeByNeIds(unassigned);
    }

    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutput batchConfigNe(
            String neId, Physical physical,
            List<TerminationPoint> terminationPoint) {
        log.info("batch config ne :{} physical:{} terminationPoint:{}", neId, physical,
                terminationPoint);
        Adapter adapter = adapterBalancer.getAdapterForNeWithException(neId);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeInput configNeInput = new BatchConfigNeInputBuilder()
                .setNodeId(new NodeId(neId))
                .setPhysical(physical)
                .setTerminationPoint(terminationPoint)
                .build();
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeOutput output = adapterRpc.batchConfigNe(
                adapter, configNeInput);
        log.info("finish to config ne the result is :{}", output);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutput result = null;
        result = new BatchConfigNeOutputBuilder().setSuccessObj(output.getSuccessObj())
                .setFailObj(output.getFailObj()).build();

        AsynchronousExecutor.execute(
                () -> {
                    StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(
                            Collections.singletonList(
                                    NeStatus.builder().neId(neId).neStatus(
                                            NeStatusType.CONFIG).build())).build());
//                    neSynchronizeService.handleStateChange(neId, true);
                });
        return result;
    }

    private void assignTelemetry2Ne(Node ne) {
        //clear old telemetry configuration to ne
        removeOldTelemetry(ne);
        TelemetryConfig telemetryConfig = telemetryConfigCache.getTelemetryConfig();
        log.debug("start to assign telemetry configuration to the ne:{}",
                ne.getNodeId().getValue());
        TelemetryInfo telemetryInfo = telemetryManager.getTelemetryConfigurationByNe(ne);
        if (telemetryInfo == null) {
            log.warn("there's no available telemetry configuration to set,do nothing");
            return;
        }
        log.debug("config ne:{} telemetryConfiguration is:{}", ne.getNodeId().getValue(),
                telemetryInfo);
        TelemetryServer telemetryServer = telemetryInfo.getTelemetryServer();
        SensorGroup sensorGroup = telemetryInfo.getSensorGroup();
        String neIp = NeInfoUtil.getIp(ne);
        String ntpIp = telemetryInfo.getNtpIp();
        String timezone = telemetryInfo.getTimezone();
        Telemetry telemetry = new TelemetryBuilder()
                .setIp(telemetryServer.getIp())
                .setKey(new TelemetryKey(telemetryServer.getIp(), sensorGroup.getSensorGroupName()))
                .setSensorGroupName(sensorGroup.getSensorGroupName())
                .setSensorGroupPath(sensorGroup.getSensorGroupPath())
                .setHeartbeatInterval(sensorGroup.getHeartbeatInterval())
                .setSampleInterval(sensorGroup.getSampleInterval())
                .setSuppressRedundant(sensorGroup.getSuppressRedundant())
                .setPort(telemetryServer.getPort())
                .setLocalSourceAddress(neIp) //set telemetry report ip
                .build();
        NtpVersion ntpVersion = resolveNtpVersion(ne, ntpIp);
        NtpBuilder ntpBuilder = new NtpBuilder().setIp(
                        ntpIp)
                .setKey(new NtpKey(ntpIp))
                .setEnable(true)
                .setClockMode(ClockMode.FreeRun);
        if (ntpVersion != null) {
            ntpBuilder.setVersion(ntpVersion.getValue());
        }
        Ntp ntp = ntpBuilder.build();

        PhysicalBuilder phyBuilder = new PhysicalBuilder();
        SystemBuilder sysBuilder = new SystemBuilder();
        List<Telemetry> telemetries = new ArrayList<>();
        telemetries.add(telemetry);
        if (telemetryConfig.isEnable() && !telemetryConfig.getValidServers().isEmpty()) {
            Telemetry northboundTs = buildNorthboundTelemetry(ne, telemetryConfig);
            if (northboundTs != null) {
                telemetries.add(northboundTs);
            }
        }
        sysBuilder.setTelemetry(telemetries);
        sysBuilder.setNtp(Collections.singletonList(ntp));
        //timezone
        List<Property> listPro = new ArrayList<>();
        Property pro = new PropertyBuilder().setName(TIMEZONE)
                .setValue(timezone)
                .build();
        listPro.add(pro);
        Properties properties = new PropertiesBuilder().setProperty(listPro).build();
        sysBuilder.setProperties(properties);
        phyBuilder.setSystem(sysBuilder.build());
        phyBuilder.setIp(neIp);
        log.info(
                "config ne:{} telemetry configuration:{} ntp:{} telemetry server address:{} port:{}",
                ne.getNodeId().getValue(),
                sensorGroup, ntpIp, telemetry.getIp(), telemetry.getPort());
        ConfigNeOutput confOutput = configNe(ne.getNodeId().getValue(),
                phyBuilder.build(), new ArrayList<>());
        if (confOutput.getFailObj() != null
                && confOutput.getFailObj().getObject() != null
                && !confOutput.getFailObj().getObject().isEmpty()) {
            FailObj failObj = confOutput.getFailObj();
            String errInfo = getFailObjInfo(failObj);
            log.error("failed to config the telemetry configuration:{}", errInfo);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errInfo);
        }
        //todo:record the registered neId
        telemetryDao.addRegisteredNeId(telemetryServer.getName().getValue(),
                ne.getNodeId().getValue());

    }

    private void removeOldTelemetry(Node ne) {
        log.info("remove old telemetry or not the ne:{}", ne.getNodeId());
        List<Telemetry> oldTelemetry = getOldTelemetry(ne);
        if (CollectionUtils.isEmpty(oldTelemetry)) {
            log.debug("the ne do not assign the telemetry,discard it and skip");
            return;
        }
        String neId = ne.getNodeId().getValue();
        for (Telemetry telemetry : oldTelemetry) {
            try {
                RemoveResourceInput removeResourceInput = NeManagerUtils.buildRemoveTelemetryResourceInput(
                        telemetry.getSensorGroupName(), telemetry.getIp(),
                        telemetry.getPort().getValue(), neId);
                RemoveResourceOutput output = neResourceService.removeResource(
                        removeResourceInput);
                if (output.getSuccessObj() != null
                        && output.getSuccessObj().getObject() != null) {
                    log.info("success clear the old telemetry for the ne:{} sensorGroupName:{}",
                            neId,
                            telemetry.getSensorGroupName());
                } else {
                    log.warn("failed to remove old telemetry for ne:{}, continue to assign new one",
                            neId);
                }
            } catch (Exception e) {
                log.warn("exception when removing old telemetry for ne:{}, continue", neId, e);
            }
        }
    }

    private List<Telemetry> getOldTelemetry(Node node) {
        log.info("get old telemetry for ne：{}", node.getNodeId());
        Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 == null || node1.getPhysical() == null) {
            return Collections.emptyList();
        }

        Physical physical = node1.getPhysical();
        NodeType nodeType = physical.getNodeType();
        String nodeTypeKeyword = nodeType.name().toUpperCase();

        return Optional.ofNullable(physical.getSystem())
                .map(org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.System::getTelemetry)
                .filter(telemetries -> !CollectionUtils.isEmpty(telemetries))
                .map(telemetries -> telemetries.stream()
                        .filter(telemetry -> telemetry.getSensorGroupName() != null)
                        .filter(telemetry -> telemetry.getSensorGroupName()
                                .contains(nodeTypeKeyword))
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    private Telemetry buildNorthboundTelemetry(Node ne, TelemetryConfig telemetryConfig) {
        log.debug("build north bound telemetry:{} to the ne id:{}", telemetryConfig,
                ne.getNodeId().getValue());
        SensorGroup sensorGroup = telemetryManager.getNorthboundTelemetryConfigurationByNe(
                ne);
        log.info("build northbound telemetry the sensorGroup name is:{}",
                sensorGroup.getSensorGroupName());
        NeInfo neInfo = NeInfoUtil.getNeInfo(ne);
        IpAddressType ipAddressType = NeManagerUtils.getIpAddressType(neInfo.getIp());
        net.flex.dci.otc.mongo.mdoel.telemetry.TelemetryServer telemetryServer =
                ipAddressType == IpAddressType.IPV4 ? telemetryConfig.getIpv4Server()
                        : telemetryConfig.getIpv6Server();
        if (!telemetryServer.isValid() || (!StringUtils.hasText(
                telemetryServer.getServerAddress()) && telemetryServer.getPort() == null)) {
            log.warn("there is no valid northbound telemetry server for the current ne:{}",
                    ne.getNodeId().getValue());
            return null;
        }
        //set telemetry report ip
        return new TelemetryBuilder()
                .setIp(telemetryServer.getServerAddress())
                .setKey(new TelemetryKey(telemetryServer.getServerAddress(),
                        sensorGroup.getSensorGroupName()))
                .setSensorGroupName(sensorGroup.getSensorGroupName())
                .setSensorGroupPath(sensorGroup.getSensorGroupPath())
                .setHeartbeatInterval(sensorGroup.getHeartbeatInterval())
                .setSampleInterval(sensorGroup.getSampleInterval())
                .setSuppressRedundant(sensorGroup.getSuppressRedundant())
                .setPort(new PortNumber(telemetryServer.getPort()))
                .setLocalSourceAddress(neInfo.getIp()) //set telemetry report ip
                .build();
    }


    public void registerNeWithTimeout(String neId, String friendlyName) {
        log.info("register ne with timeout,current neId:{}", neId);

//        if (registerService.isNeRegistered(neId)) {
//            log.info("NE {} already registered, skip.", neId);
//            if (!phyNodeDao.existsOpNode(neId)) {
//                neResourceService.SyncNe(neId, friendlyName);
//            }
//            return;
//        }

        try {
            executeLockAndRegistration(neId, friendlyName);
        } catch (Exception ex) {
            log.error("Critical error during registration for NE: {}", neId, ex);
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.SyncFailed);
            String fn = friendlyName != null ? friendlyName : phyNodeDao.getFriendlyName(neId);
            BroadcastMessager.publishKafkaMessage(
                    BroadcastMessage.builder()
                            .title(BroadCastConstant.REGISTER_NE)
                            .message(String.format("Failed to register NE: %s (%s)", fn, neId))
                            .error(true)
                            .build());
        }

    }

    public void reRegisterNeWithTimeout(String neId, String friendlyName) {
        log.info("reRegister ne with timeout,current neId:{}", neId);

//        if (registerService.isNeRegistered(neId)) {
//            log.info("NE {} already registered, skip.", neId);
//            if (friendlyName == null) {
//                friendlyName = phyNodeDao.getFriendlyName(neId);
//            }
//            if (!phyNodeDao.existsOpNode(neId)) {
//                neResourceService.SyncNe(neId, friendlyName);
//            }
//            return;
//        }

        String finalFriendlyName = friendlyName;
        try {
            executeLockAndReRegistration(neId, finalFriendlyName);
        } catch (Exception e) {
            log.error("Critical error during registration for NE: {}", neId, e);
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.SyncFailed);
            String fn = finalFriendlyName != null ? finalFriendlyName
                    : phyNodeDao.getFriendlyName(neId);
            BroadcastMessager.publishKafkaMessage(
                    BroadcastMessage.builder()
                            .title(BroadCastConstant.REGISTER_NE)
                            .message(String.format("Failed to register NE: %s (%s)", fn, neId))
                            .error(true)
                            .build());
        }


    }

    private void executeLockAndRegistration(String neId, String friendlyName) throws Exception {
        log.debug("execute registration for ne:{}", neId);
//        if (!registerNeCache.tryRegisterLock(neId)) {
//            log.info("NE {} is being processed, skip duplicate register", neId);
//            return;
//        }
        try {
            registerService.registerNe(neId, friendlyName);
        } catch (Exception e) {
            log.error("failed to register the ne,:{}", e.getMessage(), e);
            throw e;
        } finally {
            registerNeCache.unlockRegister(neId);
        }
    }

    private void executeLockAndReRegistration(String neId, String friendlyName) throws Exception {
        log.debug("execute re-registration for ne:{}", neId);
//        if (!registerNeCache.tryRegisterLock(neId)) {
//            log.info("NE {} is being processed, skip duplicate reRegister", neId);
//            return;
//        }
        try {
            log.debug("Proceeding with re-registration for NE:{}", neId);
            registerService.reRegisterNe(neId, friendlyName);
            log.info("Successfully re-registered NE: {}", neId);
        } catch (Exception e) {
            log.error("failed to re-register the ne,:{}", e.getMessage(), e);
            throw e;
        }
    }

    private void sendTimeoutNotification(String neId, String neFriendlyName) {
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().
                        title(BroadCastConstant.REGISTER_NE)
                        .message(
                                String.format("connect to ne timeout : %s (%s)", neFriendlyName,
                                        neId))
                        .error(true)
                        .build());
    }


    private void handleSynchronizedFailure(String neId, Exception e) {
        log.error("Failed to register NE {}: {}", neId, e.getMessage(), e);
        String neFriendlyName = phyNodeDao.getFriendlyName(neId);

        // 发送Kafka通知
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(BroadCastConstant.MANAGE_NE)
                        .message(String.format(
                                "Failed to synchronizing NE: %s (%s),the reason is:%s",
                                neFriendlyName,
                                neId, e.getMessage()))
                        .error(true)
                        .build()
        );

        log.error("Critical error for NE {} reason is:{}, aborting", neId, e.getMessage(), e);

    }

    private void handleRegistrationFailure(String neId, CommonException e) {
        log.error("Failed to unregister NE {}: {}", neId, e.getMessage(), e);
        String neFriendlyName = phyNodeDao.getFriendlyName(neId);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(BroadCastConstant.REGISTER_NE)
                        .message(String.format("Failed to unregister NE: %s (%s)",
                                neFriendlyName,
                                neId))
                        .error(true)
                        .build()
        );
        log.error("Critical error for NE {}, aborting", neId, e);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down NeManagerImpl thread pools...");

        neTelemetryExecutor.shutdown();
        try {
            if (!neTelemetryExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("neTelemetryExecutor did not terminate in 30 seconds, forcing shutdown");
                neTelemetryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for neTelemetryExecutor to terminate", e);
            neTelemetryExecutor.shutdownNow();
        }

        log.info("NeManagerImpl thread pools shutdown complete");
    }

}
