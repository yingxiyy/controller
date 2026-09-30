package net.flex.dci.otn.controller.nms.properties.topo;

import lombok.Data;
import org.springframework.util.AntPathMatcher;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static net.flex.dci.otn.controller.nms.utils.Constants.CHARSET_UTF8;

/**
 * @version 1.0
 * @date 2022/4/27 13:39
 */
@Data
public class TopologyAntPathMatcherMap implements Serializable {

    List<TopologyAntPathMatcher> pathMatchers;

    public Map<String, TopologyAntPathMatcher> getPathMatcherMap() {
        return this.getPathMatchers()
                .stream().collect(
                        Collectors.toMap(TopologyAntPathMatcher::getAntPathMatcher, m -> m));
    }

    public TopologyAntPathMatcher getTopologyAntPathMatcherByIdentifier(String identifier) throws UnsupportedEncodingException {
        identifier = URLDecoder.decode(identifier, CHARSET_UTF8);
        Map<String, TopologyAntPathMatcher> pathMatcherMap = this.getPathMatcherMap();
        TopologyAntPathMatcher relativeTopologyPathMatcher = getRelativePathMatcher(identifier,
                pathMatcherMap);
        return relativeTopologyPathMatcher;
    }

    private TopologyAntPathMatcher getRelativePathMatcher(String identifier,
                                                          Map<String, TopologyAntPathMatcher> pathMatcherMap) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        Set<String> antPaths = pathMatcherMap.keySet();
        Optional<String> path = antPaths.stream()
                .filter(antPath -> antPathMatcher.match(antPath, identifier))
                .findAny();
        return path.map(pathMatcherMap::get).orElse(null);
    }


}
