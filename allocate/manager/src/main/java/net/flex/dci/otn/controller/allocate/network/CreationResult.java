package net.flex.dci.otn.controller.allocate.network;

import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

@Data
public class CreationResult {
    /**
     * link fake name, provided from UI when create create netwrok
     */
    private String linkName;

    /**
     * created siteLink object
     */
    private Link createdLink;

    /**
     * if error happen this isn't empty
     */
    private String errorInfo;
}
