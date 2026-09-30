package net.flex.dci.otc.controller.ne.manager.utils;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.POUND;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.dto.SlotInfo;

/**
 * @version 1.0
 * @date 2022/3/29 16:10
 */
@Slf4j
public class SlotInfoUtils {

    public static SlotInfo extractSlotInfoFromEqId(String eqId) {
        String[] eqIds = eqId.split(POUND);
        String directEqName = eqIds[eqIds.length - 1];
        String[] eqDirectIds = directEqName.split(HYPHEN);
        int size = eqDirectIds.length;

        if (size == 4) {
            return SlotInfo.builder().slot(eqDirectIds[2])
                    .shelf(eqDirectIds[1]).port(eqDirectIds[3]).build();
        }
        return SlotInfo.builder().slot(eqDirectIds[2])
                .shelf(eqDirectIds[1]).build();
    }

}
