package net.flex.dci.otn.controller.system.config.common.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NtpServerOutput implements Serializable {
    private String ipv4Address;

    private String ipv6Address;

    private String timezone;

    private Long updatedAt;

    private String updatedBy;
}
