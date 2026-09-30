package net.flex.dci.otn.controller.sftp.manager.components.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.sftp.manager.components.FtpPathMatcher;
import net.flex.dci.otn.controller.sftp.manager.model.AntPathInfo;
import net.flex.dci.otn.controller.sftp.manager.properties.FtpAntPathMatcher;
import net.flex.dci.otn.controller.sftp.manager.properties.FtpAntPathMatcherMap;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @version 1.0
 * @date 6/7/2023 3:22 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FtpPathMatcherImpl implements FtpPathMatcher {

    private final FtpAntPathMatcherMap ftpAntPathMatcherMap;


    @Override
    public AntPathInfo getAntPathInfo(String url) {
        log.debug("start to get ant path info for the url:{}", url);

        AntPathInfo antPathInfo = _getAntPathInfo(url);
        return antPathInfo;
    }

    private AntPathInfo _getAntPathInfo(String url) {
        log.debug("get ant path info from url:{}", url);
        FtpAntPathMatcher ftpAntPathMatcher = getRelativePathMatcher(url);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String antPath = ftpAntPathMatcher.getPath();
        String variable = ftpAntPathMatcher.getPathVariable();
        //todo:assumption only have one variable
//        List<String> elements = Arrays.asList(variable.split(COMMA));
        Map<String, String> uriVariable = antPathMatcher.extractUriTemplateVariables(antPath,
                url);
        String pathVariableValue = uriVariable.getOrDefault(variable, null);
        return AntPathInfo.builder().pathVariable(variable).pathVariableValue(pathVariableValue).path(url).build();
    }


    private FtpAntPathMatcher getRelativePathMatcher(String identifier) {
        Map<String, FtpAntPathMatcher> pathMatcherMap = ftpAntPathMatcherMap.getFtpPathMatcherMap();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        Set<String> antPaths = pathMatcherMap.keySet();
        Optional<String> path = antPaths.stream()
                .filter(antPath -> antPathMatcher.match(antPath, identifier))
                .findAny();
        return path.map(pathMatcherMap::get).orElse(null);
    }
}
