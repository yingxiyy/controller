package net.flex.dci.otc.controller.status.model;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/24 14:00
 */
@Data
public class LinkAlarmConfig implements Serializable {

    private List<String> keywords;

    private Boolean generate;

}
