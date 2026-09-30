package net.flex.dci.otc.controller.ne.manager.core.kafka.model;

import java.io.Serializable;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/27 15:55
 */
@Data
public class ObjectChange implements Serializable {

    private String objectId;

    private String type;

    private String action;

}
