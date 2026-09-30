package net.flex.dci.otn.controller.sftp.manager.components;

import net.flex.dci.otn.controller.sftp.manager.model.AntPathInfo;

/**
 * @version 1.0
 * @date 6/7/2023 3:21 PM
 */
public interface FtpPathMatcher {

    AntPathInfo getAntPathInfo(String url);
}
