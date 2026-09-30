/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.sort.query.params.SortInfos;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yangtools.concepts.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @date: 2021/4/9
 */
public class PagedList {

    private static final Logger logger = LoggerFactory.getLogger(PagedList.class);
    private Class<?> listContentClass = null;
    private Class<?> baseClass;

    private List<?> sortedList = null;

    public PagedList(List<?> alist) {
        sortedList = alist;
        if (sortedList.size() > 0) {
            listContentClass = sortedList.get(0).getClass().getInterfaces()[0];
            logger.info("listContentClass: {}", listContentClass.toString());
            if (listContentClass.getSimpleName().equals("Node")) {
                baseClass = org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.NodeBaseInfo.class;
            } else if (listContentClass.getSimpleName().equals("Link")) {
                baseClass = org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LinkBaseInfo.class;
            } else if (listContentClass.getSimpleName().equals("Tunnel")) {
                baseClass = org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelAttributes.class;
            } else if (listContentClass.getSimpleName().equals("Schedule")) {
                baseClass = org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.schedule.rev190626.schedule.list.Schedule.class;
            }
        }
    }

    public void sort(List<SortInfos> sorts) {
        if (sortedList.size() > 1) {
            if (sorts == null || sorts.size() == 0) {
                Collections.sort(sortedList, new SortByIndex());
            } else {
                Collections.sort(sortedList, new OrderBySort(sorts));
            }
        }
    }

    /**
     * which list should be filter
     *
     * @param filter filter string, graphics key: = < > >= <= contain conjunction: and or
     * @return
     * @throws Exception
     */
    public void setFilter(String filter) throws Exception {
        if (filter == null || filter.isEmpty()) {
            return;
        }
        filter = filter.replaceAll(">=", " >= ");
        filter = filter.replaceAll("<=", " <= ");
        filter = filter.replaceAll(">", " > ");
        filter = filter.replaceAll("<", " < ");
        filter = filter.replaceAll("=", " = ");

        String words[] = filter.split("\\s+");

        String situation[] = new String[3];
        Set<Object> baseSet = Sets.newHashSet(sortedList);
        Set<Object> filterOut = Sets.newHashSet(sortedList);

        int count = 0;
        while (count < words.length) {
            if (count + 3 <= words.length) {
                situation[0] = words[count++];
                situation[1] = words[count++];
                situation[2] = words[count++];
                //a condition cause is compose of A = B, thus the length is 3 (0, 1, 2)
                filterOut = filterBy(filterOut, situation);
            }
            if (count < words.length) {
                String joiner = words[count++];
                if (joiner.equalsIgnoreCase("AND")) {
                    //do nothing.
                } else if (joiner.equalsIgnoreCase("OR")) {
                    if (count + 3 <= words.length) {
                        situation[0] = words[count++];
                        situation[1] = words[count++];
                        situation[2] = words[count++];

                        Set<Object> filterOutB = filterBy(baseSet, situation);
                        filterOut.addAll(filterOutB);
                    }
                } else {
                    //the rest words cannot compose one condition cause.
                    break;
                }
            }
        }

        sortedList = Lists.newLinkedList(filterOut);
    }

    private String formatMthName(String name) {
        String retName = "get";
        String tmp[] = name.split("-");
        for (int i = 0; i < tmp.length; i++) {
            String newName = (new StringBuilder()).append(Character.toUpperCase(tmp[i].charAt(0)))
                    .append(tmp[i].substring(1)).toString();
            retName = retName + newName;
        }
        return retName;
    }

    private Class<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider> initClass() {
        //force to declare
        Class<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider> provider = org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider.class;
        return provider;
    }

    private String getObjAttrStrValue(Object obj, String attrName) throws Exception {
        return getObjAttrStrValue(obj, attrName, false);
    }

