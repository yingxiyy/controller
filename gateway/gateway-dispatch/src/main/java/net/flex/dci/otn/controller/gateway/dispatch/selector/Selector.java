package net.flex.dci.otn.controller.gateway.dispatch.selector;

import java.io.Serializable;
import java.util.List;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;

/**
 * @version 1.0
 * @date 2022/2/15 15:44
 */
public interface Selector extends Serializable {

    List<ServerInstance> selectByPath(String path);

    List<ServerInstance> selectByName(String name);
}
