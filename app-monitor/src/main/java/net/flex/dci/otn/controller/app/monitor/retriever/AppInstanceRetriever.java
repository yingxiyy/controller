package net.flex.dci.otn.controller.app.monitor.retriever;

import java.util.List;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;

/**
 * @version 1.0
 * @date 2022/6/28 16:46
 */
public interface AppInstanceRetriever {

    List<InstanceInfo> getAllAppInstances();
}
