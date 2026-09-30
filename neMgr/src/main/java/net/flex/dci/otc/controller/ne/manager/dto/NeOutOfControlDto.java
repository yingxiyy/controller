package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import java.text.MessageFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/25 14:35
 */
@Data
@Builder
@AllArgsConstructor
public class NeOutOfControlDto implements Serializable {

    public static final String DESCRIPTION = "ne out of control";

    private String neId;

    private String friendlyName;

    private String ip;

    private String port;

    @Override
    public String toString() {
        return MessageFormat.format("ne :{0}(id:{1} ,ip:{2},port:{3}) is out of control ",
                friendlyName, neId, ip, port);
    }
}
