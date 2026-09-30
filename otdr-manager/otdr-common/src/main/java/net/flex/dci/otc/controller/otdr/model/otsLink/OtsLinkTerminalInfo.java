package net.flex.dci.otc.controller.otdr.model.otsLink;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtsLinkTerminalInfo implements Serializable {

    @JSONField(name = "tp-id")
    private String tpId;

    @JSONField(name = "tp-name")
    private String tpName;

    @JSONField(name = "node-id")
    private String nodeId;

    @JSONField(name = "node-name")
    private String nodeName;

    @JSONField(name = "node-ip")
    private String nodeIp;

    @JSONField(name = "site-id")
    private String siteId;

    @JSONField(name = "site-name")
    private String siteName;

}
