/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.dfs;

import java.util.List;
import java.util.Objects;

public class DFSPath {
    List<String> paths;
    List<Long> frequencies;

    public DFSPath(List<String> paths, List<Long> frequencies) {
        this.paths = paths;
        this.frequencies = frequencies;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DFSPath)) return false;
        DFSPath dfsPath = (DFSPath) o;
        return Objects.equals(paths, dfsPath.paths) && Objects.equals(frequencies, dfsPath.frequencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paths, frequencies);
    }
}
