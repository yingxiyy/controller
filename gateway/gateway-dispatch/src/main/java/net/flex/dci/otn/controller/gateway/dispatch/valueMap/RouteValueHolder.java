package net.flex.dci.otn.controller.gateway.dispatch.valueMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.gateway.common.properties.route.Instance;
import net.flex.dci.otn.controller.gateway.common.properties.route.RouteMap;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

/**
 * @version 1.0
 * @date 2022/2/15 16:34
 */
@Data
@Slf4j
public class RouteValueHolder implements Serializable {


    private final static RouteMap routeMap;

    private final static Map<String, List<String>> modulePrefixMap = new HashMap<>();

    private final static Map<String, List<String>> moduleSubPathMap = new HashMap<>();

    static {
        routeMap = SpringBeanFinder.getBean(RouteMap.class);
        initMap(routeMap);
    }

    private static void initMap(RouteMap routeMap) {
        List<Instance> instances = routeMap.getInstances();
        for (Instance instance : instances) {
            modulePrefixMap.put(instance.getName(), instance.getPrefix());
            moduleSubPathMap.put(instance.getName(), instance.getSubPaths());
        }
    }

    /**
     * get instance name
     *
     * @param path
     * @return microservice name
     */
    public static String getInstanceName(String path) {
        //prefix find by path
        List<String> modules = findInstanceNameByPrefix(path);
        if (modules.size() == 0) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "the request path is not found");
        } else if (modules.size() == 1) {
            return modules.get(0);
        } else {
            return findInstanceNameBySubPath(modules, path);
        }
    }

    private static String findInstanceNameBySubPath(List<String> modules, String path) {
        log.debug("select the module by the sub-path,path is {}", path);
        for (String module : modules) {
            List<String> subPaths = moduleSubPathMap.get(module);
            if (subPaths != null && isIncludeSubPath(subPaths, path)) {
                return module;
            }
        }
        return null;
    }

    private static List<String> findInstanceNameByPrefix(String path) {
        List<String> names = new ArrayList<>();
        Set set = modulePrefixMap.entrySet();
        Iterator<Entry<String, List<String>>> iterator = set.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, List<String>> entry = iterator.next();
            List<String> prefixes = entry.getValue();
            if (isIncludePrefixes(prefixes, path)) {
                names.add(entry.getKey());
            }
        }
        return names;
    }

    /**
     * judge if is including the prefix path
     *
     * @param prefixes
     * @param path
     * @return
     */
    private static boolean isIncludePrefixes(List<String> prefixes, String path) {
        PathMatcher pathMatcher = new AntPathMatcher();
        long count = 0l;
        for (String prefix : prefixes) {
            if (pathMatcher.match(prefix, path)) {
                count++;
            }
        }
        return count > 0;
//        long count = prefixes.stream().filter(prefix -> pathMatcher.match(prefix, path)).count();
//        return count > 0;
    }


    private static boolean isIncludeSubPath(List<String> subPaths, String path) {
        long count = subPaths.stream().filter(path::contains).count();
        return count > 0;
    }


}
