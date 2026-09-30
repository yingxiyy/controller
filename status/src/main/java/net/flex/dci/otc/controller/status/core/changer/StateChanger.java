package net.flex.dci.otc.controller.status.core.changer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.alarm.AlarmStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.AlignStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.OperStateChanger;
import net.flex.dci.otc.controller.status.dto.PhysicalStateResult;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/23/2023 10:27 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StateChanger implements IStateChanger<PhysicalStateResult> {

    private final AlarmStateChanger alarmStateChanger;

    private final OperStateChanger operStateChanger;

    private final AlignStateChanger alignStateChanger;

    @Autowired
    @Qualifier("stateUpdateExecutor")
    private ExecutorService executor;

    @Override
    public void changeState(PhysicalStateResult state) {
        log.debug("start to change the physical state result");

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        AlarmStateResult alarmStateResult = state.getAlarmStateResult();
        if (null != alarmStateResult) {
            futures.add(CompletableFuture.runAsync(() ->
                            alarmStateChanger.changeState(alarmStateResult)
                    , executor));

        }
        OperationStateResult operationStateResult = state.getOperationStateResult();
        if (null != operationStateResult) {
//            operStateChanger.changeState(operationStateResult);
            futures.add(CompletableFuture.runAsync(
                    () -> operStateChanger.changeState(operationStateResult), executor));
        }
        AlignStateResult alignStateResult = state.getAlignStateResult();
        if (null != alignStateResult) {
            futures.add(CompletableFuture.runAsync(
                    () -> alignStateChanger.changeState(alignStateResult), executor));
        }
        if (!futures.isEmpty()) {
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(5, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.warn("State update timeout");
            } catch (Exception e) {
                log.error("State update failed", e);
            }
        }
    }
}
