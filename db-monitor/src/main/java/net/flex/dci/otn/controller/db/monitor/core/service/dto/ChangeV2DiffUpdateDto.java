package net.flex.dci.otn.controller.db.monitor.core.service.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.db.monitor.core.enums.OperationMethod;
import org.bson.Document;

/**
 * @version 1.0
 * @date 7/10/2025 2:26 PM
 */
@Data
@Builder
public class ChangeV2DiffUpdateDto implements Serializable {

    private Document changeBody;

    private OperationMethod operationMethod;
}
