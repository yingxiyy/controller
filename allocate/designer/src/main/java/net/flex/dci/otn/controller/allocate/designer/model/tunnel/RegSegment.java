/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import lombok.Data;

@Data
public class RegSegment {

    private String srcNodeId;
    private String srcTpFriendlyName;//<cardType-shelf-slot-port>, e.g. T2X4C8-1-1-L1
    private String destNodeId;
    private String destTpFriendlyName;//<cardType-shelf-slot-port>, e.g. T2X4C8-1-1-L1


    public RegSegment(String srcNodeId, String srcTpFriendlyName, String destNodeId, String destTpFriendlyName) {
        this.srcNodeId = srcNodeId;
        this.srcTpFriendlyName = srcTpFriendlyName;
        this.destNodeId = destNodeId;
        this.destTpFriendlyName = destTpFriendlyName;
    }
}
