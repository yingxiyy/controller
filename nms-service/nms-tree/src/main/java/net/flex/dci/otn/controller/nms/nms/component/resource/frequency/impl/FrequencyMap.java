package net.flex.dci.otn.controller.nms.nms.component.resource.frequency.impl;

import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.Scope;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.Map;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.MapBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.MapKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

@Slf4j
public class FrequencyMap {

    public static List<Map> constructSiteLinksRefFrequencyMap(List<Long> avaFrequencies,
            List<Link> ochLists, List<Available> initialList, GridType grid) {
        log.debug("start to construct frequency map");

        List<Map> resultMapList = new ArrayList<>();
        long step = getStepWithSpecGrid(grid);

        // Step 1: 基于 OCH 构建所有 scope（已占用）
        java.util.Map<ImplementState, List<FrequencyScope>> scopeByState = new HashMap<>();
        scopeByState.put(ImplementState.Implement, new ArrayList<>());
        scopeByState.put(ImplementState.Allocate, new ArrayList<>());

        for (Link och : ochLists) {
            Long centerFreq = getCenterFrequencyFromOchLink(och);
            if (centerFreq == null) {
                continue;
            }

            ImplementState state = getImplementState(och); // "implemented" or "allocated"
            FrequencyScope scope = FrequencyScope.builder()
                    .centre(centerFreq.intValue())
                    .lower((int) (centerFreq - step / 2))
                    .upper((int) (centerFreq + step / 2))
                    .index(0) // 后面赋值
                    .implementState(state)
                    .ochLinkId(och.getLinkId().getValue())
                    .ochLinkFriendlyName(
                            och.getAugmentation(Link1.class).getOch().getFriendlyName())
                    .build();

            if (scopeByState.containsKey(state)) {
                scopeByState.get(state).add(scope);
            }
        }

        // Step 2: 构建 free scopes（Plan 状态，排除与 OCH 使用频段重叠的）
        List<FrequencyScope> allOccupied = new ArrayList<>();
        allOccupied.addAll(scopeByState.get(ImplementState.Implement));
        allOccupied.addAll(scopeByState.get(ImplementState.Allocate));

        List<FrequencyScope> freeScopes = new ArrayList<>();
        for (Long freq : avaFrequencies) {
            int centre = freq.intValue();
            int lower = (int) (freq - step / 2);
            int upper = (int) (freq + step / 2);

            boolean overlaps = false;
            for (FrequencyScope used : allOccupied) {
                if (upper > used.getLower() && lower < used.getUpper()) {
                    overlaps = true;
                    break;
                }
            }
            if (overlaps) continue;

            FrequencyScope scope = FrequencyScope.builder()
                    .centre(centre)
                    .lower(lower)
                    .upper(upper)
                    .index(0)
                    .implementState(ImplementState.Plan)
                    .build();

            freeScopes.add(scope);
        }

        // Step 3: Merge plan and used
        resultMapList.addAll(mergeScopesToMaps(freeScopes, step, ImplementState.Plan, allOccupied));

//        resultMapList.addAll(mergeScopesToMaps(freeScopes, step, ImplementState.Plan));

        // Step 4: 最终 mapList 按频率排序 & 裁剪在 initialList 范围内
        resultMapList = resultMapList.stream()
                .filter(map -> isInInitialRange(map.getStart(), map.getEnd(), initialList))
                .sorted(Comparator.comparingInt(Map::getStart))
                .collect(Collectors.toList());

        return resultMapList;
    }

    private static List<Map> mergeScopesToMaps(
            List<FrequencyScope> freeScopes,
            long step,
            ImplementState state,
            List<FrequencyScope> usedScopes // implement & allocate
    ) {
        List<Map> mapList = new ArrayList<>();
        if (freeScopes == null || freeScopes.isEmpty()) return mapList;

        List<FrequencyScope> wholeScope = new ArrayList<>(freeScopes);
        wholeScope.addAll(usedScopes);

        wholeScope.sort(Comparator.comparingInt(FrequencyScope::getLower).reversed());
        List<FrequencyScope> buffer = new ArrayList<>();
        int index = 1;

        for (FrequencyScope current : wholeScope) {
            if (state == ImplementState.Plan && hasOverlap(current, usedScopes)) {
                // 不能合并该 scope，单独输出
                if (!buffer.isEmpty()) {
                    mapList.add(buildMapFromScopeBuffer(buffer, state));
                    buffer.clear();
                }
                current.setIndex(index++);
                mapList.add(buildMapFromScopeBuffer(Collections.singletonList(current), state));
                continue;
            }

            if (buffer.isEmpty()) {
                current.setIndex(index++);
                buffer.add(current);
            } else {
                FrequencyScope last = buffer.get(buffer.size() - 1);
                if (scopesOverlap(last, current)) {
                    current.setIndex(index++);
                    buffer.add(current);
                } else {
                    mapList.add(buildMapFromScopeBuffer(buffer, state));
                    buffer = new ArrayList<>();
                    current.setIndex(index++);
                    buffer.add(current);
                }
            }
        }

        if (!buffer.isEmpty()) {
            mapList.add(buildMapFromScopeBuffer(buffer, state));
        }

        return mapList;
    }

