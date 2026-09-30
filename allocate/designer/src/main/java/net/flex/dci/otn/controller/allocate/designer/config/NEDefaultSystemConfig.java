/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.ne.LoginInfo;
import net.flex.dci.otn.controller.allocate.ne.OpenConfigNe;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.System;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.SystemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.NtpBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.NtpKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.RadiusBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.RadiusKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.SyslogBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.SyslogKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NEDefaultSystemConfig {

    private static final String CONFIG_FILE = "OpenConfigNe.json";
    private OpenConfigNe setting;
    private System system;
    private Map<String, LoginInfo> loginInfoMap = new HashMap<>();

    @PostConstruct
    public void load() {
        log.debug("start load NEDefaultSystemConfig.");
        try {
            InputStream configFile = new ClassPathResource(CONFIG_FILE).getInputStream();
//            File configFile = new ClassPathResource(CONFIG_FILE).getFile();
            ObjectMapper mapper = new ObjectMapper();
            setting = mapper.readValue(configFile, OpenConfigNe.class);
        } catch (Exception e) {
            log.error("default setting hasn't found or content is an invalid JSON {}", e);
        }
        system = new SystemBuilder()
                .setNtp(setting.getNtp().stream().map(item -> {
                    try {
                        String ip = item.getIp();
                        return new NtpBuilder().setIp(ip).setKey(new NtpKey(ip)).build();
                    } catch (IllegalArgumentException e) {
                        log.error("Get invalid ntp IP for {}, return null.", item, e);
                        return null;
                    }
                }).collect(Collectors.toList()))
                .setRadius(setting.getRadius().stream().map(item -> {
                    try {
                        String ip = item.getIp();
                        return new RadiusBuilder().setIp(ip).setKey(new RadiusKey(ip))
                                .build();
                    } catch (IllegalArgumentException e) {
                        log.error("Get invalid radius IP for {}, return null.", item, e);
                        return new RadiusBuilder().build();
                    }

                }).collect(Collectors.toList()))
                .setSyslog(setting.getSyslog().stream().map(item -> {
                    try {
                        String ip = item.getIp();
                        return new SyslogBuilder().setIp(ip).setKey(new SyslogKey(ip))
                                .build();
                    } catch (IllegalArgumentException e) {
                        log.error("Get invalid syslog IP for {}, return null.", item, e);
                        return new SyslogBuilder().build();
                    }

                }).collect(Collectors.toList()))
                .setProperties(new PropertiesBuilder().setProperty(Arrays.asList(
                        new PropertyBuilder().setName("timezone")
                                .setValue(setting.getTimezone())
                                .build())).build()).build();

        loginInfoMap = setting.getLoginInfo().stream().collect(Collectors.toMap(LoginInfo::getVendor, Function.identity()));
    }

    public System getSystemConfig() {
        return this.system;
    }

    public LoginInfo getLoginInfo(String vendor) {
        return loginInfoMap.get(vendor);
    }
}
