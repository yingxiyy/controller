/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;

/**
 * @version 1.0
 * @date 2021/11/29 14:45
 */
@Data
@Builder
public class ViewNodeDto {

    private String nodeId;

    private AlarmSeverity alarmState;

    private String friendlyName;

    private Integer posX;

    private Integer posY;

    List<SupportingNode> supportingNodes;
}
