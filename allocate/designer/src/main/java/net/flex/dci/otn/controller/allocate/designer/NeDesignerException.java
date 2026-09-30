/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer;

public class NeDesignerException extends Exception {

    public NeDesignerException(String errorMessage) {
        super(errorMessage);
    }

    public NeDesignerException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

}
