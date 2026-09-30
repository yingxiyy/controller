/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.impl;

import lombok.Data;
import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YYX
 * @version 1.0
 */
public class StepResult {
    private String nodeId;
    private List<ErrorInfo> error;

    public StepResult(String nodeId) {
        this.nodeId = nodeId;
        this.error = new ArrayList<>();
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<ErrorInfo> getError() {
        return error;
    }

    public void addError(String objId, CommonException exception) {
        error.add(new ErrorInfo(objId, exception));
    }

    public boolean hasError() {
        return !error.isEmpty();
    }

    public class ErrorInfo {
        private String objId;
        private CommonException exception;

        public ErrorInfo(String objId, CommonException exception) {
            this.objId = objId;
            this.exception = exception;
        }

        public String getObjId() {
            return objId;
        }

        public CommonException getException() {
            return exception;
        }
    }
}
