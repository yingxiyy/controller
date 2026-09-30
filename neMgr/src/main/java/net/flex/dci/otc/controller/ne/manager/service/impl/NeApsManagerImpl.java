package net.flex.dci.otc.controller.ne.manager.service.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.CLEAR_SUCCESS;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.validator.InputValidator;
import net.flex.dci.otc.controller.ne.manager.service.NeApsManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.clear.ne.aps._switch.logs.input.TargetDevice;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.clear.ne.aps._switch.logs.output.ResultItem;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.clear.ne.aps._switch.logs.output.ResultItem.Status;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.clear.ne.aps._switch.logs.output.ResultItemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ClearApsSwitchLogOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/8/2025 2:50 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeApsManagerImpl implements NeApsManager {

    private final AdapterRpc adapterRpc;

    private final AdapterBalancer adapterBalancer;

    private final InputValidator inputValidator;

    private static final Semaphore APS_SEMAPHORE = new Semaphore(10);


    @Override
    public String clearNeApsSwitchLogs(String input) throws CommonException {
        log.info("clear ne aps switch logs input is:{}", input);
        try {
            ClearNeApsSwitchLogsInput clearNeApsSwitchLogsInput = SerializeUtil.parseRpcInput(input,
                    ClearNeApsSwitchLogsInput.class);
            inputValidator.validateClearNeApsSwitchLogsInput(clearNeApsSwitchLogsInput);
            List<String> neIds = clearNeApsSwitchLogsInput.getTargetDevice().stream().map(
                    TargetDevice::getDeviceId).collect(Collectors.toList());
            log.debug("parallel clear ne aps switch logs the neIds :{}", neIds);
            List<CompletableFuture<ResultItem>> futures = neIds.stream()
                    .map(neId -> CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    APS_SEMAPHORE.acquire();
                                    return clearSingleDeviceApsSwitchLog(neId);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    return null;
                                } finally {
                                    APS_SEMAPHORE.release();
                                }
                            },
                            AsynchronousExecutor.getCleanupExecutor())).collect(
                            Collectors.toList());
            CompletableFuture<Void> allDone = CompletableFuture.allOf(
                    futures.toArray(new CompletableFuture[0]));

            CompletableFuture<List<ResultItem>> allResults = allDone.thenApply(
                    v -> futures.stream().map(CompletableFuture::join).collect(
                            Collectors.toList()));
            List<ResultItem> resultItems = allResults.get(30, TimeUnit.SECONDS);
            ClearNeApsSwitchLogsOutput clearApsSwitchLogOutput = new ClearNeApsSwitchLogsOutputBuilder()
                    .setResultItem(resultItems)
                    .build();
            return SerializeUtil.serializeRpcOutput2Json(clearApsSwitchLogOutput);
        } catch (Exception ex) {
            log.error("failed to clear  the ne aps switch logs ,the reason is :{}", ex.getMessage(),
                    ex);
            if (ex instanceof CommonException) {
                throw (CommonException) ex;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        ex.getMessage(), ex);
            }
        }
    }


    @Override
    public String manageApsSwitch(String input) {
        log.debug("manage ne module aps switch,the input is:{}", input);
        try {
            ManageApsSwitchInput manageApsSwitchInput = SerializeUtil.parseRpcInput(input,
                    ManageApsSwitchInput.class);
            inputValidator.validateManageApsSwitchInput(manageApsSwitchInput);
            String neId = manageApsSwitchInput.getNeId().getValue();
            Adapter adapter = adapterBalancer.getAdapterForNe(neId);
            ApsSwitchOutput apsSwitchOutput = adapterRpc.apsSwitch(adapter, neId,
                    manageApsSwitchInput.getName(), manageApsSwitchInput.getPath(),
                    manageApsSwitchInput.getIndex(),
                    Action.forValue(manageApsSwitchInput.getAction().getIntValue()));
            RpcResultType returnCode = apsSwitchOutput.getReturnCode();
            ManageApsSwitchOutputBuilder manageApsSwitchOutputBuilder = new ManageApsSwitchOutputBuilder();
            if (returnCode.equals(RpcResultType.Success)) {
                log.error("success to manage the aps switch");
                manageApsSwitchOutputBuilder.setReturnCode(RpcResultType.Success);
                manageApsSwitchOutputBuilder.setReturnMessage(apsSwitchOutput.getReturnMessage());
            } else {
                log.error("unable to manage the aps switch the message is:{}",
                        apsSwitchOutput.getReturnMessage());
                manageApsSwitchOutputBuilder.setReturnCode(returnCode);
                manageApsSwitchOutputBuilder.setReturnMessage(apsSwitchOutput.getReturnMessage());
            }
            return SerializeUtil.serializeRpcOutput2Json(manageApsSwitchOutputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to manage aps switch, the reason is:{}", ex.getMessage(), ex);
            if (ex instanceof CommonException) {
                throw (CommonException) ex;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        ex.getMessage(), ex);
            }
        }
    }


    private ResultItem clearSingleDeviceApsSwitchLog(String neId) {
        log.debug("clear current neId:{} device aps switch log", neId);
        ResultItemBuilder resultItemBuilder = new ResultItemBuilder();
        resultItemBuilder.setDeviceId(neId);
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        try {
            log.debug("send clear aps switch log by neId:{} through adapter:{}", neId,
                    adapter.getName());
            ClearApsSwitchLogOutput clearApsSwitchLogOutput = adapterRpc.clearApsSwitchLog(adapter,
                    neId);
            if (clearApsSwitchLogOutput.getReturnCode() == RpcResultType.Success) {
                resultItemBuilder.setStatus(Status.SUCCESS);
                resultItemBuilder.setMessage(CLEAR_SUCCESS);
            } else {
                resultItemBuilder.setStatus(Status.FAILURE);
                resultItemBuilder.setMessage(clearApsSwitchLogOutput.getReturnMessage());
            }
        } catch (Exception e) {
            log.warn("clear log failed for {}: {}", neId, e.getMessage());
            resultItemBuilder.setStatus(Status.FAILURE);
            resultItemBuilder.setMessage(e.getMessage());
        }

        return resultItemBuilder.build();
    }
}
