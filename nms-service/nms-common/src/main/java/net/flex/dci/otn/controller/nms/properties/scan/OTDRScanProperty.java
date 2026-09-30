package net.flex.dci.otn.controller.nms.properties.scan;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.Map;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;

/**
 * @version 1.0
 * @date 11/28/2023 1:08 PM
 */
@Data
public class OTDRScanProperty implements Serializable {

    @JSONField(name = "default")
    private String defaultSuffix;


    @JSONField(name = "special")
    private Map<PortType, Map<String, String>> specialSuffixRule;
}
