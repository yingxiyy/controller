/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.ne.Card;

@Builder
@Data
class CardTps {

    @NonNull
    Card card;
    @NonNull
    Map<String, String> portNameTpMap;//key is portName, value is TpId    @NonNull
    Map<String, String> slavePortNameTpMap;//key is portName, value is TpId
    Map<String, String> thirdPortNameTpMap;//used by Bone2.0 OLP3-3 port C in 1+2 mode

    Set<String> busyTpIds;

}
