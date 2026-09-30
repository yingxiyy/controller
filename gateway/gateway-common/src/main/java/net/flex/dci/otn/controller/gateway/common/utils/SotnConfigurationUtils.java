/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * @author: xinyzhao
 * @date: 2021/4/7
 */
@Slf4j
public class SotnConfigurationUtils {

    private static final String CONFIGFILE = System.getProperty("user.dir")
            + "/sotn-version.properties";
    private Properties properties = new Properties();

    private static SotnConfigurationUtils INSTANCE = new SotnConfigurationUtils();

    private SotnConfigurationUtils() {
        configure();
    }

    public static SotnConfigurationUtils getInstance() {
        return INSTANCE;
    }


    public void configure() {
        properties.clear();
        InputStream inputStream = null;
        try {
            log.info("Begin to load nms configuration {}", CONFIGFILE);
            inputStream = SotnConfigurationUtils.class.getClassLoader()
                    .getResourceAsStream("sotn-version.properties");
            BufferedReader buff = new BufferedReader(new InputStreamReader(inputStream));

            properties.load(buff);
            inputStream.close();
        } catch (IOException e) {
            log.error("Failed to load nms configuration", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public Object getParamMapValue(String key) {
        return properties.get(key);
    }

    public Properties getParamMap() {
        return properties;
    }

    public String getString(String key) {
        return (String) properties.get(key);
    }

    public boolean getBoolean(String key) {
        boolean ret = false;
        if (!StringUtils.isBlank(key)) {
            try {
                ret = (boolean) properties.get(key);
            } catch (Exception e) {
                log.error("Failed to load key {} with val {}", key, properties.get(key), e);
            }
        }
        return ret;
    }

    public Long getLong(String key) throws Exception {
        if (!StringUtils.isBlank(key)) {
            try {
                return Long.parseLong((String) properties.get(key));
            } catch (Exception e) {
                log.error("Failed to load key {} with val {}", key, properties.get(key), e);
                throw new Exception(String.format("Failed to load key %s with val %s", key,
                        properties.get(key)));
            }
        } else {
            throw new Exception("The key cannot be empty.");
        }
    }

}
