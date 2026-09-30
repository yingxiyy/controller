package net.flex.dci.otn.controller.implement.common.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;

/**
 *
 * 2025/8/24
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum ActualApsPath {

    PRIMARY(ApsPathType.PRIMARY, "0", "A"),
    SECONDARY(ApsPathType.ALTERNATE, "1", "B"),
    THIRD(ApsPathType.ALTERNATE, "2", "C");

    private final ApsPathType apsPathType;

    private final String index;

    private final String briefName;

    ActualApsPath(ApsPathType apsPathType, String index, String briefName) {
        this.apsPathType = apsPathType;
        this.index = index;
        this.briefName = briefName;
    }


}
