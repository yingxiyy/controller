package net.flex.dci.otn.controller.app.monitor.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zk.common.entity.InstanceInfo;

/**
 * @version 1.0
 * @date 2022/5/6 10:47
 */
public class SortUtils {


    /**
     * sort the instance details
     *
     * @param details
     * @return
     */
    public static List<InstanceDetails> sortInstance(List<InstanceDetails> details) {
        Comparator<InstanceDetails> moduleComparator = Comparator.comparing(
                InstanceDetails::getModule);
        details.sort(moduleComparator);
        return details;
    }

    public static List<InstanceInfo> sortInstanceInfo(List<InstanceInfo> infos) {
        Collections.sort(infos, new Comparator<InstanceInfo>() {
            @Override
            public int compare(InstanceInfo o1, InstanceInfo o2) {
                return o1.getData().getModule().compareTo(o2.getData().getModule());
            }
        });
        return infos;
    }
}
