package net.flex.dci.otc.controller.status.core.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;

/**
 *
 * @version 1.0
 * @date 8/27/2025 3:49 PM
 */
@Getter
public enum CustomAlignmentStatus {
    Aligned("aligned", AlignmentStatusType.Aligned),

    /**
     * Modular is lack
     *
     */
    ModularLack("modularLack", AlignmentStatusType.ModularLack),

    /**
     * card miss match
     *
     */
    CardMissMatch("cardMissMatch", AlignmentStatusType.CardMissMatch),

    /**
     * card is lack
     *
     */
    CardLack("cardLack", AlignmentStatusType.CardLack),

    /**
     * unknown status
     *
     */
    Unknown("unknown", AlignmentStatusType.Unknown);
    private final String name;

    private final AlignmentStatusType alignmentStatusType;

    CustomAlignmentStatus(String name, AlignmentStatusType alignmentStatusType) {
        this.name = name;
        this.alignmentStatusType = alignmentStatusType;
    }

    public static AlignmentStatusType getAlignmentStatusFromName(String alignmentStateStr) {
        for (CustomAlignmentStatus customAlignmentStatus : CustomAlignmentStatus.values()) {
            if (customAlignmentStatus.getName().equals(alignmentStateStr)) {
                return customAlignmentStatus.getAlignmentStatusType();
            }
        }
        throw new IllegalArgumentException("UnSupported alignmentState name :" + alignmentStateStr);
    }
}
