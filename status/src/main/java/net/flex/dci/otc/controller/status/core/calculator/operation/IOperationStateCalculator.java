package net.flex.dci.otc.controller.status.core.calculator.operation;

import java.util.List;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;

/**
 * @version 1.0
 * @date 2022/4/8 16:37
 */
public interface IOperationStateCalculator<K, V> {

    K calculate(List<V> vs);

    K calculate(List<V> vs, OperStatus operStatus);
}
