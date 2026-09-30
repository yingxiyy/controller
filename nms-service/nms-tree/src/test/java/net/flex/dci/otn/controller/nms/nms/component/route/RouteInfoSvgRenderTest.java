/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.component.route;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteInfoSvgRenderTest {

    private static final Path OUTPUT_DIR = Paths.get("target", "route-display-graph");
    private static final int LEFT = 48;
    private static final int TOP = 120;
    private static final int SITE_WIDTH = 320;
    private static final int ROW_HEIGHT = 150;
    private static final int SITE_GAP = 42;
    private static final int MAX_HOPS_PER_SITE = 4;

    @Test
    void shouldRenderRouteInfoJsonAsStaticHtmlSvg() throws Exception {
        Path routeInfoJson = findRouteInfoJson();
        JsonObject root = new JsonParser().parse(
                new String(Files.readAllBytes(routeInfoJson), StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonArray routeInfos = root.getAsJsonObject("output").getAsJsonArray("route-info");
        assertFalse(routeInfos.size() == 0, "route-info is empty: " + routeInfoJson);

        String html = renderHtml(routeInfoJson, routeInfos);
        Files.createDirectories(OUTPUT_DIR);
        Path output = OUTPUT_DIR.resolve("index.html");
        Files.write(output, html.getBytes(StandardCharsets.UTF_8));

        System.out.println("routeInfoJson=" + routeInfoJson.toAbsolutePath());
        System.out.println("routeSvgHtml=" + output.toAbsolutePath());
        assertTrue(Files.size(output) > 0, "rendered html is empty");
    }

    private static Path findRouteInfoJson() throws Exception {
        String configuredPath = System.getProperty("routeInfoJson");
        if (configuredPath != null && !configuredPath.trim().isEmpty()) {
            Path path = Paths.get(configuredPath.trim());
            assertTrue(Files.isRegularFile(path), "routeInfoJson does not exist: " + path);
            return path;
        }

        List<Path> jsonFiles = new ArrayList<>();
        if (Files.isDirectory(OUTPUT_DIR)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(OUTPUT_DIR,
                    "route-info-*.json")) {
                for (Path path : stream) {
                    jsonFiles.add(path);
                }
            }
        }
        assertFalse(jsonFiles.isEmpty(), "no route-info-*.json found under " + OUTPUT_DIR);
        jsonFiles.sort(Comparator.comparing(RouteInfoSvgRenderTest::lastModified).reversed());
        return jsonFiles.get(0);
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String renderHtml(Path sourceJson, JsonArray routeInfos) {
        List<RouteVariant> variants = collectVariants(routeInfos);
        int width = 1720;
        int height = 560;

        StringBuilder body = new StringBuilder();
        body.append("<!doctype html><html><head><meta charset=\"UTF-8\"><title>")
                .append(escapeHtml(sourceJson.getFileName().toString()))
                .append("</title><style>");
        body.append("body{margin:0;background:#f6f7f9;color:#20242a;font-family:Arial,"
                + "Helvetica,sans-serif;}header{padding:18px 24px;background:#fff;border-bottom:"
                + "1px solid #d8dde6;position:sticky;top:0;z-index:2}h1{font-size:18px;margin:0 "
                + "0 8px}.meta{font-size:12px;color:#5f6b7a}.legend{display:flex;gap:18px;"
                + "margin-top:10px;font-size:12px}.chip{display:inline-flex;align-items:center;"
                + "gap:6px}.dot{width:22px;height:4px;border-radius:4px}.wrap{padding:18px 24px}"
                + "svg{background:#fff;border:1px solid #d8dde6;box-shadow:0 1px 2px #0001}"
                + ".summary{margin-top:16px;border-collapse:collapse;background:#fff;font-size:"
                + "12px}.summary th,.summary td{border:1px solid #d8dde6;padding:6px 8px;"
                + "text-align:left}.warn{color:#b42318;font-weight:700}.ok{color:#067647;"
                + "font-weight:700}</style></head><body>");
        body.append("<header><h1>Route Info SVG Check</h1><div class=\"meta\">source: ")
                .append(escapeHtml(sourceJson.toAbsolutePath().toString()))
                .append("</div><div class=\"legend\">")
                .append(legend("primary", "#d92d20"))
                .append(legend("secondary", "#16a34a"))
                .append(legend("third", "#2563eb"))
                .append("</div></header><div class=\"wrap\">");
        body.append("<svg width=\"").append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ').append(height)
                .append("\" xmlns=\"http://www.w3.org/2000/svg\">");
        body.append("<defs><marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"9\" refY=\"5\" "
                + "markerWidth=\"7\" markerHeight=\"7\" orient=\"auto-start-reverse\">"
                + "<path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#6b7280\"/></marker></defs>");
        body.append(renderFrontLikeDiagram(variants, width, height));
        body.append("</svg>");
        body.append(renderSummary(variants));
        body.append("</div></body></html>");
        return body.toString();
    }

    private static String renderFrontLikeDiagram(List<RouteVariant> variants, int width,
            int height) {
        StringBuilder svg = new StringBuilder();
        if (variants.isEmpty()) {
            return svg.toString();
        }
        RouteVariant base = firstAvailableVariant(variants);
        String leftSite = firstSite(base);
        String rightSite = lastSite(base);
        String leftLabel = siteLabel(base, leftSite);
        String rightLabel = siteLabel(base, rightSite);

        int leftX = 24;
        int siteY = 68;
        int siteW = 680;
        int siteH = 420;
        int rightX = width - siteW - 24;
        int lineLeftX = leftX + siteW;
        int lineRightX = rightX;

        svg.append("<rect x=\"").append(leftX).append("\" y=\"").append(siteY)
                .append("\" width=\"").append(siteW).append("\" height=\"").append(siteH)
                .append("\" fill=\"#f7f7f8\" stroke=\"#ff0000\" stroke-width=\"3\"/>");
        svg.append("<rect x=\"").append(rightX).append("\" y=\"").append(siteY)
                .append("\" width=\"").append(siteW).append("\" height=\"").append(siteH)
                .append("\" fill=\"#f7f7f8\" stroke=\"#ff0000\" stroke-width=\"3\"/>");
        svg.append("<text x=\"").append(leftX + siteW / 2).append("\" y=\"")
                .append(siteY + siteH - 10)
                .append("\" text-anchor=\"middle\" font-size=\"14\" fill=\"#ff0000\">")
                .append(escapeXml(leftLabel)).append("</text>");
        svg.append("<text x=\"").append(rightX + siteW / 2).append("\" y=\"")
                .append(siteY + siteH - 10)
                .append("\" text-anchor=\"middle\" font-size=\"14\" fill=\"#ff0000\">")
                .append(escapeXml(rightLabel)).append("</text>");

        List<Hop> leftCommon = endpointHops(base, leftSite, true);
        List<Hop> rightCommon = endpointHops(base, rightSite, false);
        Point leftCommonPoint = renderEndpointGroup(svg, orderedAccessHops(leftCommon, true),
                leftX + 54, 220, "#ff0000", "access", true);
        Point rightCommonPoint = renderEndpointGroup(svg, orderedAccessHops(rightCommon, false),
                rightX + siteW - 356, 220, "#ff0000", "access", false);

        List<Point> leftOchPoints = new ArrayList<>();
        List<Point> rightOchPoints = new ArrayList<>();
        for (int i = 0; i < variants.size(); i++) {
            RouteVariant variant = variants.get(i);
            int laneY = 120 + i * 118;
            String color = colorOf(variant.type);
            List<Hop> leftHops = endpointHops(variant, leftSite, true);
            List<Hop> rightHops = endpointHops(variant, rightSite, false);
            int leftGroupX = leftX + 350;
            int rightGroupX = rightX + 40;
            List<Hop> leftOchHops = orderedOchHops(leftHops, true);
            List<Hop> rightOchHops = orderedOchHops(rightHops, false);

            Point leftPoint = renderEndpointGroup(svg, leftOchHops, leftGroupX, laneY - 40,
                    color, variant.label(), true);
            Point rightPoint = renderEndpointGroup(svg, rightOchHops, rightGroupX, laneY - 40,
                    color, variant.label(), false);
            leftOchPoints.add(groupEntrance(leftGroupX, leftPoint.y));
            rightOchPoints.add(groupExit(rightGroupX, rightPoint.y, rightOchHops.size()));
            svg.append("<line x1=\"").append(leftPoint.x).append("\" y1=\"").append(leftPoint.y)
                    .append("\" x2=\"").append(lineLeftX + 8).append("\" y2=\"")
                    .append(leftPoint.y).append("\" stroke=\"#707070\" stroke-width=\"2\"/>");
            svg.append("<line x1=\"").append(lineRightX - 8).append("\" y1=\"")
                    .append(rightPoint.y).append("\" x2=\"").append(rightPoint.x)
                    .append("\" y2=\"").append(rightPoint.y)
                    .append("\" stroke=\"#707070\" stroke-width=\"2\"/>");
            svg.append("<line x1=\"").append(lineLeftX + 8).append("\" y1=\"")
                    .append(leftPoint.y).append("\" x2=\"").append(lineRightX - 8)
                    .append("\" y2=\"").append(rightPoint.y)
                    .append("\" stroke=\"#303030\" stroke-width=\"2\"/>");
            if (!variant.regResources.isEmpty()) {
                renderRegResourceCard(svg, variant.regResources.get(0), width / 2 - 135,
                        laneY - 38, color);
            }
            svg.append("<text x=\"").append(width / 2).append("\" y=\"")
                    .append(variant.regResources.isEmpty() ? laneY - 10 : laneY - 48)
                    .append("\" text-anchor=\"middle\" font-size=\"12\" fill=\"")
                    .append(color).append("\">")
                    .append(escapeXml(routeLineLabel(variant))).append("</text>");
            String via = viaLabel(variant, leftSite, rightSite);
            if (!via.isEmpty()) {
                svg.append("<text x=\"").append(width / 2).append("\" y=\"")
                        .append(variant.regResources.isEmpty() ? laneY + 18 : laneY + 68)
                        .append("\" text-anchor=\"middle\" font-size=\"11\" fill=\"#667085\">")
                        .append(escapeXml(via)).append("</text>");
            }
        }
        for (Point point : leftOchPoints) {
            svg.append("<line x1=\"").append(leftCommonPoint.x).append("\" y1=\"")
                    .append(leftCommonPoint.y).append("\" x2=\"").append(point.x)
                    .append("\" y2=\"").append(point.y)
                    .append("\" stroke=\"#707070\" stroke-width=\"2\"/>");
        }
        for (Point point : rightOchPoints) {
            svg.append("<line x1=\"").append(point.x).append("\" y1=\"").append(point.y)
                    .append("\" x2=\"").append(rightCommonPoint.x).append("\" y2=\"")
                    .append(rightCommonPoint.y)
                    .append("\" stroke=\"#707070\" stroke-width=\"2\"/>");
        }
        return svg.toString();
    }

    private static void renderRegResourceCard(StringBuilder svg, RegResource reg, int x, int y,
            String routeColor) {
        svg.append("<g><title>").append(escapeXml(reg.tooltip())).append("</title>");
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"270\" height=\"92\" rx=\"8\" fill=\"#fffbeb\" ")
                .append("stroke=\"#d97706\" stroke-width=\"2.5\"/>");
        svg.append("<rect x=\"").append(x + 10).append("\" y=\"").append(y + 13)
                .append("\" width=\"48\" height=\"64\" rx=\"4\" fill=\"#fef3c7\" ")
                .append("stroke=\"").append(routeColor).append("\" stroke-width=\"2\"/>");
        svg.append("<text x=\"").append(x + 34).append("\" y=\"").append(y + 36)
                .append("\" text-anchor=\"middle\" font-size=\"11\" font-weight=\"700\" ")
                .append("fill=\"#92400e\">REG</text>");
        svg.append("<text x=\"").append(x + 34).append("\" y=\"").append(y + 53)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#92400e\">")
                .append(reg.equipment.size()).append(" card</text>");
        svg.append("<text x=\"").append(x + 68).append("\" y=\"").append(y + 19)
                .append("\" font-size=\"10\" font-weight=\"700\" fill=\"#78350f\">")
                .append(shortName(firstNotEmpty(reg.siteName, reg.siteId, "REG site")))
                .append(" / ").append(shortName(firstNotEmpty(reg.nodeName, reg.nodeId, "REG")))
                .append("</text>");
        svg.append(regCountText(x + 68, y + 38, "L ports", reg.lPorts.size()));
        svg.append(regCountText(x + 158, y + 38, "self XCs", reg.xcs.size()));
        svg.append(regCountText(x + 68, y + 57, "OS Links", reg.osLinks.size()));
        svg.append(regCountText(x + 158, y + 57, "Internal", reg.internalLinks.size()));
        svg.append("<text x=\"").append(x + 68).append("\" y=\"").append(y + 77)
                .append("\" font-size=\"8\" fill=\"#92400e\">ports: ")
                .append(escapeXml(shortList(reg.lPorts, 2))).append("</text></g>");
    }

    private static String regCountText(int x, int y, String label, int count) {
        return "<text x=\"" + x + "\" y=\"" + y
                + "\" font-size=\"9\" fill=\"#78350f\">" + label + ": " + count
                + "</text>";
    }

    private static String shortList(List<String> values, int limit) {
        List<String> shortValues = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, values.size()); i++) {
            shortValues.add(portOf(values.get(i)));
        }
        if (values.size() > limit) {
            shortValues.add("+" + (values.size() - limit));
        }
        return shortValues.toString();
    }

    private static RouteVariant firstAvailableVariant(List<RouteVariant> variants) {
        for (RouteVariant variant : variants) {
            if (!variant.sites.isEmpty()) {
                return variant;
            }
        }
        return variants.get(0);
    }

    private static Point groupEntrance(int groupX, int y) {
        return new Point(groupX + 42, y);
    }

    private static Point groupExit(int groupX, int y, int hopCount) {
        int shown = Math.min(4, Math.max(1, hopCount));
        return new Point(groupX + 42 + (shown - 1) * 70 + 74, y);
    }

    private static Point renderEndpointGroup(StringBuilder svg, List<Hop> hops, int x, int y,
            String color, String label, boolean leftSide) {
        List<Board> boards = boardsOf(hops);
        int shown = Math.min(4, boards.size());
        int groupW = Math.max(250, 48 + shown * 84);
        int groupH = 92;
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"").append(groupW).append("\" height=\"").append(groupH)
                .append("\" fill=\"none\" stroke=\"").append(color)
                .append("\" stroke-width=\"3\" stroke-dasharray=\"2 7\"/>");
        if (shown == 0) {
            svg.append("<text x=\"").append(x + groupW / 2).append("\" y=\"").append(y + 50)
                    .append("\" text-anchor=\"middle\" font-size=\"12\" fill=\"#b42318\">")
                    .append(escapeXml(label)).append(" no endpoint hops</text>");
            return new Point(leftSide ? x : x + groupW, y + groupH / 2);
        }
        int startX = x + 42;
        int step = 70;
        Point previous = null;
        Point edgePoint = null;
        Point entrancePoint = null;
        for (int i = 0; i < shown; i++) {
            Board board = boards.get(i);
            int hopX = startX + i * step;
            Point current = renderDevice(svg, board, hopX, y + 20, color, leftSide);
            if (entrancePoint == null) {
                entrancePoint = current;
            }
            if (previous != null) {
                svg.append("<line x1=\"").append(previous.x).append("\" y1=\"")
                        .append(previous.y).append("\" x2=\"").append(current.x)
                        .append("\" y2=\"").append(current.y).append("\" stroke=\"")
                        .append(color).append("\" stroke-width=\"2\"/>");
            }
            previous = current;
            edgePoint = current;
        }
        if (boards.size() > shown) {
            svg.append("<text x=\"").append(x + groupW - 34).append("\" y=\"").append(y + 16)
                    .append("\" text-anchor=\"end\" font-size=\"11\" fill=\"#475467\">+")
                    .append(boards.size() - shown).append("</text>");
        }
        if (edgePoint == null) {
            return new Point(leftSide ? x + groupW : x, y + groupH / 2);
        }
        return leftSide ? edgePoint : entrancePoint;
    }

    private static Point renderDevice(StringBuilder svg, Board board, int x, int y, String color,
            boolean leftSide) {
        Hop hop = board.primary;
        svg.append("<g><title>").append(escapeXml(hop.tooltip())).append("</title>");
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"74\" height=\"58\" fill=\"#eeeeef\" stroke=\"#666\" "
                        + "stroke-width=\"2\"/>");
        svg.append("<text x=\"").append(x + 37).append("\" y=\"").append(y + 10)
                .append("\" text-anchor=\"middle\" font-size=\"7\" fill=\"#555\">")
                .append(shortName(firstNotEmpty(hop.phyNodeName, neOf(hop.phyNodeId), "NE")))
                .append("</text>");
        int leftPortX = x;
        int rightPortX = x + 74;
        renderBoardPorts(svg, board, x, y);
        if (hop.linkHop) {
            svg.append("<line x1=\"").append(x + 15).append("\" y1=\"").append(y + 30)
                    .append("\" x2=\"").append(x + 59).append("\" y2=\"").append(y + 30)
                    .append("\" stroke=\"#777\" stroke-width=\"2\"/>");
        } else {
            renderDeviceSymbol(svg, hop, x, y, leftSide);
        }
        svg.append("<text x=\"").append(x + 37).append("\" y=\"").append(y + 70)
                .append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#555\">")
                .append(shortName(board.label))
                .append("</text></g>");
        return new Point(leftSide ? rightPortX : leftPortX, y + 30);
    }

    private static List<Board> boardsOf(List<Hop> hops) {
        Map<String, Board> boards = new LinkedHashMap<>();
        for (Hop hop : hops) {
            String key = boardKey(hop);
            Board board = boards.get(key);
            if (board == null) {
                board = new Board(key, boardLabel(hop), hop);
                boards.put(key, board);
            }
            board.ports.add(portLabel(hop));
        }
        return new ArrayList<>(boards.values());
    }

    private static String boardKey(Hop hop) {
        String name = firstNotEmpty(hop.tpName, portOf(hop.tpId), hop.phyNodeName);
        String normalized = name.replaceAll("-(C|L)[0-9]*$", "")
                .replaceAll("-[0-9]+(SIG|[ABC])$", "")
                .replaceAll("-M(PO|[0-9]+D[0-9]+)[0-9]*$", "")
                .replaceAll("-LINE$", "");
        return deviceKind(hop) + ":" + normalized;
    }

    private static String boardLabel(Hop hop) {
        String name = firstNotEmpty(hop.tpName, portOf(hop.tpId), hop.phyNodeName);
        return name.replaceAll("-(C|L)[0-9]*$", "")
                .replaceAll("-[0-9]+(SIG|[ABC])$", "")
                .replaceAll("-M(PO|[0-9]+D[0-9]+)[0-9]*$", "")
                .replaceAll("-LINE$", "");
    }

    private static String portLabel(Hop hop) {
        String name = firstNotEmpty(hop.tpName, portOf(hop.tpId), "");
        int index = name.lastIndexOf('-');
        return index < 0 ? name : name.substring(index + 1);
    }

    private static void renderBoardPorts(StringBuilder svg, Board board, int x, int y) {
        int count = Math.min(4, board.ports.size());
        int startY = y + 14 + Math.max(0, 4 - count) * 5;
        for (int i = 0; i < count; i++) {
            int portY = startY + i * 10;
            renderPort(svg, x, portY);
            renderPort(svg, x + 68, portY);
        }
    }

    private static void renderPort(StringBuilder svg, int x, int y) {
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"6\" height=\"12\" rx=\"1\" fill=\"#66b83f\" "
                        + "stroke=\"#3f8f21\" stroke-width=\"1\"/>");
    }

    private static void renderDeviceSymbol(StringBuilder svg, Hop hop, int x, int y,
            boolean leftSide) {
        String kind = deviceKind(hop);
        if ("olp".equals(kind)) {
            renderOlpSymbol(svg, x, y);
        } else if ("ira".equals(kind)) {
            renderIraSymbol(svg, x, y);
        } else if ("mux".equals(kind)) {
            renderMuxSymbol(svg, x, y, leftSide);
        } else {
            renderTerminalSymbol(svg, x, y);
        }
    }

    private static String deviceKind(Hop hop) {
        String name = (hop.phyNodeName + " " + hop.tpName + " " + hop.tpId).toUpperCase();
        if (name.contains("OLP")) {
            return "olp";
        }
        if (name.contains("IRA") || name.contains("AMPLIFIER") || name.contains("AMP")) {
            return "ira";
        }
        if (name.contains("MUX") || name.contains("M33") || name.contains("MPO")) {
            return "mux";
        }
        return "terminal";
    }

    private static void renderTerminalSymbol(StringBuilder svg, int x, int y) {
        renderBoardLabel(svg, x, y, "RL");
    }

    private static void renderMuxSymbol(StringBuilder svg, int x, int y, boolean leftSide) {
        renderBoardLabel(svg, x, y, "MUX");
        svg.append("<line x1=\"").append(x + 24).append("\" y1=\"").append(y + 40)
                .append("\" x2=\"").append(x + 50).append("\" y2=\"").append(y + 20)
                .append("\" stroke=\"#777\" stroke-width=\"1.4\"/>");
    }

    private static void renderIraSymbol(StringBuilder svg, int x, int y) {
        renderBoardLabel(svg, x, y, "IRA");
        svg.append("<rect x=\"").append(x + 22).append("\" y=\"").append(y + 18)
                .append("\" width=\"8\" height=\"24\" fill=\"#d8d8d8\" stroke=\"#777\" "
                        + "stroke-width=\"1\"/>");
        svg.append("<path d=\"M").append(x + 38).append(' ').append(y + 18)
                .append(" L").append(x + 54).append(' ').append(y + 30)
                .append(" L").append(x + 38).append(' ').append(y + 42)
                .append(" Z\" fill=\"none\" stroke=\"#777\" stroke-width=\"1.2\"/>");
    }

    private static void renderOlpSymbol(StringBuilder svg, int x, int y) {
        renderBoardLabel(svg, x, y, "OLP");
        svg.append("<line x1=\"").append(x + 24).append("\" y1=\"").append(y + 20)
                .append("\" x2=\"").append(x + 50).append("\" y2=\"").append(y + 40)
                .append("\" stroke=\"#4b8f28\" stroke-width=\"1.5\"/>");
        svg.append("<line x1=\"").append(x + 24).append("\" y1=\"").append(y + 40)
                .append("\" x2=\"").append(x + 50).append("\" y2=\"").append(y + 20)
                .append("\" stroke=\"#4b8f28\" stroke-width=\"1.5\"/>");
    }

    private static void renderBoardLabel(StringBuilder svg, int x, int y, String label) {
        svg.append("<rect x=\"").append(x + 17).append("\" y=\"").append(y + 16)
                .append("\" width=\"40\" height=\"28\" fill=\"#dddddd\" stroke=\"#777\" "
                        + "stroke-width=\"1.4\"/>");
        svg.append("<text x=\"").append(x + 37).append("\" y=\"").append(y + 34)
                .append("\" text-anchor=\"middle\" font-size=\"8\" fill=\"#555\">")
                .append(label).append("</text>");
    }

    private static List<Hop> endpointHops(RouteVariant variant, String site, boolean fromStart) {
        List<Hop> matched = new ArrayList<>();
        if (fromStart) {
            for (Hop hop : variant.hops) {
                if (site.equals(hop.siteId) || site.equals(siteOf(hop.phyNodeId))
                        || site.equals(siteOf(hop.tpId))) {
                    matched.add(hop);
                } else if (!matched.isEmpty()) {
                    break;
                }
            }
            return matched;
        }
        for (int i = variant.hops.size() - 1; i >= 0; i--) {
            Hop hop = variant.hops.get(i);
            if (site.equals(hop.siteId) || site.equals(siteOf(hop.phyNodeId))
                    || site.equals(siteOf(hop.tpId))) {
                matched.add(0, hop);
            } else if (!matched.isEmpty()) {
                break;
            }
        }
        return matched;
    }

    private static List<Hop> firstItems(List<Hop> hops, int count) {
        if (hops.size() <= count) {
            return hops;
        }
        return new ArrayList<>(hops.subList(0, count));
    }

    private static List<Hop> lastItems(List<Hop> hops, int count) {
        if (hops.size() <= count) {
            return hops;
        }
        return new ArrayList<>(hops.subList(hops.size() - count, hops.size()));
    }

    private static List<Hop> orderedBoardHops(List<Hop> hops, boolean leftToRight) {
        Map<Integer, Hop> byRank = new LinkedHashMap<>();
        for (Hop hop : hops) {
            if (hop.linkHop) {
                continue;
            }
            int rank = boardRank(hop);
            if (!byRank.containsKey(rank)) {
                byRank.put(rank, hop);
            }
        }
        List<Map.Entry<Integer, Hop>> entries = new ArrayList<>(byRank.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        List<Hop> ordered = new ArrayList<>();
        for (Map.Entry<Integer, Hop> entry : entries) {
            ordered.add(entry.getValue());
        }
        if (!leftToRight) {
            List<Hop> reversed = new ArrayList<>();
            for (int i = ordered.size() - 1; i >= 0; i--) {
                reversed.add(ordered.get(i));
            }
            return reversed;
        }
        return ordered;
    }

    private static List<Hop> orderedAccessHops(List<Hop> hops, boolean leftToRight) {
        List<Hop> ordered = orderedBoardHops(hops, leftToRight);
        List<Hop> selected = new ArrayList<>();
        for (Hop hop : ordered) {
            int rank = boardRank(hop);
            if (rank <= 50) {
                selected.add(hop);
            }
        }
        return trimHops(selected, 4);
    }

    private static List<Hop> orderedOchHops(List<Hop> hops, boolean leftToRight) {
        List<Hop> ordered = orderedBoardHops(hops, leftToRight);
        List<Hop> selected = new ArrayList<>();
        for (Hop hop : ordered) {
            int rank = boardRank(hop);
            if (rank >= 60 && rank <= 100) {
                selected.add(hop);
            }
        }
        return trimHops(selected, 4);
    }

    private static List<Hop> trimHops(List<Hop> hops, int limit) {
        if (hops.size() <= limit) {
            return hops;
        }
        return new ArrayList<>(hops.subList(0, limit));
    }

    private static int boardRank(Hop hop) {
        String name = normalizedBoardName(hop);
        if (name.matches(".*PORT-.*-C[0-9]*.*")) {
            return 10;
        }
        if (name.contains("RL") && name.contains("-L")) {
            return 20;
        }
        if (name.contains("SIG")) {
            return 30;
        }
        if (name.contains("OLP")) {
            return 40;
        }
        if (name.matches(".*-[0-9]+[ABC].*") || name.matches(".*PORT-.*-[ABC].*")) {
            return 50;
        }
        if (name.contains("M1D1") || name.contains("M33D33")) {
            return 60;
        }
        if (name.contains("IRA") && name.contains("LINE")) {
            return 100;
        }
        if (name.contains("IRA")) {
            return 90;
        }
        if (name.contains("MPO1")) {
            return 80;
        }
        if (name.contains("MUX") || name.contains("PANEL")) {
            return 70;
        }
        if (name.contains("LINE")) {
            return 100;
        }
        return 500;
    }

    private static String normalizedBoardName(Hop hop) {
        return (hop.phyNodeName + " " + hop.tpName + " " + hop.tpId).toUpperCase();
    }

    private static List<Hop> accessHops(List<Hop> hops, boolean fromStart) {
        List<Hop> selected = new ArrayList<>();
        addFirstKind(selected, hops, "terminal", fromStart);
        addFirstKind(selected, hops, "olp", fromStart);
        addFallbackHops(selected, hops, fromStart);
        return selected.size() > 3 ? firstItems(selected, 3) : selected;
    }

    private static List<Hop> pathHops(List<Hop> hops, boolean fromStart) {
        List<Hop> selected = new ArrayList<>();
        addFirstKind(selected, hops, "mux", fromStart);
        addFirstKind(selected, hops, "ira", fromStart);
        addFallbackHops(selected, hops, fromStart);
        return selected.size() > 3 ? firstItems(selected, 3) : selected;
    }

    private static void addFirstKind(List<Hop> selected, List<Hop> hops, String kind,
            boolean fromStart) {
        if (fromStart) {
            for (Hop hop : hops) {
                if (!hop.linkHop && kind.equals(deviceKind(hop)) && !selected.contains(hop)) {
                    selected.add(hop);
                    return;
                }
            }
            return;
        }
        for (int i = hops.size() - 1; i >= 0; i--) {
            Hop hop = hops.get(i);
            if (!hop.linkHop && kind.equals(deviceKind(hop)) && !selected.contains(hop)) {
                selected.add(hop);
                return;
            }
        }
    }

    private static void addFallbackHops(List<Hop> selected, List<Hop> hops, boolean fromStart) {
        if (fromStart) {
            for (Hop hop : hops) {
                if (!selected.contains(hop) && !hop.linkHop) {
                    selected.add(hop);
                }
                if (selected.size() >= 3) {
                    return;
                }
            }
            return;
        }
        for (int i = hops.size() - 1; i >= 0; i--) {
            Hop hop = hops.get(i);
            if (!selected.contains(hop) && !hop.linkHop) {
                selected.add(hop);
            }
            if (selected.size() >= 3) {
                return;
            }
        }
    }

    private static String firstSite(RouteVariant variant) {
        return variant.sites.isEmpty() ? "" : variant.sites.get(0);
    }

    private static String lastSite(RouteVariant variant) {
        return variant.sites.isEmpty() ? "" : variant.sites.get(variant.sites.size() - 1);
    }

    private static String siteLabel(RouteVariant variant, String siteId) {
        for (Hop hop : variant.hops) {
            if (siteId.equals(hop.siteId) && !hop.siteName.isEmpty()) {
                return hop.siteName;
            }
        }
        return siteId;
    }

    private static String routeLineLabel(RouteVariant variant) {
        return variant.label() + "  " + siteLabel(variant, firstSite(variant)) + "-"
                + siteLabel(variant, lastSite(variant)) + "  hops=" + variant.hops.size();
    }

    private static String viaLabel(RouteVariant variant, String leftSite, String rightSite) {
        List<String> via = new ArrayList<>();
        for (String site : variant.sites) {
            if (!site.equals(leftSite) && !site.equals(rightSite)) {
                via.add(siteLabel(variant, site));
            }
        }
        return via.isEmpty() ? "" : "via " + via;
    }

    private static String legend(String name, String color) {
        return "<span class=\"chip\"><span class=\"dot\" style=\"background:" + color
                + "\"></span>" + name + "</span>";
    }

    private static List<RouteVariant> collectVariants(JsonArray routeInfos) {
        List<RouteVariant> variants = new ArrayList<>();
        for (int i = 0; i < routeInfos.size(); i++) {
            JsonObject routeInfo = routeInfos.get(i).getAsJsonObject();
            String routeIndex = stringValue(routeInfo, "index", String.valueOf(i + 1));
            addVariant(variants, routeIndex, "primary", 1, objectValue(routeInfo, "primary"));
            addVariant(variants, routeIndex, "secondary", 1, objectValue(routeInfo, "secondary"));
            JsonArray third = arrayValue(routeInfo, "third");
            if (third != null) {
                for (int j = 0; j < third.size(); j++) {
                    addVariant(variants, routeIndex, "third", j + 1,
                            third.get(j).getAsJsonObject());
                }
            }
        }
        return variants;
    }

    private static void addVariant(List<RouteVariant> variants, String routeIndex, String type,
            int variantIndex, JsonObject payload) {
        if (payload == null) {
            if (!"primary".equals(type)) {
                return;
            }
            variants.add(new RouteVariant(routeIndex, type, variantIndex, new ArrayList<String>(),
                    new ArrayList<Hop>(), new ArrayList<RegResource>(), true));
            return;
        }

        List<String> sites = strings(arrayValue(payload, "site-sequence"));
        List<Hop> hops = new ArrayList<>();
        JsonArray routeSequence = arrayValue(payload, "route-sequence");
        if (routeSequence != null) {
            for (JsonElement element : routeSequence) {
                JsonObject sequence = element.getAsJsonObject();
                hops.add(hopFromRouteSequence(sequence, hops.size() + 1));
            }
        }
        if (hops.isEmpty() && !"primary".equals(type)) {
            return;
        }
        variants.add(new RouteVariant(routeIndex, type, variantIndex, sites, hops,
                collectRegResources(payload), false));
    }

    private static List<RegResource> collectRegResources(JsonObject payload) {
        List<RegResource> resources = new ArrayList<>();
        JsonArray values = arrayValue(payload, "reg-resources");
        if (values == null) {
            return resources;
        }
        for (JsonElement value : values) {
            JsonObject resource = value.getAsJsonObject();
            resources.add(new RegResource(stringValue(resource, "node-id", ""),
                    stringValue(resource, "node-name", ""),
                    stringValue(resource, "site-id", ""),
                    stringValue(resource, "site-name", ""),
                    strings(arrayValue(resource, "l-ports")),
                    strings(arrayValue(resource, "endpoint-tps")),
                    strings(arrayValue(resource, "equipment")),
                    strings(arrayValue(resource, "xcs")),
                    strings(arrayValue(resource, "os-links")),
                    strings(arrayValue(resource, "internal-links"))));
        }
        return resources;
    }

    private static Hop hopFromRouteSequence(JsonObject sequence, int defaultSequence) {
        JsonObject tpHop = objectValue(sequence, "tp-hop");
        if (tpHop == null) {
            return hopFromLinkHop(sequence, defaultSequence);
        }
        JsonObject phyNode = objectValue(tpHop, "phy-node");
        JsonObject physicalNode = objectValue(phyNode, "physical");
        JsonObject siteNode = objectValue(tpHop, "site-node");
        JsonObject site = objectValue(siteNode, "site");
        JsonObject phyTp = objectValue(tpHop, "phy-tp");
        JsonObject physicalTp = objectValue(phyTp, "physical");
        return new Hop(intValue(sequence, "sequence", defaultSequence),
                stringValue(sequence, "topology-ref", ""),
                stringValue(siteNode, "node-id", ""),
                stringValue(site, "friendly-name", ""),
                stringValue(phyNode, "node-id", ""),
                stringValue(physicalNode, "friendly-name", ""),
                stringValue(phyTp, "tp-id", ""),
                stringValue(physicalTp, "friendly-name", ""),
                stringValue(physicalTp, "port-type", ""),
                stringValue(physicalTp, "implement-state", ""),
                false, false);
    }

    private static Hop hopFromLinkHop(JsonObject sequence, int defaultSequence) {
        JsonObject linkHop = objectValue(sequence, "link-hop");
        JsonObject source = objectValue(linkHop, "source");
        JsonObject destination = objectValue(linkHop, "destination");
        JsonObject physical = objectValue(linkHop, "physical");
        String sourceNode = stringValue(source, "source-node", "");
        String sourceTp = stringValue(source, "source-tp", "");
        String destNode = stringValue(destination, "dest-node", "");
        String destTp = stringValue(destination, "dest-tp", "");
        String linkId = stringValue(linkHop, "link-id", "");
        return new Hop(intValue(sequence, "sequence", defaultSequence),
                stringValue(sequence, "topology-ref", ""),
                firstNotEmpty(siteOf(sourceNode), siteOf(sourceTp), siteOf(destNode)),
                "",
                firstNotEmpty(sourceNode, destNode, ""),
                stringValue(physical, "friendly-name-display",
                        stringValue(physical, "friendly-name", "")),
                firstNotEmpty(sourceTp, destTp, linkId),
                firstNotEmpty(portOf(sourceTp), portOf(destTp), "link"),
                stringValue(physical, "link-type", "link-hop"),
                stringValue(physical, "implement-state", ""),
                linkHop == null, true);
    }

    private static List<String> collectSiteOrder(List<RouteVariant> variants) {
        Set<String> sites = new LinkedHashSet<>();
        for (RouteVariant variant : variants) {
            sites.addAll(variant.sites);
            for (Hop hop : variant.hops) {
                addSiteFromRef(sites, hop.siteId);
                addSiteFromRef(sites, hop.phyNodeId);
                addSiteFromRef(sites, hop.tpId);
            }
        }
        return new ArrayList<>(sites);
    }

    private static void addSiteFromRef(Set<String> sites, String value) {
        String site = siteOf(value);
        if (!site.isEmpty()) {
            sites.add(site);
        }
    }

    private static String renderSites(List<String> siteOrder, int rowCount) {
        StringBuilder svg = new StringBuilder();
        int height = Math.max(1, rowCount) * ROW_HEIGHT + 32;
        for (int i = 0; i < siteOrder.size(); i++) {
            int x = siteX(i);
            svg.append("<rect x=\"").append(x).append("\" y=\"72\" width=\"")
                    .append(SITE_WIDTH).append("\" height=\"").append(height)
                    .append("\" rx=\"0\" fill=\"#fbfbfc\" stroke=\"#ef4444\" "
                            + "stroke-width=\"3\"/>");
            svg.append("<text x=\"").append(x + 12).append("\" y=\"")
                    .append(98).append("\" font-size=\"13\" fill=\"#d92d20\">")
                    .append(shortName(siteOrder.get(i))).append("</text>");
        }
        return svg.toString();
    }

    private static String renderVariant(RouteVariant variant, List<String> siteOrder, int rowIndex) {
        StringBuilder svg = new StringBuilder();
        String color = colorOf(variant.type);
        int y = TOP + rowIndex * ROW_HEIGHT;
        svg.append("<text x=\"12\" y=\"").append(y + 29)
                .append("\" font-size=\"12\" fill=\"#344054\">route ")
                .append(escapeXml(variant.routeIndex)).append(" / ")
                .append(escapeXml(variant.label())).append("</text>");
        svg.append("<line x1=\"").append(LEFT).append("\" y1=\"").append(y + 62)
                .append("\" x2=\"").append(siteX(siteOrder.size() - 1) + SITE_WIDTH)
                .append("\" y2=\"").append(y + 62)
                .append("\" stroke=\"#d0d5dd\" stroke-width=\"1\" stroke-dasharray=\"4 6\"/>");

        if (variant.missingPayload) {
            svg.append("<text x=\"").append(LEFT).append("\" y=\"").append(y + 70)
                    .append("\" font-size=\"13\" fill=\"#b42318\">missing ")
                    .append(escapeXml(variant.label())).append("</text>");
            return svg.toString();
        }
        if (variant.hops.isEmpty()) {
            svg.append("<text x=\"").append(LEFT).append("\" y=\"").append(y + 70)
                    .append("\" font-size=\"13\" fill=\"#b42318\">route-sequence empty</text>");
            return svg.toString();
        }

        Point previous = null;
        Map<String, Integer> visibleBySite = new LinkedHashMap<>();
        Map<String, Integer> hiddenBySite = new LinkedHashMap<>();
        for (int i = 0; i < variant.hops.size(); i++) {
            Hop hop = variant.hops.get(i);
            String site = firstNotEmpty(hop.siteId, siteOf(hop.phyNodeId), siteOf(hop.tpId));
            int siteIndex = siteOrder.indexOf(site);
            if (siteIndex < 0) {
                siteIndex = 0;
            }
            int lane = countThenIncrease(visibleBySite, site);
            if (lane >= MAX_HOPS_PER_SITE) {
                countThenIncrease(hiddenBySite, site);
                continue;
            }
            int x = siteX(siteIndex) + 26 + lane * 70;
            int deviceY = y + 32;
            Point current = new Point(x + 26, deviceY + 28);
            if (previous != null) {
                svg.append("<line x1=\"").append(previous.x).append("\" y1=\"")
                        .append(previous.y).append("\" x2=\"").append(current.x)
                        .append("\" y2=\"").append(current.y).append("\" stroke=\"")
                        .append(color).append("\" stroke-width=\"2\" marker-end=\"url(#arrow)\"/>");
            }
            svg.append(renderHop(hop, x, deviceY, color));
            previous = current;
        }
        for (Map.Entry<String, Integer> entry : hiddenBySite.entrySet()) {
            int siteIndex = siteOrder.indexOf(entry.getKey());
            if (siteIndex < 0) {
                siteIndex = 0;
            }
            int x = siteX(siteIndex) + SITE_WIDTH - 54;
            svg.append("<text x=\"").append(x).append("\" y=\"").append(y + 101)
                    .append("\" font-size=\"12\" fill=\"#475467\">+")
                    .append(entry.getValue()).append(" hops</text>");
        }
        return svg.toString();
    }

    private static String renderHop(Hop hop, int x, int y, String color) {
        StringBuilder svg = new StringBuilder();
        svg.append("<g>");
        svg.append("<title>").append(escapeXml(hop.tooltip())).append("</title>");
        svg.append("<rect x=\"").append(x).append("\" y=\"").append(y)
                .append("\" width=\"52\" height=\"56\" fill=\"#f8fafc\" stroke=\"#667085\" "
                        + "stroke-width=\"1.5\"/>");
        svg.append("<circle cx=\"").append(x).append("\" cy=\"").append(y + 28)
                .append("\" r=\"4\" fill=\"").append(color).append("\"/>");
        svg.append("<circle cx=\"").append(x + 52).append("\" cy=\"").append(y + 28)
                .append("\" r=\"4\" fill=\"").append(color).append("\"/>");
        svg.append("<path d=\"M").append(x + 13).append(' ').append(y + 18)
                .append(" L").append(x + 39).append(' ').append(y + 10)
                .append(" L").append(x + 39).append(' ').append(y + 46)
                .append(" L").append(x + 13).append(' ').append(y + 38)
                .append(" Z\" fill=\"#eceff3\" stroke=\"#667085\"/>");
        svg.append("<text x=\"").append(x + 4).append("\" y=\"").append(y + 10)
                .append("\" font-size=\"8\" fill=\"#475467\">#").append(hop.sequence)
                .append("</text>");
        svg.append("<text x=\"").append(x + 26).append("\" y=\"").append(y + 70)
                .append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#475467\">")
                .append(shortName(firstNotEmpty(hop.phyNodeName, neOf(hop.phyNodeId), "NE")))
                .append("</text>");
        svg.append("<text x=\"").append(x + 26).append("\" y=\"").append(y + 82)
                .append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#667085\">")
                .append(shortName(firstNotEmpty(hop.tpName, portOf(hop.tpId), "TP")))
                .append("</text>");
            if (hop.hasMissingGraphData()) {
                svg.append("<text x=\"").append(x + 26).append("\" y=\"").append(y - 4)
                    .append("\" text-anchor=\"middle\" font-size=\"10\" fill=\"#b42318\">!</text>");
            } else if (hop.linkHop) {
                svg.append("<text x=\"").append(x + 26).append("\" y=\"").append(y - 4)
                        .append("\" text-anchor=\"middle\" font-size=\"9\" fill=\"#475467\">link</text>");
            }
        svg.append("</g>");
        return svg.toString();
    }

    private static String renderSummary(List<RouteVariant> variants) {
        StringBuilder html = new StringBuilder();
        html.append("<table class=\"summary\"><thead><tr><th>route</th><th>type</th>"
                + "<th>site-sequence</th><th>route-sequence</th><th>sites in hops</th>"
                + "<th>REG nodes</th><th>REG L ports/XCs/OS/Internal</th>"
                + "<th>check</th></tr></thead><tbody>");
        for (RouteVariant variant : variants) {
            List<String> missing = variant.missingFields();
            html.append("<tr><td>").append(escapeHtml(variant.routeIndex)).append("</td><td>")
                    .append(escapeHtml(variant.label())).append("</td><td>")
                    .append(variant.sites.size()).append("</td><td>")
                    .append(variant.hops.size()).append("</td><td>")
                    .append(variant.siteCountInHops()).append("</td><td>")
                    .append(variant.regResources.size()).append("</td><td>")
                    .append(escapeHtml(variant.regCountSummary())).append("</td><td");
            if (!missing.isEmpty()) {
                html.append(" class=\"warn\"");
            } else {
                html.append(" class=\"ok\"");
            }
            html.append(">").append(escapeHtml(missing.isEmpty() ? "OK" : missing.toString()))
                    .append("</td></tr>");
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private static int countThenIncrease(Map<String, Integer> counter, String key) {
        Integer count = counter.get(key);
        if (count == null) {
            count = 0;
        } else {
            count++;
        }
        counter.put(key, count);
        return count;
    }

    private static int siteX(int index) {
        return LEFT + index * (SITE_WIDTH + SITE_GAP);
    }

    private static String colorOf(String type) {
        if ("primary".equals(type)) {
            return "#d92d20";
        }
        if ("secondary".equals(type)) {
            return "#16a34a";
        }
        return "#2563eb";
    }

    private static JsonObject objectValue(JsonObject object, String key) {
        if (object == null) {
            return null;
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsJsonObject();
    }

    private static JsonArray arrayValue(JsonObject object, String key) {
        if (object == null) {
            return null;
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsJsonArray();
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        if (array == null) {
            return values;
        }
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static String firstTp(JsonObject sequence, String key) {
        JsonArray tps = arrayValue(sequence, key);
        if (tps == null || tps.size() == 0) {
            return "";
        }
        return stringValue(tps.get(0).getAsJsonObject(), "tp-ref", "");
    }

    private static String stringValue(JsonObject object, String key, String defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? defaultValue : value.getAsString();
    }

    private static int intValue(JsonObject object, String key, int defaultValue) {
        if (object == null) {
            return defaultValue;
        }
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? defaultValue : value.getAsInt();
    }

    private static String siteOf(String ref) {
        if (ref == null || !ref.startsWith("Site-")) {
            return "";
        }
        int index = ref.indexOf("#Ne-");
        return index < 0 ? ref : ref.substring(0, index);
    }

    private static String neOf(String ref) {
        if (ref == null) {
            return "";
        }
        int index = ref.indexOf("#Ne-");
        if (index < 0) {
            return ref;
        }
        String remain = ref.substring(index + 1);
        int end = remain.indexOf('#');
        return end < 0 ? remain : remain.substring(0, end);
    }

    private static String portOf(String ref) {
        if (ref == null) {
            return "";
        }
        int index = ref.lastIndexOf("#PORT-");
        return index < 0 ? ref : ref.substring(index + 1);
    }

    private static String firstNotEmpty(String first, String second, String third) {
        if (first != null && !first.isEmpty()) {
            return first;
        }
        if (second != null && !second.isEmpty()) {
            return second;
        }
        return third == null ? "" : third;
    }

    private static String shortName(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value.length() <= 18 ? escapeXml(value) : escapeXml(value.substring(0, 8)
                + "..." + value.substring(value.length() - 6));
    }

    private static String escapeHtml(String value) {
        return escapeXml(value);
    }

    private static String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static final class RouteVariant {
        private final String routeIndex;
        private final String type;
        private final int variantIndex;
        private final List<String> sites;
        private final List<Hop> hops;
        private final List<RegResource> regResources;
        private final boolean missingPayload;

        private RouteVariant(String routeIndex, String type, int variantIndex, List<String> sites,
                List<Hop> hops, List<RegResource> regResources, boolean missingPayload) {
            this.routeIndex = routeIndex;
            this.type = type;
            this.variantIndex = variantIndex;
            this.sites = sites;
            this.hops = hops;
            this.regResources = regResources;
            this.missingPayload = missingPayload;
        }

        private String label() {
            return "third".equals(type) ? type + "-" + variantIndex : type;
        }

        private List<String> missingFields() {
            List<String> missing = new ArrayList<>();
            if (missingPayload) {
                missing.add(type);
                return missing;
            }
            if (sites.isEmpty()) {
                missing.add("site-sequence");
            }
            if (hops.isEmpty()) {
                missing.add("route-sequence");
            }
            for (Hop hop : hops) {
                if (hop.missingTpHop) {
                    missing.add("tp-hop/link-hop#" + hop.sequence);
                }
                if (hop.siteId.isEmpty()) {
                    missing.add("site#" + hop.sequence);
                }
                if (hop.phyNodeId.isEmpty()) {
                    missing.add("node#" + hop.sequence);
                }
                if (hop.tpId.isEmpty()) {
                    missing.add("tp/link-id#" + hop.sequence);
                }
            }
            return missing;
        }

        private int siteCountInHops() {
            Set<String> siteIds = new LinkedHashSet<>();
            for (Hop hop : hops) {
                if (!hop.siteId.isEmpty()) {
                    siteIds.add(hop.siteId);
                }
            }
            return siteIds.size();
        }

        private String regCountSummary() {
            if (regResources.isEmpty()) {
                return "-";
            }
            int ports = 0;
            int xcs = 0;
            int osLinks = 0;
            int internalLinks = 0;
            for (RegResource resource : regResources) {
                ports += resource.lPorts.size();
                xcs += resource.xcs.size();
                osLinks += resource.osLinks.size();
                internalLinks += resource.internalLinks.size();
            }
            return ports + "/" + xcs + "/" + osLinks + "/" + internalLinks;
        }
    }

    private static final class RegResource {
        private final String nodeId;
        private final String nodeName;
        private final String siteId;
        private final String siteName;
        private final List<String> lPorts;
        private final List<String> endpointTps;
        private final List<String> equipment;
        private final List<String> xcs;
        private final List<String> osLinks;
        private final List<String> internalLinks;

        private RegResource(String nodeId, String nodeName, String siteId, String siteName,
                List<String> lPorts, List<String> endpointTps, List<String> equipment,
                List<String> xcs, List<String> osLinks, List<String> internalLinks) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.siteId = siteId;
            this.siteName = siteName;
            this.lPorts = lPorts;
            this.endpointTps = endpointTps;
            this.equipment = equipment;
            this.xcs = xcs;
            this.osLinks = osLinks;
            this.internalLinks = internalLinks;
        }

        private String tooltip() {
            return "REG node=" + nodeId + "\nsite=" + siteId + "\nequipment=" + equipment
                    + "\nL ports=" + lPorts + "\nendpoint TPs=" + endpointTps
                    + "\nself XCs=" + xcs + "\nOS Links=" + osLinks
                    + "\nInternalLinks=" + internalLinks;
        }
    }

    private static final class Board {
        private final String key;
        private final String label;
        private final Hop primary;
        private final List<String> ports = new ArrayList<>();

        private Board(String key, String label, Hop primary) {
            this.key = key;
            this.label = label;
            this.primary = primary;
        }
    }

    private static final class Hop {
        private final int sequence;
        private final String topologyRef;
        private final String siteId;
        private final String siteName;
        private final String phyNodeId;
        private final String phyNodeName;
        private final String tpId;
        private final String tpName;
        private final String portType;
        private final String implementState;
        private final boolean missingTpHop;
        private final boolean linkHop;

        private Hop(int sequence, String topologyRef, String siteId, String siteName,
                String phyNodeId, String phyNodeName, String tpId, String tpName, String portType,
                String implementState, boolean missingTpHop, boolean linkHop) {
            this.sequence = sequence;
            this.topologyRef = topologyRef;
            this.siteId = siteId;
            this.siteName = siteName;
            this.phyNodeId = phyNodeId;
            this.phyNodeName = phyNodeName;
            this.tpId = tpId;
            this.tpName = tpName;
            this.portType = portType;
            this.implementState = implementState;
            this.missingTpHop = missingTpHop;
            this.linkHop = linkHop;
        }

        private boolean hasMissingGraphData() {
            return missingTpHop || siteId.isEmpty() || phyNodeId.isEmpty() || tpId.isEmpty();
        }

        private String tooltip() {
            return "sequence=" + sequence + "\nhop-type=" + (linkHop ? "link-hop" : "tp-hop")
                    + "\nsite-id=" + siteId + "\nsite-name=" + siteName
                    + "\nphy-node-id=" + phyNodeId + "\nphy-node-name=" + phyNodeName
                    + "\ntp-id=" + tpId + "\ntp-name=" + tpName + "\nport-type=" + portType
                    + "\ntopology-ref=" + topologyRef + "\nimplement-state=" + implementState;
        }
    }

    private static final class Point {
        private final int x;
        private final int y;

        private Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
