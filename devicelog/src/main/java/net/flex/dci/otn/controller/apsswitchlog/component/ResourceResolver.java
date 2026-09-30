package net.flex.dci.otn.controller.apsswitchlog.component;

import java.util.List;
import net.flex.dci.otn.controller.apsswitchlog.enums.ResourceType;

/**
 *
 * @version 1.0
 * @date 11/17/2025 11:11 AM
 */
public interface ResourceResolver {

    List<String> resolve(String resourceId);

    ResourceType resourceType();
}
