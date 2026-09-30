/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.common.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @version 1.0
 * @date 2021/12/9 13:32
 */
@Data
@AllArgsConstructor
public class ToopAlarm implements Serializable {

    public ToopAlarm() {
        
    }

    @JSONField(name = "ip")
    private String ip;

    @JSONField(name = "toop-key")
    private String toopKey;

    @JSONField(name = "toop-ip")
    private String toopIp;

    @JSONField(name = "id")
    private String id;

    @JSONField(name = "is-cleared")
    private Boolean isClear;

    @JSONField(name = "resource")
    private String resource;

    @JSONField(name = "group")
    private String group;

    @JSONField(name = "type-id")
    private String typeId;

    @JSONField(name = "severity")
    private String severity;

    @JSONField(name = "component")
    private String component;

    @JSONField(name = "time-created")
    private Long timeCreated;

    @JSONField(name = "text")
    private String text;

    @JSONField(name = "service-affect")
    private Boolean serviceAffect;


    private ToopAlarm(Builder builder) {
        this.id = builder.id;
        this.component = builder.component;
        this.group = builder.group;
        this.ip = builder.ip;
        this.isClear = builder.isClear;
        this.resource = builder.resource;
        this.serviceAffect = builder.serviceAffect;
        this.text = builder.text;
        this.toopIp = builder.toopIp;
        this.toopKey = builder.toopKey;
        this.typeId = builder.typeId;
        this.severity = builder.severity;
        this.timeCreated = builder.timeCreated;
    }

    public static class Builder {

        private String ip;

        private String toopKey;

        private String toopIp;

        private String id;

        private Boolean isClear;

        private String resource;

        private String group;

        private String typeId;

        private String severity;

        private String component;

        private Long timeCreated;

        private String text;

        private Boolean serviceAffect;

        public Builder(ToopAlarm alarm) {
            this.ip = alarm.getIp();
            this.toopKey = alarm.getToopKey();
            this.toopIp = alarm.getToopIp();
            this.id = alarm.getId();
            this.isClear = alarm.getIsClear();
            this.resource = alarm.getResource();
            this.group = alarm.getGroup();
            this.typeId = alarm.getTypeId();
            this.severity = alarm.getSeverity();
            this.component = alarm.getComponent();
            this.timeCreated = alarm.getTimeCreated();
            this.text = alarm.getText();
            this.serviceAffect = alarm.getServiceAffect();
        }

        public Builder ip(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder toopKey(String toopKey) {
            this.toopKey = toopKey;
            return this;
        }

        public Builder toopIp(String toopIp) {
            this.toopIp = toopIp;
            return this;
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder isClear(Boolean isClear) {
            this.isClear = isClear;
            return this;
        }

        public Builder resource(String resource) {
            this.resource = resource;
            return this;
        }

        public Builder group(String group) {
            this.group = group;
            return this;
        }

        public Builder typeId(String typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder severity(String severity) {
            this.severity = severity;
            return this;
        }

        public Builder component(String component) {
            this.component = component;
            return this;
        }

        public Builder timeCreated(Long timeCreated) {
            this.timeCreated = timeCreated;
            return this;
        }

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public Builder serviceAffect(Boolean serviceAffect) {
            this.serviceAffect = serviceAffect;
            return this;
        }

        public ToopAlarm build() {
            return new ToopAlarm(this);
        }

    }


}