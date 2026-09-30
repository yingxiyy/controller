package net.flex.dci.otn.controller.app.monitor.retriever.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.app.monitor.retriever.AppInstanceRetriever;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/6/28 16:47
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AppInstanceRetrieverImpl implements AppInstanceRetriever {

    @Override
    public List<InstanceInfo> getAllAppInstances() {
        log.debug("start to retrieve all the system instance info");
        List<InstanceInfo> instanceInfos = new ArrayList<>();
        List<InstanceDetails> configInstances = DciInstancesUtils.getAllConfigInstances();
        List<InstanceDetails> stateInstances = DciInstancesUtils.getAllStateInstances();
        Map<String, InstanceDetails> configInstanceMap = configInstances.stream()
                .collect(Collectors.toMap(InstanceDetails::getId,
                        instanceDetails -> instanceDetails));
        Map<String, InstanceDetails> stateInstanceMap = stateInstances.stream()
                .collect(Collectors.toMap(InstanceDetails::getId,
                        instanceDetails -> instanceDetails));
        for (String instanceId : configInstanceMap.keySet()) {
            InstanceDetails instanceConfigDetail = configInstanceMap.get(instanceId);
            InstanceDetails instanceStateDetail = stateInstanceMap.get(instanceId);

            InstanceInfo instanceInfo = new InstanceInfo();
            instanceInfo.setId(instanceId);
            instanceInfo.setData(instanceConfigDetail);
            long currentTime = System.currentTimeMillis();
            if (instanceStateDetail == null) {
                //the instance is already down;
                instanceInfo.setAlive(false);
                instanceInfo.setDuration(currentTime - instanceConfigDetail.getLatestDeadTime());
                instanceInfo.setRecoverTimes(instanceConfigDetail.getRestartTimes());
            } else {
                instanceInfo.setAlive(true);
                instanceInfo.setDuration(currentTime - instanceStateDetail.getCreateTime());
            }
            instanceInfos.add(instanceInfo);
        }
        return instanceInfos;
    }
}
