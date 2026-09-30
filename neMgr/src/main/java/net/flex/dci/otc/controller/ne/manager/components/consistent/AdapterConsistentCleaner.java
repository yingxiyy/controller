package net.flex.dci.otc.controller.ne.manager.components.consistent;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.springframework.stereotype.Component;

/**
 * 2025/6/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class AdapterConsistentCleaner implements ConsistentCleaner {

    private final AdapterDao adapterDao;

    @Override
    public void checkAndCleanInconsistent() {
        log.debug("clean inconsistent adapter from db to zookeeper");
        List<InstanceDetails> adapterMetrics = MapperType.ADAPTER.getModuleNames()
                .stream()
                .flatMap(moduleName -> DciInstancesUtils.getStateInstancesByModuleName(
                        moduleName).stream()).collect(Collectors.toList());
        List<Adapter> adapters = adapterDao.getAdapters();
        _checkAndCleanInconsistent(adapterMetrics, adapters);
    }

    private void _checkAndCleanInconsistent(List<InstanceDetails> adapterMetrics,
            List<Adapter> adapters) {
        log.debug("check inconsistent adapter and clean");
        Map<String, List<Adapter>> adapterMap = adapters.stream()
                .collect(Collectors.groupingBy(
                        adapter -> adapter.getName().getValue()
                ));
        adapterMap.forEach((name, adapterList) -> {
            if (adapterList.size() > 1) {
                log.error("Adapter name {} have {} duplicated duplicated key",
                        name, adapterList.size());
                adapterDao.deleteAdapter(name);
            }
        });
        Map<String, InstanceDetails> adapterMetricMap = adapterMetrics.stream().collect(
                Collectors.toMap(InstanceDetails::getId,
                        instanceDetails -> instanceDetails));
        Set<String> adapterKeys = adapterMap.keySet();
        Set<String> adapterMetricKeys = adapterMetricMap.keySet();

        Set<String> indbAdapterIds = NeManagerUtils.getDifferenceSetByGuava(adapterKeys,
                adapterMetricKeys);
        Set<String> inMetricAdapterIds = NeManagerUtils.getDifferenceSetByGuava(adapterMetricKeys,
                adapterKeys);
        log.debug("only in db adapter:{}", indbAdapterIds);
        log.debug("only in zookeeper metal data adapter:{}", inMetricAdapterIds);

        List<Adapter> inMetricAdapters = inMetricAdapterIds.stream().map(adapterName -> {
            InstanceDetails instanceDetails = adapterMetricMap.get(adapterName);
            Adapter adapter = NeManagerUtils.convert2Adapter(instanceDetails);
            return adapter;
        }).collect(Collectors.toList());
        cleanInconsistent(indbAdapterIds, inMetricAdapters);
    }

    private void cleanInconsistent(Set<String> indbAdapterIds, List<Adapter> inMetricAdapters) {
        log.debug("remove adapter:{} in db and create adapters:{}", indbAdapterIds,
                inMetricAdapters);
        indbAdapterIds.forEach(adapterDao::deleteAdapter);
        inMetricAdapters.forEach(adapterDao::saveAdapter);
    }
}
