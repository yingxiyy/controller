package net.flex.dci.otc.controller.otdr.model;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Data;

/**
 * "atten-coefficient": "0.216", "reflectance": "j8Jbwg==", "event-type": "CONNECTOR", "index": 1,
 * "distance-to-last-location": "0.0096", "location": "0.0096", "total-loss": "0.45", "event-loss":
 * "0.62"
 *
 * @version 1.0
 * @date 2022/7/20 15:44
 */
@Data
public class OtdrEvent implements Serializable {

    @JSONField(name = "atten-coefficient")
    private String attenCoefficient;

    @JSONField(name = "reflectance")
    private String reflectanc;

    @JSONField(name = "event-type")
    private String eventType;

    private int index;

    @JSONField(name = "distance-to-last-location")
    private String distanceToLastLocation;


    @JSONField(name = "location")
    private String location;

    @JSONField(name = "total-loss")
    private String totalLoss;

    @JSONField(name = "event-loss")
    private String eventLoss;
}
