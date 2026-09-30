package net.flex.dci.otn.controller.implement.common.recorder;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *{
 *   "node-ref": "Site-1637822513176#Ne-1637823014069",,
 *   "friendly-name": "CD-SN-TMP-TMP-IOPC4-D-02-1637823014069",
 *   "ip": "172.24.168.75",
 *   "properties": {
 *     "property": [{
 *         "name": "xc@XC-Site-1637822513176#Ne-1637823014069#LINECARD-1-1#PORT-1-1-C2/odu4=1-Site-1637822513176#Ne-1637823014069#LINECARD-1-1#PORT-1-1-L1/odu4=2@PORT-1-1-C2-PORT-1-1-L1",
 *         "value": "failure"
 *       }, {
 *         "name": "equipment@Site-1637822513176#Ne-1637823014069#TRANSCEIVER-1-1-C2@TRANSCEIVER-1-1-C2",
 *         "value": "success"
 *       }, {
 *         "name": "tp@Site-1637822513176#Ne-1637823014069#LINECARD-1-1#PORT-1-1-C2@T2X4C8-1-1-C2",
 *         "value": "success"
 *       }
 *     ]
 *   }
 * }
 */
@Data
public class StepRecord {
    @SerializedName("node-ref")
    private String nodeId;

    @SerializedName("friendly-name")
    private String friendlyName;

    private String ip;

    @SerializedName("properties")
    private Properties properties;

    @Data
    public class Properties {
        @SerializedName("property")
        List<Property> propertyList;

        public Properties() {
            this.propertyList = new ArrayList<>();
        }
    }

    @Data
    public class Property {
        String name;
        String value;
        String errorMsg;

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public Property(String name, String value, String errorMsg) {
            this.name = name;
            this.value = value;
            this.errorMsg = errorMsg;
        }
    }

    public StepRecord(String nodeId, String friendlyName, String ip) {
        this.nodeId = nodeId;
        this.friendlyName = friendlyName;
        this.ip = ip;
        this.properties = new Properties();
    }

    public void updateProperty(String name, String value) {
        boolean found = false;
        for (Property prop : this.properties.propertyList) {
            if (prop.name.equals(name)) {
                prop.value = value;
                found = true;
                break;
            }
        }
        if (!found) {
            this.properties.propertyList.add(new Property(name, value));
        }
    }

    public void updatePropertyWithError(String name, String value, String errorMsg) {
        boolean found = false;
        for (Property prop : this.properties.propertyList) {
            if (prop.name.equals(name)) {
                prop.value = value;  //update value;
                prop.errorMsg = errorMsg;
                found = true;
                break;
            }
        }
        if (!found) {
            this.properties.propertyList.add(new Property(name, value, errorMsg));
        }
    }
}
