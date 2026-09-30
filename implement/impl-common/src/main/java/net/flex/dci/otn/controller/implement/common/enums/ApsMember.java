package net.flex.dci.otn.controller.implement.common.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput.TargetApsMember;

/**
 *
 * @version 1.0
 * @date 10/24/2025 1:16 PM
 */
@Getter
public enum ApsMember {
    MEMBER_A(TargetApsMember.A, ApsPathType.PRIMARY) {
        @Override
        public String getActualIndexPropertyKey() {
            return "A.index";
        }
    },
    MEMBER_B(TargetApsMember.B, ApsPathType.ALTERNATE) {
        @Override
        public String getActualIndexPropertyKey() {
            return "B.index";
        }
    },
    MEMBER_C(TargetApsMember.C, ApsPathType.ALTERNATE) {
        @Override
        public String getActualIndexPropertyKey() {
            return "C.index";
        }
    };

    private final TargetApsMember targetApsMember;

    private final ApsPathType apsPathType;

    ApsMember(TargetApsMember targetApsMember, ApsPathType apsPathType) {
        this.targetApsMember = targetApsMember;
        this.apsPathType = apsPathType;
    }

    public static ApsMember fromTargetApsMember(TargetApsMember targetApsMember) {
        for (ApsMember apsMember : values()) {
            if (apsMember.targetApsMember == targetApsMember) {
                return apsMember;
            }
        }
        throw new IllegalArgumentException("unSupport aps member:" + targetApsMember);
    }

    public abstract String getActualIndexPropertyKey();
}
