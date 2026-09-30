package net.flex.dci.otn.controller.implement.common.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;

/**
 *
 * 2025/8/21
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum CustomApsPath {

    NONE(ApsPath.NONE, ApsPathType.PRIMARY, ApsSwitchMode.AUTO, Action.CLEAR) {
        @Override
        public Short getIndex() {
            return 1;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.PRIMARY;
        }

        @Override
        public String getApsPortNum() {
            return "A";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "A.index";
        }
    },
    PRIMARY(ApsPath.PRIMARY, ApsPathType.PRIMARY, ApsSwitchMode.FORCE, Action.RESTORE) {
        @Override
        public Short getIndex() {
            return 1;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.PRIMARY;
        }

        @Override
        public String getApsPortNum() {
            return "A";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "A.index";
        }
    },
    SECONDARY(ApsPath.SECONDARY, ApsPathType.ALTERNATE, ApsSwitchMode.FORCE, Action.RESTORE) {
        @Override
        public Short getIndex() {
            return 1;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.SECONDARY;
        }

        @Override
        public String getApsPortNum() {
            return "B";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "B.index";
        }
    },
    THIRD(ApsPath.THIRD, ApsPathType.ALTERNATE, ApsSwitchMode.FORCE, Action.RESTORE) {
        @Override
        public Short getIndex() {
            return 2;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.THIRD;
        }

        @Override
        public String getApsPortNum() {
            return "C";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "C.index";
        }
    },
    CONDITIONAL_PRIMARY(ApsPath.CONDITIONALPRIMARY, ApsPathType.PRIMARY, ApsSwitchMode.MANUEL,
            Action.CONDITIONALSWITCH) {
        @Override
        public Short getIndex() {
            return 1;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.PRIMARY;
        }

        @Override
        public String getApsPortNum() {
            return "A";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "A.index";
        }
    },
    CONDITIONAL_SECONDARY(ApsPath.CONDITIONALSECONDARY, ApsPathType.ALTERNATE,
            ApsSwitchMode.MANUEL, Action.CONDITIONALSWITCH) {
        @Override
        public Short getIndex() {
            return 1;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.SECONDARY;
        }

        @Override
        public String getApsPortNum() {
            return "B";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "B.index";
        }
    },
    CONDITIONAL_THIRD(ApsPath.CONDITIONALTHIRD, ApsPathType.ALTERNATE, ApsSwitchMode.MANUEL,
            Action.CONDITIONALSWITCH) {
        @Override
        public Short getIndex() {
            return 2;
        }

        @Override
        public ActualApsPath actualApsPath() {
            return ActualApsPath.THIRD;
        }

        @Override
        public String getApsPortNum() {
            return "C";
        }

        @Override
        public String getActualIndexPropertyKey() {
            return "C.index";
        }
    };
    private final ApsPath apsPath;
    private final ApsPathType apsRealPath;
    private final ApsSwitchMode apsSwitchMode;
    private final Action action;


    CustomApsPath(ApsPath apsPath, ApsPathType apsRealPath, ApsSwitchMode apsSwitchMode,
            Action action) {
        this.apsPath = apsPath;
        this.apsRealPath = apsRealPath;
        this.apsSwitchMode = apsSwitchMode;
        this.action = action;
    }

    public static CustomApsPath fromApsPath(ApsPath apsPath) {
        for (CustomApsPath customApsPath : CustomApsPath.values()) {
            if (customApsPath.getApsPath() == apsPath) {
                return customApsPath;
            }
        }
        throw new IllegalArgumentException("No apsPath found : " + apsPath);
    }

    public abstract Short getIndex();

    public abstract ActualApsPath actualApsPath();

    public abstract String getApsPortNum();

    public abstract String getActualIndexPropertyKey();
}
