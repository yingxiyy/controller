package net.flex.dci.otc.controller.status.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmStateResult;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;

/**
 * attribute state result for the alarm align operation state
 *
 * @version 1.0
 * @date 8/23/2023 10:29 AM
 */
@Data
@Builder
public class PhysicalStateResult implements Serializable {

    private AlarmStateResult alarmStateResult;

    private AlignStateResult alignStateResult;

    private OperationStateResult operationStateResult;
}
