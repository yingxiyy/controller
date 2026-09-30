package net.flex.dci.otn.controller.nms.component.dto;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/2/15
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ViewNodeWrapper {

    // 映射 "view" 字段
    @JSONField(name = "view")
    private View view;


    @Data
    public static class View {

        @JSONField(name = "friendly-name")
        private String friendlyName;

        @JSONField(name = "alarm-state")
        private String alarmState;

        @JSONField(name = "pos-x")
        private Integer posX;

        @JSONField(name = "pos-y")
        private Integer posY;
    }
}


