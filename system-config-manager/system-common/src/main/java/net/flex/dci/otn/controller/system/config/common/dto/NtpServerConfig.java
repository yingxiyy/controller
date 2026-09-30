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
public class NtpServerConfig implements Serializable {

    private String ipv4Ntp;

    private String ipv6Ntp;

    @Builder.Default
    private String timezone = "Asia/Shanghai";
}
