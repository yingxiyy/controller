/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.properties;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;


/**
 * @author: xinyzhao
 * @date: 2021/3/24
 */
@Data
@Slf4j
public class RpcModulePathMap implements Serializable {

    private String desc;

    private List<Rpc> rpcs;

    private final ConcurrentHashMap<String, List<String>> rpcMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("initializing the rpc opName map ");
        for (Rpc rpc : rpcs) {
            rpcMap.put(rpc.getModule(), rpc.getPaths());
        }
    }

    /**
     * get module name from opName
     *
     * @param path
     * @return
     */
    public String getModuleFromPath(String path) {
        log.debug("get relative module from the opName {}", path);
        path = path.replace("/", "");
        Set set = rpcMap.entrySet();
        Iterator<Entry<String, List<String>>> iterator = set.iterator();
        String module = null;
        while (iterator.hasNext()) {
            Entry<String, List<String>> entry = iterator.next();
            List<String> paths = entry.getValue();
            if (paths.contains(path)) {
                module = entry.getKey();
            }
        }
        return module;

    }

}
