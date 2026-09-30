/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;

import com.alibaba.fastjson.JSON;
import net.flex.dci.otn.controller.gateway.common.properties.OpenNeDefaultSetting;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @author: xinyzhao
 * @date: 2021/4/12
 */
@Slf4j
public class OpenNeSettingUtils {

    private static final String CONFIG_FILE = System.getProperty("user.dir")
            + "/OpenConfigNe.properties";
    private static OpenNeDefaultSetting openNeDefaultSetting;

    private static OpenNeSettingUtils INSTANCE;

    static {
        try {
            INSTANCE = new OpenNeSettingUtils();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private OpenNeSettingUtils() throws IOException {
        loadSetting();
    }

    private void loadSetting() throws IOException {
        log.debug("start to load default setting");
        try {
            Resource resource = new ClassPathResource(CONFIG_FILE);
            InputStream inputStream = resource.getInputStream();
            openNeDefaultSetting = JSON.parseObject(inputStream, OpenNeDefaultSetting.class);
        } catch (IOException ex) {
            log.error("default setting hasn't found or content is an invalid JSON {}",
                    ex.getMessage());
        }
    }

    public static OpenNeSettingUtils getINSTANCE() {
        return INSTANCE;
    }


}
