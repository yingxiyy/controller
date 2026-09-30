/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ZooKeeperConfig {

    private static ZooKeeperConfig _instance;

    private Properties properties = new Properties();
    private final static String KEY_ZOOKEEPER_SERVERS = "ZOOKEEPER_SERVERS";
    private final static String KEY_NAME_SPACE = "NAMESPACE";

    private ZooKeeperConfig() {
        init();
    }

    private void init() {
        String rootDir = System.getProperty("user.dir");
        String filename = "zkclient_conf.properties";
        File f = new File(rootDir, "config/" + filename);
        InputStream in = null;
        String fileLocation = null;
        try {
            if (f.exists()) {
                // 读取config目录下的配置文件
                in = new FileInputStream(f);
                fileLocation = f.getAbsolutePath();
            } else {
                // 读取classpath下的配置文件
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                in = classLoader.getResourceAsStream(filename);
                fileLocation = classLoader.getResource(filename).toString();
            }

            log.info("read config file: " + fileLocation);
            properties.clear();
            properties.load(in);
        } catch (Exception e) {
            log.error("fail to fetch zookeeper configuration file. the batch lock will not work.");
        }
    }

    public static ZooKeeperConfig instance() {
        if (_instance == null) {
            _instance = new ZooKeeperConfig();
        }
        return _instance;
    }

    public String getZookeeperServer() {
        String value = getValue(KEY_ZOOKEEPER_SERVERS);
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        return value;
    }

    public String getNameSpace() {
        String value = getValue(KEY_NAME_SPACE);
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        return value;
    }

    public String getValue(String key) {
        if (properties.isEmpty()) {
            log.error("property from config file is empty");
            return null;
        }
        String value = properties.getProperty(key);
        if (value == null) {
            return null;
        }
        value = value.trim();
        return parseEnv(value);
    }

    public static String parseEnv(String str) {
        if (str == null) {
            return "";
        }
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        IEvaluator ie = (groups) -> {
            if (groups == null || groups.length != 2) {
                return "";
            }
            String key = groups[1];
            String value = System.getenv(key);
            if (value == null) {
                return groups[0];
            }
            return value;
        };
        return replace(str, pattern, ie);
    }

    private static String replace(final String src, final Pattern regex, IEvaluator replacer) {
        final Matcher m = regex.matcher(src);
        // 先查询匹配
        List<EvaluateContext> ctxs = new ArrayList<>();
        while (m.find()) {
            // 记录所有匹配的位置 & 匹配结果
            EvaluateContext ctx = new EvaluateContext();
            ctx.startPos = m.start();
            ctx.endPos = m.end();
            String[] groups = new String[m.groupCount() + 1];
            groups[0] = m.group();
            for (int i = 0; i < m.groupCount(); i++) {
                groups[i + 1] = m.group(i + 1);
            }
            ctx.replacement = replacer.evaluate(groups);
            ctxs.add(ctx);
        }
        // 再进行替换
        List<String> parts = new ArrayList<>();
        int sp = 0;
        for (EvaluateContext ctx : ctxs) {
            if (sp < ctx.startPos) {
                parts.add(src.substring(sp, ctx.startPos));
            }
            parts.add(ctx.replacement);
            sp = ctx.endPos;
        }
        // 结尾补齐
        if (sp < src.length()) {
            parts.add(src.substring(sp));
        }
        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            sb.append(s);
        }
        return sb.toString();
    }

    private static class EvaluateContext {

        int startPos;
        int endPos;
        String replacement;
    }

    private static interface IEvaluator {

        String evaluate(String[] groups);
    }
}