    private String getObjAttrStrValue(Object obj, String attrName, boolean forFilter)
            throws Exception {
        String index = "";
        String tmp[] = attrName.split("#");
        try {
            Object subObj = obj;
            Class<?> contentClass = subObj.getClass().getInterfaces()[0];
            Class<?> cmpClass = Class.forName(contentClass.getName());
            Set<String> methodName = getMethodNames(cmpClass);
            for (int i = 0; i < tmp.length; i++) {
//                Class<?> contentClass = subObj.getClass().getInterfaces()[0];
//                Class<?> cmpClass = Class.forName(contentClass.getName());
                String mthName = formatMthName(tmp[i]);
                if (!methodName.contains(mthName)) {
                    continue;
                }
                Method cmpMth = cmpClass.getMethod(mthName, null);
                Object tmpObj = (Object) cmpMth.invoke(subObj, null);
                if (tmpObj != null) {
                    Class<?> tmpClazz = tmpObj.getClass();
                    if (tmpClazz.isEnum()) {
                        if (forFilter) {
                            //export enum string value.
                            return tmpObj.toString();
                        } else {
                            //return enum integer value.
                            Method mth = tmpClazz.getMethod("ordinal", null);
                            int a = (int) mth.invoke(tmpObj, null);
                            return String.valueOf(a);
                        }
                    } else if (tmpObj instanceof List) {
                        if (((List) tmpObj).size() != 0 && ((List) tmpObj)
                                .get(0) instanceof Property) {
                            i++;
                        }
                        List<Property> proList = (List<Property>) tmpObj;
                        for (Property att : proList) {
                            if (att.getName().equalsIgnoreCase(tmp[i])) {
                                return att.getValue();
                            }
                        }
                    }
                }
                subObj = tmpObj;
            }
            if (subObj != null) {
                index = subObj.toString();
            } else {
                index = "";
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("data convert error");
        }
        return index;
    }

    private Set<String> getMethodNames(Class<?> cmpClass) {
        Set<String> methodNames = new HashSet<>();
        for (Method method : cmpClass.getMethods()) {
            methodNames.add(method.getName());
        }
        return methodNames;
    }

    private Set<Object> filterBy(Set<Object> baseSet, String[] situation) throws Exception {
        Set<Object> output = new HashSet<>();
        initClass();

        String attrValue = null;
        for (Object obj : baseSet) {
            attrValue = getObjAttrStrValue(obj, situation[0], true);
            if (situation[1].equalsIgnoreCase("CONTAIN")) {
                if (attrValue.toLowerCase().contains(situation[2].toLowerCase())) {
                    output.add(obj);
                }
            } else if (situation[1].equals(">=")) {
                if (attrValue.compareTo(situation[2]) >= 0) {
                    output.add(obj);
                }
            } else if (situation[1].equals("<=")) {
                if (attrValue.compareTo(situation[2]) <= 0) {
                    output.add(obj);
                }
            } else if (situation[1].equals(">")) {
                if (attrValue.compareTo(situation[2]) > 0) {
                    output.add(obj);
                }
            } else if (situation[1].equals("<")) {
                if (attrValue.compareTo(situation[2]) < 0) {
                    output.add(obj);
                }
            } else if (situation[1].equals("=")) {
                if (attrValue.equalsIgnoreCase(situation[2])) {
                    output.add(obj);
                }
            }
        }

        return output;
    }

    public Integer getRecordsNumber() {
        return sortedList.size();
    }

    @SuppressWarnings("rawtypes")
    public List<?> getPage(Integer startPos, Integer howMany) throws Exception {
        List<Object> output = new LinkedList<>();
        Integer counter = 0;
        Integer endPos = howMany == null ? sortedList.size() : startPos + howMany - 1;
        endPos = endPos > sortedList.size() ? sortedList.size() : endPos;
        Iterator<?> iter = sortedList.iterator();
        try {
            while (iter.hasNext()) {
                if (counter > endPos) {
                    break;
                }
                if (counter >= startPos && counter <= endPos) {
                    Object obj = ((Builder) Class.forName(listContentClass.getName() + "Builder")
                            .getDeclaredConstructor(baseClass).newInstance(iter.next())).build();
                    output.add(obj);
                } else {
                    iter.next();
                }
                counter++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("data convert error");
        }
        return output;
    }

    class SortByIndex implements Comparator<Object> {

        @Override
        public int compare(Object arg0, Object arg1) {
            try {
                return getIndex(arg0).compareTo(getIndex(arg1));
            } catch (Exception e) {
                return 1;
            }
        }

        private String getIndex(Object obj) throws Exception {
            String index = "";
            try {
                Class<?> compareKeyClass = Class.forName(listContentClass.getName() + "Key");
                Method getKeyMth = listContentClass.getMethod("getKey", null);
                if (listContentClass.getSimpleName().equals("Node")) {
                    Method cmpMth = compareKeyClass.getMethod("getNodeId", null);
                    NodeId nodeId = (NodeId) cmpMth.invoke(getKeyMth.invoke(obj, null), null);
                    index = nodeId.getValue();
                } else if (listContentClass.getSimpleName().equals("Link")) {
                    Method cmpMth = compareKeyClass.getMethod("getLinkId", null);
                    LinkId linkId = (LinkId) cmpMth.invoke(getKeyMth.invoke(obj, null), null);
                    index = linkId.getValue();
                } else if (listContentClass.getSimpleName().equals("Tunnel")) {
                    Method cmpMth = compareKeyClass.getMethod("getTunnelId", null);
                    Uri tunnelId = (Uri) cmpMth.invoke(getKeyMth.invoke(obj, null), null);
                    index = tunnelId.getValue();
                } else if (listContentClass.getSimpleName().equals("Schedule")) {
                    Method cmpMth = compareKeyClass.getMethod("getId", null);
                    BigInteger id = (BigInteger) cmpMth.invoke(getKeyMth.invoke(obj, null), null);
                    index = id.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("data convert error");
            }
            return index;
        }
    }

    class OrderBySort implements Comparator<Object> {

        List<SortInfos> items;

        public OrderBySort(List<SortInfos> sort) {
            items = sort;
        }

        @Override
        public int compare(Object arg0, Object arg1) {
            try {
                for (SortInfos item : items) {
                    int cmp;
                    if (item.isAscending()) {
                        cmp = getObjAttrStrValue(arg0, item.getSortName())
                                .compareTo(getObjAttrStrValue(arg1, item.getSortName()));
                    } else {
                        cmp = getObjAttrStrValue(arg1, item.getSortName())
                                .compareTo(getObjAttrStrValue(arg0, item.getSortName()));
                    }
                    if (cmp == 0) {
                        //arg0 == arg1, thus continue graphics.
                    } else {
                        return cmp;
                    }
                }
                return 0;

            } catch (Exception e) {
                return 1;
            }
        }

    }
}
