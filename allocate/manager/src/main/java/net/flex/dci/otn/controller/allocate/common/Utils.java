package net.flex.dci.otn.controller.allocate.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Utils {

    private final static MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);

    static public void store2DB(ChangedObject changedObject) {
        log.debug("start save to mongo");
        changedObject.unsetAllPhyOpNode();
        mongoTransaction.save(changedObject);

        log.debug("start merge to OP");

        OpNodeMerger opMerger = new OpNodeMerger();

        List<String> nodes = new ArrayList<>(changedObject.getChangedPhyNodeList().keySet());
        int batchSize = 8;
        for (int i = 0; i < nodes.size(); i += batchSize) {
            int end = Math.min(i + batchSize, nodes.size());
            List<String> batch = nodes.subList(i, end);

            batch.parallelStream().forEach(opMerger::merge);
        }
        log.debug("merge to OP done");
    }
}
