package net.flex.dci.otn.controller.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class CommonUtils {

    public static <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        int size = list.size();
        for (int i = 0; i < size; i += batchSize) {
            partitions.add(list.subList(i, Math.min(size, i + batchSize)));
        }
        return partitions;
    }

}
