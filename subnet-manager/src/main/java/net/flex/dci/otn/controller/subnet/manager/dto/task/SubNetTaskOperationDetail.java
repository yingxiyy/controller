package net.flex.dci.otn.controller.subnet.manager.dto.task;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/2/28
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubNetTaskOperationDetail implements Serializable {

    private Object request;

    private Object response;

}
