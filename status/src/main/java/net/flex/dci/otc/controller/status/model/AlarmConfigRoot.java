package net.flex.dci.otc.controller.status.model;

import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/24 13:56
 */
@Data
public class AlarmConfigRoot implements Serializable {

    private AlarmConfig alarm;

    private Boolean filter;

}
