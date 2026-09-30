package net.flex.dci.otn.controller.nms.nms.component.resource.frequency.impl;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;

@Builder
@Data
public class FrequencyScope implements Serializable {

    private int lower;
    private int upper;
    private int centre;
    private int index;
    private ImplementState implementState; // free / implemented / allocated
    private String ochLinkId;
    private String ochLinkFriendlyName;
}