/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PortNameComparator implements Comparator<String> {
    @Override
    public int compare(String a1, String a2) {
        ParsedName p1 = parse(a1);
        ParsedName p2 = parse(a2);

        int cmpGroup = Integer.compare(p1.shelf, p2.shelf);
        if (cmpGroup != 0) return cmpGroup;

        int cmpSubgroup = Integer.compare(p1.slot, p2.slot);
        if (cmpSubgroup != 0) return cmpSubgroup;

        return compareSuffixNaturally(p1.suffixParts, p2.suffixParts);
    }

    private int compareSuffixNaturally(List<Object> s1, List<Object> s2) {
        int len = Math.min(s1.size(), s2.size());
        for (int i = 0; i < len; i++) {
            Object o1 = s1.get(i);
            Object o2 = s2.get(i);

            if (o1 instanceof Integer && o2 instanceof Integer) {
                int cmp = Integer.compare((Integer) o1, (Integer) o2);
                if (cmp != 0) return cmp;
            } else {
                int cmp = o1.toString().compareTo(o2.toString());
                if (cmp != 0) return cmp;
            }
        }
        return Integer.compare(s1.size(), s2.size());
    }
    private  ParsedName parse(String name) {
        String[] parts = name.split("-");
        int group = parts.length > 1 ? parseIntSafe(parts[1]) : 0;
        int subgroup = parts.length > 2 ? parseIntSafe(parts[2]) : 0;
        String suffix = parts.length > 3 ? parts[3] : "";

        List<Object> suffixParts = splitAlphaNumeric(suffix);
        return new ParsedName(group, subgroup, suffixParts);
    }
    private  int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private  List<Object> splitAlphaNumeric(String input) {
        List<Object> parts = new ArrayList<>();
        Matcher m = Pattern.compile("(\\D+)|(\\d+)").matcher(input);
        while (m.find()) {
            if (m.group(1) != null) {
                parts.add(m.group(1));
            } else {
                parts.add(Integer.parseInt(m.group(2)));
            }
        }
        return parts;
    }

    private class ParsedName {
        int shelf;
        int slot;
        List<Object> suffixParts;

        private ParsedName(int shelf, int slot, List<Object> suffixParts) {
            this.shelf = shelf;
            this.slot = slot;
            this.suffixParts = suffixParts;
        }

    }
}
