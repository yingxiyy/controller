package net.flex.dci.otn.ne.upgrade.tool.components;

import java.util.concurrent.ExecutionException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * 2026/4/8
 *
 * @author musa
 * @version 1.0
 **/
public interface NeManager {

    void unSupervisionNe(String neId) throws ExecutionException, InterruptedException;

    void supervisionNe(Node node) throws ExecutionException, InterruptedException;
}
