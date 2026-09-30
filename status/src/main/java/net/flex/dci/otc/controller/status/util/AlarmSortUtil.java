/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmAttributeNameType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.sort.query.params.SortInfos;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.util.CollectionUtils;

public class AlarmSortUtil {

    public static Sort getSort(List<SortInfos> sortList) {
        List<Map<String, String>> sortMapList = new ArrayList<>();
        if (!CollectionUtils.isEmpty(sortList)) {
            for (SortInfos sort : sortList) {
                Map<String, String> map = new HashMap<>();
                String attr = null;
                if (sort.getSortName() == AlarmAttributeNameType.AlarmId) {
                    attr = "index";
                } else if (sort.getSortName() == AlarmAttributeNameType.Serverity) {
                    attr = "severity";
                } else if (sort.getSortName() == AlarmAttributeNameType.ResourceRef) {
                    attr = "resourceRef";
                } else if (sort.getSortName() == AlarmAttributeNameType.AlarmText) {
                    attr = "alarmText";
                } else if (sort.getSortName() == AlarmAttributeNameType.AlarmGroup) {
                    attr = "alarmGroup";
                } else if (sort.getSortName() == AlarmAttributeNameType.AlarmTypeId) {
                    attr = "alarmTypeId";
                } else if (sort.getSortName() == AlarmAttributeNameType.CreationTime) {
                    attr = "creationTime";
                } else if (sort.getSortName() == AlarmAttributeNameType.CreationReceivedTime) {
                    attr = "nmlReceivedTime";
                } else if (sort.getSortName() == AlarmAttributeNameType.ClearTime) {
                    attr = "clearedTime";
                } else if (sort.getSortName() == AlarmAttributeNameType.ClearReceivedTime) {
                    attr = "nmlReceivedTime";
                } else if (sort.getSortName() == AlarmAttributeNameType.ArchiveTime) {
                    attr = "archivedTime";
                } else if (sort.getSortName() == AlarmAttributeNameType.ArchiveType) {
                    attr = "actionType";
                } else if (sort.getSortName() == AlarmAttributeNameType.NeId) {
                    attr = "neId";
                } else if (sort.getSortName() == AlarmAttributeNameType.NmlKey) {
                    attr = "nmlKey";
                } else if (sort.getSortName() == AlarmAttributeNameType.Sa) {
                    attr = "sa";
                } else if (sort.getSortName() == AlarmAttributeNameType.EquipmentRef) {
                    attr = "componentRef";
                } else if (sort.getSortName() == AlarmAttributeNameType.NmlKeyName) {
                    attr = "nmlKeyName";
                }
                map.put("attribute", attr);
                map.put("ascending", sort.isAscending() ? "asc" : "desc");
                sortMapList.add(map);
            }
        }
        return getSortFromMaps(sortMapList);
    }

    public static Sort getSortFromMaps(List<Map<String, String>> sortMaps) {
        if (sortMaps == null || sortMaps.isEmpty()) {
            return Sort.unsorted();
        }

        return sortMaps.stream()
                .map(map -> {
                    String attribute = map.get("attribute");
                    Direction order = map.get("ascending").equals("asc") ? Sort.Direction.ASC
                            : Sort.Direction.DESC;
                    return Sort.by(order, attribute);
                })
                .reduce(Sort.by("index"), Sort::and);
    }
}
