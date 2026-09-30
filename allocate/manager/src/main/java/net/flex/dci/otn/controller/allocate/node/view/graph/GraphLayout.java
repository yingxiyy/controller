package net.flex.dci.otn.controller.allocate.node.view.graph;

import org.apache.commons.lang3.tuple.ImmutablePair;
import java.util.*;

public class GraphLayout {

    private static class Node {
        double x;
        double y;
        double dx;
        double dy;
    }

    public Map<String, ImmutablePair<Integer, Integer>> layout(
            List<String> nodes,
            List<ImmutablePair<String, String>> links,
            int minX,
            int minY,
            int maxX,
            int maxY) {

        if (links == null || links.isEmpty()) {
            return gridLayout(nodes, minX, minY, maxX, maxY);
        }

        return forceLayout(nodes, links, minX, minY, maxX, maxY);
    }

    // ----------------------------------------------------
    // GRID LAYOUT
    // ----------------------------------------------------

    private Map<String, ImmutablePair<Integer, Integer>> gridLayout(
            List<String> nodes,
            int minX,
            int minY,
            int maxX,
            int maxY) {

        Map<String, ImmutablePair<Integer, Integer>> result = new HashMap<>();
        if (nodes == null || nodes.isEmpty()) {
            return result;
        }

        int n = nodes.size();

        int cols = (int) Math.ceil(Math.sqrt(n));
        int rows = (int) Math.ceil((double) n / cols);

        double width = maxX - minX;
        double height = maxY - minY;

        double cellW = width / cols;
        double cellH = height / rows;

        for (int i = 0; i < n; i++) {

            int r = i / cols;
            int c = i % cols;

            int x = (int) (minX + c * cellW + cellW / 2);
            int y = (int) (minY + r * cellH + cellH / 2);

            result.put(nodes.get(i), new ImmutablePair<>(x, y));
        }

        return result;
    }

    // ----------------------------------------------------
    // FORCE DIRECTED LAYOUT
    // ----------------------------------------------------

    private static Map<String, ImmutablePair<Integer, Integer>> forceLayout(
            List<String> nodeIds,
            List<ImmutablePair<String, String>> links,
            int minX,
            int minY,
            int maxX,
            int maxY) {

        int width = maxX - minX;
        int height = maxY - minY;

        int n = nodeIds.size();
        if (n == 0) {
            return new HashMap<>();
        }

        Map<String, Node> nodes = new HashMap<>();

        Random rand = new Random(42);

        int cols = (int) Math.ceil(Math.sqrt(n));
        double cell = Math.min(width, height) / cols;

        for (int i = 0; i < n; i++) {

            int r = i / cols;
            int c = i % cols;

            Node node = new Node();

            node.x = minX + c * cell + rand.nextDouble() * cell;
            node.y = minY + r * cell + rand.nextDouble() * cell;

            nodes.put(nodeIds.get(i), node);
        }

        double area = width * height;
        double k = Math.sqrt(area / n);

        int iterations = 300;
        double temperature = width / 10.0;

        double nodeRadius = Math.max(10, Math.min(width, height) / 50.0);

        for (int iter = 0; iter < iterations; iter++) {

            for (Node v : nodes.values()) {
                v.dx = 0;
                v.dy = 0;
            }

            // REPULSION
            for (String vId : nodeIds) {

                Node v = nodes.get(vId);

                for (String uId : nodeIds) {

                    if (vId.equals(uId)) continue;

                    Node u = nodes.get(uId);

                    double dx = v.x - u.x;
                    double dy = v.y - u.y;

                    double dist = Math.sqrt(dx * dx + dy * dy) + 0.01;

                    double force = (k * k) / dist;

                    v.dx += dx / dist * force;
                    v.dy += dy / dist * force;
                }
            }

            // ATTRACTION
            for (ImmutablePair<String, String> link : links) {

                Node v = nodes.get(link.left);
                Node u = nodes.get(link.right);

                if (v == null || u == null) continue;

                double dx = v.x - u.x;
                double dy = v.y - u.y;

                double dist = Math.sqrt(dx * dx + dy * dy) + 0.01;

                double force = (dist * dist) / k;

                double fx = dx / dist * force;
                double fy = dy / dist * force;

                v.dx -= fx;
                v.dy -= fy;

                u.dx += fx;
                u.dy += fy;
            }

            // MOVE
            for (Node v : nodes.values()) {

                double disp = Math.sqrt(v.dx * v.dx + v.dy * v.dy);

                if (disp > 0) {

                    double limited = Math.min(disp, temperature);

                    v.x += (v.dx / disp) * limited;
                    v.y += (v.dy / disp) * limited;
                }

                v.x = Math.max(minX + nodeRadius, Math.min(maxX - nodeRadius, v.x));
                v.y = Math.max(minY + nodeRadius, Math.min(maxY - nodeRadius, v.y));
            }

            temperature *= 0.95;
        }

        Map<String, ImmutablePair<Integer, Integer>> result = new HashMap<>();

        for (String id : nodeIds) {

            Node node = nodes.get(id);

            result.put(id, new ImmutablePair<>(
                    (int) Math.round(node.x),
                    (int) Math.round(node.y)
            ));
        }

        return result;
    }
}
