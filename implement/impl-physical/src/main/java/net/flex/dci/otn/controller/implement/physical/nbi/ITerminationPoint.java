/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;

import net.flex.dci.otc.common.exception.CommonException;

/**
 * @version 1.0
 * @date 2021/11/9 15:04
 */
public interface ITerminationPoint {

    /**
     * update termination point
     *
     * @param input
     * @return
     * @throws CommonException
     */
//    public UpdateTerminationPointOutput updateTerminationPoint(UpdateTerminationPointInput input)
//            throws CommonException;
    
    public UpdateTerminationPointOutput start(UpdateTerminationPointInput input) throws CommonException;

}