    private static boolean scopesOverlap(FrequencyScope a, FrequencyScope b) {
        return a.getLower() < b.getUpper() && b.getLower() < a.getUpper();
    }

    private static boolean hasOverlap(FrequencyScope scope, List<FrequencyScope> existingScopes) {
        for (FrequencyScope existing : existingScopes) {
            if (scope.getUpper() > existing.getLower() && scope.getLower() < existing.getUpper()) {
                return true;
            }
        }
        return false;
    }

    private static List<Map> mergeScopesToMaps_old(List<FrequencyScope> scopes, long step,
            ImplementState state) {
        List<Map> mapList = new ArrayList<>();
        if (scopes == null || scopes.isEmpty()) {
            return mapList;
        }

        scopes.sort(Comparator.comparingInt(FrequencyScope::getCentre));
        List<FrequencyScope> buffer = new ArrayList<>();
        FrequencyScope prev = null;
        int index = 1;

        for (FrequencyScope scope : scopes) {
            if (prev == null) {
                // 开始新buffer
                buffer.add(scope);
                scope.setIndex(index++);
            } else {
                int gap = scope.getCentre() - prev.getCentre();
                if (gap == step) {
                    buffer.add(scope);
                    scope.setIndex(index++);
                } else {
                    // 不连续了，flush当前buffer
                    mapList.add(buildMapFromScopeBuffer(buffer, state));
                    buffer = new ArrayList<>();
                    index = 1;
                    scope.setIndex(index++);
                    buffer.add(scope);
                }
            }
            prev = scope;
        }

        if (!buffer.isEmpty()) {
            mapList.add(buildMapFromScopeBuffer(buffer, state));
        }

        return mapList;
    }

    private static Map buildMapFromScopeBuffer(List<FrequencyScope> scopes, ImplementState state) {
        Map map = new MapBuilder()
                .setStart(scopes.get(0).getLower())
                .setEnd(scopes.get(scopes.size() - 1).getUpper())
                .setState(state)
                .setKey(new MapKey(scopes.get(0).getLower()))
                .setScope(scopes.stream().map(x -> convertToScope(x))
                        .collect(Collectors.toList()))  // 已经设置好 index
                .build();
        return map;
    }

    private static Scope convertToScope(FrequencyScope x) {
        Scope scope = new ScopeBuilder()
                .setIndex(x.getIndex())
                .setCentre(x.getCentre())
                .setImplementState(x.getImplementState())
                .setLower(x.getLower())
                .setUpper(x.getUpper())
                .setOchLinkId(x.getOchLinkId())
                .setOchLinkFriendlyName(x.getOchLinkFriendlyName())
                .setKey(new ScopeKey(x.getCentre()))
                .build();
        return scope;
    }

//    private static boolean isInInitialRange(int start, int end, List<Available> initialList) {
//        for (Available a : initialList) {
//            int bandLow = a.getLowerFrequency().getValue().intValue();
//            int bandHigh = a.getUpperFrequency().getValue().intValue();
//            if (start >= bandLow && end <= bandHigh) {
//                return true;
//            }
//        }
//        return false;
//    }
    private static boolean isInInitialRange(int start, int end, List<Available> initialList) {
        for (Available a : initialList) {
            int bandLow = a.getLowerFrequency().getValue().intValue();
            int bandHigh = a.getUpperFrequency().getValue().intValue();
            if (start < bandHigh && end > bandLow) {
                return true; // 只要有重叠就保留
            }
        }
        return false;
    }

    private static long getStepWithSpecGrid(GridType gridType) {
        switch (gridType) {
            case _50:
                return 50000;
            case _75:
                return 75000;
            case _100:
                return 100000;
            case _150:
                return 150000;
            default:
                return 6250;
        }
    }

    private static Long getCenterFrequencyFromOchLink(Link ochLink) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        long lower = ochAttr.getLowerFrequency().getValue().longValue();
        long upper = ochAttr.getUpperFrequency().getValue().longValue();
        return lower + (upper - lower) / 2;
    }


    private static ImplementState getImplementState(Link ochLink) {
        Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
        return ochAttr.getImplementState().equals(ImplementState.Allocate) ? ImplementState.Allocate
                : ImplementState.Implement;
    }
}

