/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.Dijkstra;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;

public class DJNode {

    private String siteLinkId;//sitelink id

    public String getSiteA() {
        return siteA;
    }

    public String getSiteZ() {
        return siteZ;
    }

    private String siteA;
    private String siteZ;

    public List<Long> getAvailableCentFrequency() {
        return availableCentFrequency;
    }

    private List<Long> availableCentFrequency;

    private LinkedList<DJNode> shortestPath = new LinkedList<>();

    private Integer distance = Integer.MAX_VALUE;

    private Map<DJNode, Integer> adjacentNodes = new HashMap<>();
    private Map<String, String> adjacentWssLinkIds = new HashMap<>();//<siteLink,wssLink>, key is siteLink ID of adjacentNode

    public DJNode(String siteLinkId, List<Long> availableCentFrequency) {
        this.siteLinkId = siteLinkId;
        this.availableCentFrequency = availableCentFrequency;
        this.siteA= SiteLinkIdNamingRule.getSiteA(siteLinkId);
        this.siteZ= SiteLinkIdNamingRule.getSiteZ(siteLinkId);
    }

    public void addDestination(DJNode destination, int distance) {
        adjacentNodes.put(destination, distance);
    }

    public String getSiteLinkId() {
        return siteLinkId;
    }

    public void setSiteLinkId(String siteLinkId) {
        this.siteLinkId = siteLinkId;
    }

    public Map<DJNode, Integer> getAdjacentNodes() {
        return adjacentNodes;
    }

    public void setAdjacentNodes(Map<DJNode, Integer> adjacentNodes) {
        this.adjacentNodes = adjacentNodes;
    }

    public Integer getDistance() {
        return distance;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    public List<DJNode> getShortestPath() {
        return shortestPath;
    }

    public void setShortestPath(LinkedList<DJNode> shortestPath) {
        this.shortestPath = shortestPath;
    }

    public String getWssLinkId(String siteLinkId) {
        return adjacentWssLinkIds.get(siteLinkId);
    }

    public void addWssLink(String adjSiteLinkId, String wssLinkId) {
        this.adjacentWssLinkIds.put(adjSiteLinkId, wssLinkId);
    }
}
