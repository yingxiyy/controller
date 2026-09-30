/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.model;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;

@Builder
@Getter
@ToString
public class AllocateOtStartInfo {

    @NonNull
    OtRouteInfo otRouteInfo;

    //当需要建立一条新的OSlink需要的信息
    String frequencyString;
    BigInteger centFreq;
    ImmutablePair<String, String> olsPeerTps;

}
