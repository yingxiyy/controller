package net.flex.dci.otn.controller.allocate.designer.reallocate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;

public interface ReallocateInterface<T> {

    default Set<String> getMatchedEquipIds(Map<String, String> reallocateEquipMap, String linkId) {
        Set<String> oldEquipIds = reallocateEquipMap.keySet();

        Set<String> matchedEquipIds = new HashSet<>();
        for (String equipId : oldEquipIds) {
            if (linkId.contains(equipId)) {
                matchedEquipIds.add(equipId);
            }
        }
        return matchedEquipIds;
    }

    /**
     * Output: <oldId,newItem>, must keep order as list.
     *
     * @param items
     * @param reallocateEquipMap
     * @return
     * @throws NeDesignerException
     */
    default LinkedHashMap<String, T> reallocate(List<T> items, Map<String, String> reallocateEquipMap) throws NeDesignerException {
        if (items == null) {
            return null;
        }

        LinkedHashMap result = new LinkedHashMap();
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            String id = getId(item);
            Set<String> matchedEquipIds = getMatchedEquipIds(reallocateEquipMap, id);
            if (matchedEquipIds == null || matchedEquipIds.isEmpty()) {
                result.put(id, item);
                continue;
            }
            T newItem = reallocate(item, matchedEquipIds, reallocateEquipMap);
            result.put(id, newItem);
        }
        return result;
    }

    default List<T> reallocateItemsInNode(String nodeId, List<T> oldNodeItems, HashMap<String, T> newItems) {
        List<T> newNodeItems = new ArrayList<>();
        for (T oldItem : oldNodeItems) {
            String oldId = getId(oldItem);
            T newItem = newItems.get(oldId);
            if (newItem == null) { // 此Item没有被reallocate
                newNodeItems.add(oldItem);
                continue;
            }
            if (getId(newItem).contains(nodeId)) {//新的item(xc/internalLink)，仍然在这个node上
                newNodeItems.add(newItem);
            }
        }

        return newNodeItems;
    }

    T reallocate(T item, Set<String> matchedEquipIds, Map<String, String> reallocateEquipMap) throws NeDesignerException;

    String getId(T item);

}
