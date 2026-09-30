package net.flex.dci.otc.controller.ne.manager.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.flex.dci.otc.controller.ne.manager.enums.NeSynchronizedState;

/**
 * 2026/4/19
 *
 * @author musa
 * @version 1.0
 **/
@Data
@AllArgsConstructor
public class ManageNeSynchroInfo implements Serializable {

    private String neId;

    private NeSynchronizedState synState;
}
