/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.rpc;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.core.BaseNms;
import net.flex.dci.otn.controller.nms.properties.nms.NMSOperation;
import net.flex.dci.otn.controller.nms.properties.nms.NMSOperationsHandlersMap;

/**
 * @date: 2021/4/7
 */
@Slf4j
public class NMSOperationsHandler {

    private final NMSOperationsHandlersMap nmsOperationsHandlersMap;


    private final ConcurrentHashMap<String, IRpc> rpcBeanMap;

    private final ConcurrentHashMap<String, IRpc> nmsMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, IRpc> initializerHandlerMap = new ConcurrentHashMap<>();

//    private final String PACKAGE_PREFIX = "net.flex.dci.otn.controller.nms.nms.core";


    public NMSOperationsHandler(
            NMSOperationsHandlersMap nmsOperationsHandlersMap, List<BaseNms> nmsList
    ) {
        this.nmsOperationsHandlersMap = nmsOperationsHandlersMap;
        this.rpcBeanMap = loadRpcBeanMap(nmsList);
    }

    /**
     * init the nms rpc map
     *
     * @param nmsList
     * @return
     */
    private ConcurrentHashMap<String, IRpc> loadRpcBeanMap(List<BaseNms> nmsList) {
        return nmsList.stream().collect(ConcurrentHashMap::new,
                (map, nmsRpc) -> map.put(nmsRpc.getClass().getSimpleName(), nmsRpc),
                ConcurrentHashMap::putAll);
    }

    /**
     * load nms rpc map
     */
    private void loadNmsMap() {
        List<NMSOperation> nmsOperations = this.nmsOperationsHandlersMap.getNms();
        log.debug("start to init the nms rpc cmd map");
        nmsOperations.stream().forEach(nmsOperation -> {
            String nmsCmd = nmsOperation.getOperation();
            String handlerClassName = nmsOperation.getHandler();
            IRpc nmsHandler = rpcBeanMap.get(handlerClassName);
            nmsMap.put(nmsCmd, nmsHandler);
        });
    }

    /**
     * INIT HANDLER
     *
     * @param handlerClassName
     * @return
     */
//    private IRpc initNMSOperationHandler(String handlerClassName)
//            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
//        ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
//        Class<?> clazz = classLoader.loadClass(PACKAGE_PREFIX + "." + handlerClassName);
//        Constructor<?> constructor = clazz
//                .getConstructor(NetconfTopology.class);
//        IRpc nmsRpc = (IRpc) constructor
//                .newInstance(netconfTopology);
//        if (!initializerHandlerMap.containsKey(handlerClassName)) {
//            initializerHandlerMap.put(handlerClassName, nmsRpc);
//        }
//        return nmsRpc;
//    }

    /**
     * get handler by cmd
     *
     * @param cmd
     * @return
     */
    public IRpc getHandlerByCMD(String cmd) {
        return nmsMap.get(cmd);
    }

    public boolean containsKey(String nmsOperation) {
        return nmsMap.containsKey(nmsOperation);
    }
}
