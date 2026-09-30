package net.flex.dci.otc.controller.status.model;

import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/11/24 13:57
 */
@Data
public class AlarmConfig implements Serializable {

    private LinkAlarmConfig link;

}
