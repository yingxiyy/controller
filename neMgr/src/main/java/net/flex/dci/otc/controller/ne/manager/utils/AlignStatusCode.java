package net.flex.dci.otc.controller.ne.manager.utils;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;

/**
 * @version 1.0
 * @date 2022/4/4 10:41
 */
public enum AlignStatusCode {
    ALIGNED(0b00001),
    MODULARLACK(0b00010),
    CARDMISSMATCH(0b00100),
    CARDLACK(0b01000),
    UNKNOWN(0b10000);

    private int code;

    AlignStatusCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
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
        if (alignmentStatusType == null) {
            return UNKNOWN.getCode();
        }
        if (alignmentStatusType.equals(AlignmentStatusType.Unknown)) {
            code = UNKNOWN.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.CardLack)) {
            code = CARDLACK.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.CardMissMatch)) {
            code = CARDMISSMATCH.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.ModularLack)) {
            code = MODULARLACK.getCode();
        } else if (alignmentStatusType.equals(AlignmentStatusType.Aligned)) {
            code = ALIGNED.getCode();
        }
        return code;
    }

    public static AlignmentStatusType getAlignmentStatusType(Integer code) {
        AlignmentStatusType alignmentStatusType = AlignmentStatusType.Unknown;

        if ((code & ALIGNED.code) == ALIGNED.code) {
            alignmentStatusType = AlignmentStatusType.Aligned;
        }
        if ((code & CARDLACK.code) == CARDLACK.code) {
            alignmentStatusType = AlignmentStatusType.CardLack;
        }
        if ((code & CARDMISSMATCH.code) == CARDMISSMATCH.code) {
            alignmentStatusType = AlignmentStatusType.CardMissMatch;
        }
        if ((code & MODULARLACK.code) == MODULARLACK.code) {
            alignmentStatusType = AlignmentStatusType.ModularLack;
        }
        if ((code & UNKNOWN.code) == UNKNOWN.code) {
            alignmentStatusType = AlignmentStatusType.Unknown;
        }
        return alignmentStatusType;
    }
}


