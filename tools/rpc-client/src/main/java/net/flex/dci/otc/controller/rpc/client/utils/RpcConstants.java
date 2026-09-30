/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.utils;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/15 21:09
 */
public class RpcConstants {

    public final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.105 Safari/537.36";

    public final static String RPC_PREFIX = "/restconf/operations/";

    public final static String TOPOLOGY_PREFIX = "/restconf/operational/";

    public final static String POUND_SIGN = "#";

    public final static String URL_POUND = "%23";

    public final static String HTTP_PREFIX = "http://";

    public final static String COLON = ":";

    public static final Long EXECUTE_CONNECTION_TIMEOUT = 60000L;

    public static final Long EXECUTE_READ_TIMEOUT = 60000L;

    public static final Long CONNECT_NE_CONNECTION_TIMEOUT = 30000L;

    public static final Long CONNECT_NE_READ_TIMEOUT = 30000L;

    public static final Long UNREGISTER_NE_CONNECTION_TIMEOUT = 30000L;

    public static final Long UNREGISTER_NE_READ_TIMEOUT = 30000L;

    public static final Long SYNC_DATA_CONNECTION_TIMEOUT = 60000L;

    public static final Long SYNC_DATA_READ_TIMEOUT = 180000L;

    public static final Long COMMON_CONNECTION_TIMEOUT = 20000L;

    public static final Long COMMON_READ_TIMEOUT = 60000L;

    public static final Long MERGE_DATA_CONNECTION_TIMEOUT = 60000L;

    public static final Long MERGE_DATA_READ_TIMEOUT = 300000L;

    public static final Long CONFIG_NE_CONNECTION_TIMEOUT = 60000L;

    public static final Long CONFIG_NE_READ_TIMEOUT = 300000L;
}
