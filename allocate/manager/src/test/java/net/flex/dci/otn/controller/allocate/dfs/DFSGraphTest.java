/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.dfs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DFSGraphTest {

    @Test
    public void scenario1() {
        DFSGraph graph = new DFSGraph();

        graph.addNode(new DFSNode("A", Arrays.asList(1l, 2l, 3l, 4l)));
        graph.addNode(new DFSNode("B", Arrays.asList(1l, 2l, 3l, 4l)));
        graph.addNode(new DFSNode("C", Arrays.asList(1l, 2l, 3l, 4l)));
        graph.addNode(new DFSNode("D", Arrays.asList(1l, 2l, 3l, 4l)));
//        graph.addNode(new DFSNode("E", Arrays.asList(1l, 2l, 3l, 4l)));
        graph.addNode(new DFSNode("F", Arrays.asList(1l, 2l, 3l, 4l)));

        graph.addEdge("A", "F", "AF");
        graph.addEdge("A", "B", "AB");
        graph.addEdge("A", "C", "AC");
        graph.addEdge("B", "C", "BC");
        graph.addEdge("B", "F", "BF");
        graph.addEdge("C", "D", "CD");
        graph.addEdge("D", "F", "DF");

        List<DFSPath> expectedPaths = new ArrayList<>();
        List<String> path1 = Arrays.asList("A", "AB", "B", "BF", "F");
        List<String> path2 = Arrays.asList("A", "AB", "B", "BC", "C", "CD", "D", "DF", "F");
        List<String> path3 = Arrays.asList("A", "AC", "C", "CD", "D", "DF", "F");
        List<String> path4 = Arrays.asList("A", "AC", "C", "BC", "B", "BF", "F");
        List<String> path5 = Arrays.asList("A", "AF", "F");
        expectedPaths.add(new DFSPath(path1, Arrays.asList(1l, 2l, 3l, 4l)));
        expectedPaths.add(new DFSPath(path2, Arrays.asList(1l, 2l, 3l, 4l)));
        expectedPaths.add(new DFSPath(path3, Arrays.asList(1l, 2l, 3l, 4l)));
        expectedPaths.add(new DFSPath(path4, Arrays.asList(1l, 2l, 3l, 4l)));
        expectedPaths.add(new DFSPath(path5, Arrays.asList(1l, 2l, 3l, 4l)));

        List<DFSPath> actualPaths = graph.findAllPaths("A", "F");

        Assertions.assertEquals(expectedPaths.size(), actualPaths.size());
        for (DFSPath expectedPath : expectedPaths) {
            Assertions.assertTrue(actualPaths.contains(expectedPath));
        }
    }

    @Test
    public void scenario2() {
        DFSGraph graph = new DFSGraph();

        graph.addNode(new DFSNode("A", Arrays.asList(1l, 2l, 3l, 4l)));
        graph.addNode(new DFSNode("B", Arrays.asList(1l, 2l, 3l)));
        graph.addNode(new DFSNode("C", Arrays.asList(1l, 2l, 4l)));
        graph.addNode(new DFSNode("D", Arrays.asList(4l)));
//        graph.addNode(new DFSNode("E", Arrays.asList(1l, 2l, 3l, 4l)));
        graph.addNode(new DFSNode("F", Arrays.asList(3l, 4l, 5l, 6l)));

        graph.addEdge("A", "F", "AF");
        graph.addEdge("A", "B", "AB");
        graph.addEdge("A", "C", "AC");
        graph.addEdge("B", "C", "BC");
        graph.addEdge("B", "F", "BF");
        graph.addEdge("C", "D", "CD");
        graph.addEdge("D", "F", "DF");

        List<DFSPath> expectedPaths = new ArrayList<>();
        List<String> path1 = Arrays.asList("A", "AB", "B", "BF", "F");
        List<String> path3 = Arrays.asList("A", "AC", "C", "CD", "D", "DF", "F");
        List<String> path5 = Arrays.asList("A", "AF", "F");
        expectedPaths.add(new DFSPath(path1, Arrays.asList(3l)));
        expectedPaths.add(new DFSPath(path3, Arrays.asList(4l)));
        expectedPaths.add((new DFSPath(path5, Arrays.asList(3l, 4l))));

        List<DFSPath> actualPaths = graph.findAllPaths("A", "F");

        Assertions.assertEquals(expectedPaths.size(), actualPaths.size());
        for (DFSPath expectedPath : expectedPaths) {
            Assertions.assertTrue(actualPaths.contains(expectedPath));
        }

        //reverse
        Collections.reverse(path1);
        Collections.reverse(path3);
        Collections.reverse(path5);

        actualPaths = graph.findAllPaths("F", "A");

        Assertions.assertEquals(expectedPaths.size(), actualPaths.size());
        for (DFSPath expectedPath : expectedPaths) {
            Assertions.assertTrue(actualPaths.contains(expectedPath));
        }


    }

}