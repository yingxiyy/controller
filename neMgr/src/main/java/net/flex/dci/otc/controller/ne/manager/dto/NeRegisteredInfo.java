package net.flex.dci.otc.controller.ne.manager.dto;

import java.util.Set;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/11/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class NeRegisteredInfo {

    private Set<String> notRegisteredNes;
    private Set<String> registeredNes;

    private Set<String> needSynchronizedNes;

}
