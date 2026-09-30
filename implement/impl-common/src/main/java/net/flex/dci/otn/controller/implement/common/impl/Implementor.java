package net.flex.dci.otn.controller.implement.common.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.utils.OpNodeMerger;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class Implementor {
    private String linkId;
    protected ImplementState targetState;
    private final MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
            MultipleTransaction.class);

    public Implementor(String linkId, ImplementState targetState) {
        this.linkId = linkId;
        this.targetState = targetState;
    }

    public ImplementState getTargetState() {
        return targetState;
    }

    public String getLinkId() {
        return linkId;
    }

    public void startSyncAction() {
    }


    protected void store2DB(ChangedObject changedObject) {
        log.debug("start save to mongo");
        changedObject.unsetAllPhyOpNode();
        mongoTransaction.save(changedObject);

        log.debug("start merge to OP\"");

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
