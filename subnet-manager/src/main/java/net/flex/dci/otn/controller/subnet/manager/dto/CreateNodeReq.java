package net.flex.dci.otn.controller.subnet.manager.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateNodeReq implements Serializable {

    private String name;

    private String parentSubNetId;

}
