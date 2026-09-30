package net.flex.dci.otn.controller.cli.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/1/18
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsFrontMessage implements Serializable {

    private String type;

    private String content;

    private Integer cols;

    private Integer rows;

    private String message;

    public boolean isValidType() {
        return "command".equals(type)
                || "interactive".equals(type)
                || "resize".equals(type)
                || "error".equals(type);
    }
}
