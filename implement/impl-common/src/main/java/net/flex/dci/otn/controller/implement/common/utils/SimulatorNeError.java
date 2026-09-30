package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class SimulatorNeError {
    private static int MAX_VALUE = 10;
    private static int number=20;
    private static final ChangedObject changedObject = new ChangedObject();

    public Boolean setValue(int number) {
        SimulatorNeError.number = number;

        return number < MAX_VALUE;
    }

    public static StepResult check(String nodeId) {
        StepResult result = new StepResult(nodeId);
        if (number < MAX_VALUE) {
            log.debug("simulator device error");
            try {
                Node node = changedObject.getChangedPhyNode(nodeId);
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
                int sleepTime = 10;
                if (nodeAttr.getNodeType().equals(NodeType.TD)) {
                    sleepTime = 40;
                }
                TimeUnit.SECONDS.sleep(sleepTime);
                int num = ThreadLocalRandom.current().nextInt(MAX_VALUE);
                if (num >= number) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "simulator device error");
                }
            } catch (InterruptedException e) {
                //do nothing;
            } catch (CommonException er) {
                log.error("Exception triggered", er);
                result.addError(nodeId, er);
            }
        }
        return result;
    }
}
