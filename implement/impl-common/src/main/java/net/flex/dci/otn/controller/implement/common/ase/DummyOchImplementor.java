package net.flex.dci.otn.controller.implement.common.ase;

import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

import java.util.List;

public class DummyOchImplementor {
    private OchImplementInSequence implementor;

    public DummyOchImplementor(ChangedObject changedObject, Link impactedSiteLink) {
        this.implementor = new OchImplementInSequence(changedObject, impactedSiteLink);
    }

    //一条条下发太慢了
    @Deprecated
    public void implementAll_timeConsumer(Long taskGroupId, List<Link> ochLinkList) {
        if (ochLinkList != null && !ochLinkList.isEmpty()) {
            implementor.setTaskGroupId(taskGroupId);
            implementor.setOchList(ochLinkList);
            implementor.startInsert();
        }
    }

    //一条条下发太慢了
    @Deprecated
    public void deImplementAll_timeConsumer(Long taskGroupId, List<Link> ochLinkList) {
        if (ochLinkList != null && !ochLinkList.isEmpty()) {
            implementor.setTaskGroupId(taskGroupId);
            implementor.setOchList(ochLinkList);
            implementor.startRemove();
        }
    }

    public void implementAll(Long taskGroupId, List<Link> ochLinkList) {
        if (ochLinkList != null && !ochLinkList.isEmpty()) {
            implementor.setTaskGroupId(taskGroupId);
            implementor.setOchList(ochLinkList);
            implementor.startInsert();
        }
    }

    public void deImplementAll(Long taskGroupId, List<Link> ochLinkList) {
        if (ochLinkList != null && !ochLinkList.isEmpty()) {
            implementor.setTaskGroupId(taskGroupId);
            implementor.setOchList(ochLinkList);
            implementor.startRemove();
        }
    }
}
