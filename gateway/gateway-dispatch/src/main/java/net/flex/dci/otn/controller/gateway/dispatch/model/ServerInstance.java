package net.flex.dci.otn.controller.gateway.dispatch.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2022/2/15 13:51
 */
@Data
@Builder
public class ServerInstance implements Serializable {

    @Tolerate
    public ServerInstance() {

    }

    private String ip;

    private int port;

    private String path;
}
