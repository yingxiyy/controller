package net.flex.dci.otc.controller.status.core.enums;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;

/**
 * @version 1.0
 * @date 2022/4/4 10:41
 */
public enum AlignStatusCode {
    ALIGNED(0b000001),
    MODULARMISSMATCH(0b000010),
    MODULARLACK(0b000100),
    CARDMISSMATCH(0b001000),
    CARDLACK(0b010000),
    UNKNOWN(0b100000);

    private int code;

    AlignStatusCode(Integer code) {
        this.code = code;
    }

    /**
     * get align status code
     *
     * @param alignmentStatusType
     * @return
     */
    public static Integer getAlignStatusCode(AlignmentStatusType alignmentStatusType) {
        Integer code = 0b10000;
        if (alignmentStatusType.equals(AlignmentStatusType.CardLack)) {
            code = CARDLACK.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.CardMissMatch)) {
            code = CARDMISSMATCH.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.ModularLack)) {
            code = MODULARLACK.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.ModularMissMatch)) {
            code = MODULARMISSMATCH.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.Aligned)) {
            code = ALIGNED.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.Unknown)) {
            code = UNKNOWN.getCode();
        }
        return code;
    }

    public static AlignmentStatusType getAlignmentStatusType(Integer code) {

        if ((code & CARDLACK.code) == CARDLACK.code) {
            return AlignmentStatusType.CardLack;
        }
        if ((code & CARDMISSMATCH.code) == CARDMISSMATCH.code) {
            return AlignmentStatusType.CardMissMatch;
        }
        if ((code & MODULARLACK.code) == MODULARLACK.code) {
            return AlignmentStatusType.ModularLack;
        }
        if ((code & MODULARMISSMATCH.code) == MODULARMISSMATCH.code) {
            return AlignmentStatusType.ModularMissMatch;
        }
        if ((code & UNKNOWN.code) == UNKNOWN.code) {
            return AlignmentStatusType.Unknown;
        }
        if ((code & ALIGNED.code) == ALIGNED.code) {
            return AlignmentStatusType.Aligned;
        }
        return AlignmentStatusType.Unknown;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}


