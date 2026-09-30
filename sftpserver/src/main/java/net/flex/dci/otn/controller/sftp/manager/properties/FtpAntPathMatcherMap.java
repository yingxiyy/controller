package net.flex.dci.otn.controller.sftp.manager.properties;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @date 6/7/2023 3:09 PM
 */
@Data
public class FtpAntPathMatcherMap implements Serializable {

    List<FtpAntPathMatcher> pathMatchers;

    public Map<String, FtpAntPathMatcher> getFtpPathMatcherMap() {
        return pathMatchers.stream().collect(Collectors.toMap(FtpAntPathMatcher::getPath, m -> m));
    }
}
