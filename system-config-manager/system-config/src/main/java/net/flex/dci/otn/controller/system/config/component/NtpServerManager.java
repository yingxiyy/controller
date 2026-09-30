package net.flex.dci.otn.controller.system.config.component;

import net.flex.dci.otc.mongo.mdoel.ntp.NtpConfig;
import net.flex.dci.otn.controller.system.config.common.dto.NtpServerConfig;

/**
 * 2025/12/29
 *
 * @author musa
 * @version 1.0
 **/
public interface NtpServerManager {

    NtpConfig configNtpServer(NtpServerConfig ntpServerConfig,String author);
}
