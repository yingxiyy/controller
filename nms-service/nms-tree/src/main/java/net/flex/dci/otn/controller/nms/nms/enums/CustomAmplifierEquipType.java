package net.flex.dci.otn.controller.nms.nms.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 * 2025/8/11
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum CustomAmplifierEquipType {
    IRA(EquipType.IRA) {
        @Override
        public String getSourceBACSuffix() {
            return "BAC";
        }


        @Override
        public String getSourceBALSuffix() {
            return "BAL";
        }

        @Override
        public String getDestBACSuffix() {
            return "BAC";
        }

        @Override
        public String getDestBALSuffix() {
            return "BAL";
        }


        @Override
        public String getSourcePACSuffix() {
            return "PAC";
        }


        @Override
        public String getSourcePALSuffix() {
            return "PAL";
        }

        @Override
        public String getDestPACSuffix() {
            return "PAC";
        }

        @Override
        public String getDestPALSuffix() {
            return "PAL";
        }

        @Override
        public boolean relayBoard() {
            return false;
        }
    },
    DGE(EquipType.DGE) {
        @Override
        public String getSourceBACSuffix() {
            return "ILAC_WEST";
        }


        @Override
        public String getSourceBALSuffix() {
            return "ILAL_WEST";
        }

        @Override
        public String getDestBACSuffix() {
            return "ILAC_EAST";
        }

        @Override
        public String getDestBALSuffix() {
            return "ILAL_EAST";
        }

        @Override
        public String getSourcePACSuffix() {
            return "";
        }


        @Override
        public String getSourcePALSuffix() {
            return "";
        }

        @Override
        public String getDestPACSuffix() {
            return "";
        }

        @Override
        public String getDestPALSuffix() {
            return "";
        }

        @Override
        public boolean relayBoard() {
            return true;
        }
    },
    ILA(EquipType.ILA) {
        @Override
        public String getSourceBACSuffix() {
            return "ILAC_WEST";
        }


        @Override
        public String getSourceBALSuffix() {
            return "ILAL_WEST";
        }

        @Override
        public String getDestBACSuffix() {
            return "ILAC_EAST";
        }

        @Override
        public String getDestBALSuffix() {
            return "ILAL_EAST";
        }

        @Override
        public String getSourcePACSuffix() {
            return "";
        }


        @Override
        public String getSourcePALSuffix() {
            return "";
        }

        @Override
        public String getDestPACSuffix() {
            return "";
        }

        @Override
        public String getDestPALSuffix() {
            return "";
        }

        @Override
        public boolean relayBoard() {
            return true;
        }
    },
    OA(EquipType.OA) {
        @Override
        public String getSourceBACSuffix() {
            return "BA";
        }


        @Override
        public String getSourceBALSuffix() {
            return null;
        }

        @Override
        public String getDestBACSuffix() {
            return "BA";
        }

        @Override
        public String getDestBALSuffix() {
            return null;
        }

        @Override
        public String getSourcePACSuffix() {
            return "PA";
        }


        @Override
        public String getSourcePALSuffix() {
            return null;
        }

        @Override
        public String getDestPACSuffix() {
            return "PA";
        }

        @Override
        public String getDestPALSuffix() {
            return null;
        }

        @Override
        public boolean relayBoard() {
            return false;
        }
    },
    RAMAN(EquipType.RAMAN) {
        @Override
        public String getSourceBACSuffix() {
            return "";
        }

        @Override
        public String getSourceBALSuffix() {
            return "";
        }

        @Override
        public String getDestBACSuffix() {
            return "";
        }

        @Override
        public String getDestBALSuffix() {
            return "";
        }

        @Override
        public String getSourcePACSuffix() {
            return "";
        }

        @Override
        public String getSourcePALSuffix() {
            return "";
        }

        @Override
        public String getDestPACSuffix() {
            return "";
        }

        @Override
        public String getDestPALSuffix() {
            return "";
        }

        @Override
        public boolean relayBoard() {
            return true;
        }
    };

    private final EquipType equipType;

    CustomAmplifierEquipType(EquipType equipType) {
        this.equipType = equipType;
    }

    public static CustomAmplifierEquipType fromEquipType(EquipType equipType) {
        for (CustomAmplifierEquipType amplifierEquipType : CustomAmplifierEquipType.values()) {
            if (amplifierEquipType.getEquipType() == equipType) {
                return amplifierEquipType;
            }
        }
        throw new IllegalArgumentException(
                "No customAmplifier equipType found for equipType: " + equipType.name());
    }

    public abstract String getSourceBACSuffix();


    public abstract String getSourceBALSuffix();

    public abstract String getDestBACSuffix();

    public abstract String getDestBALSuffix();

    public abstract String getSourcePACSuffix();


    public abstract String getSourcePALSuffix();

    public abstract String getDestPACSuffix();

    public abstract String getDestPALSuffix();

    public abstract boolean relayBoard();
}
