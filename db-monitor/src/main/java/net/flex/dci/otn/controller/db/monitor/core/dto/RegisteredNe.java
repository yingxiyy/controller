package net.flex.dci.otn.controller.db.monitor.core.dto;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/16 13:48
 */
@Data
@Builder
@AllArgsConstructor
public class RegisteredNe implements Serializable {

    private List<NeInfo> ne;

}
