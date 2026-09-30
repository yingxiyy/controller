package net.flex.dci.otn.controller.gateway.dispatch.selector;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;
import net.flex.dci.otn.controller.gateway.dispatch.utils.DispatchConstants;
import org.springframework.util.AntPathMatcher;

/**
 * @version 1.0
 * @date 2022/2/15 15:07
 */
@Slf4j
public class PathSelector extends AbstractSelector implements Serializable {


    public PathSelector() {

    }


    @Override
    public List<ServerInstance> selectByPath(String path) {
        log.info("select live instance by path:{} ", path);
//        String moduleName = RouteValueHolder.getInstanceName(path);
        Set<String> modules = retrieveModuleByPath(path);
//        if (moduleName == null) {
//            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
//                    "the request path not found");
//        }
        if (modules.isEmpty()) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "the request path not found");
        }
//        List<InstanceDetails> instanceDetails = DciInstancesUtils.getStateInstancesByModuleName(
//                moduleName);
        List<InstanceDetails> instanceDetails = getLiveInstance(modules);
        if (instanceDetails.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the target microservice  is not alive");
        }

        return instanceDetails.stream().map(instance -> this.convert2ServerInstance(instance, path))
                .collect(Collectors.toList());
    }

    private List<InstanceDetails> getLiveInstance(Set<String> modules) {
        List<InstanceDetails> instanceDetails = new ArrayList<>();
        instanceDetails = modules.stream().map(DciInstancesUtils::getStateInstancesByModuleName)
                .flatMap(
                        Collection::stream).collect(Collectors.toList());
        return instanceDetails;
    }

    private Set<String> retrieveModuleByPath(String path) {
        log.debug("get the module by path:{}", path);
        Map<String, List<String>> serviceEndpoints = DciInstancesUtils.getServiceEndpoints();
        Set<String> modules = new HashSet<>();
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        for (Map.Entry<String, List<String>> entry : serviceEndpoints.entrySet()) {
            String module = entry.getKey();
            List<String> endPoints = entry.getValue();
            for (String endPoint : endPoints) {
                if (antPathMatcher.match(endPoint, path)) {
                    modules.add(module);
                }
            }
        }
        //TODO :FILTER THE ADAPTER MODULE API
        modules = modules.stream()
                .filter(module -> !module.contains(DispatchConstants.ADAPTER_MODULE)).collect(
                        Collectors.toSet());
        return modules;
    }


}
