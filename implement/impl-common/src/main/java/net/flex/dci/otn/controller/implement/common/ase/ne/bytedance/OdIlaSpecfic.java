package net.flex.dci.otn.controller.implement.common.ase.ne.bytedance;

import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificParamNode;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificalParam;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class OdIlaSpecfic extends SpecificParamNode implements SpecificalParam {
    public OdIlaSpecfic(ChangedObject changedObject, Node node, RouteInfo rInfo) {
        super(changedObject, node, rInfo);
    }

    //change default value and stored in changedObject
    public void set() {
        //配置CHASSIS-1-1的子框类型为BONE_OPC
//        changeChassisType();

        done();
    }
}
