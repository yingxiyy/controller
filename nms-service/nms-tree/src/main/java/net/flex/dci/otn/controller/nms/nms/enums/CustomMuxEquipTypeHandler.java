package net.flex.dci.otn.controller.nms.nms.enums;

import static net.flex.dci.otn.controller.nms.utils.Constants.MUXPANEL32C32L;

import net.flex.dci.otn.controller.nms.constructs.CMUX64Constructor;
import net.flex.dci.otn.controller.nms.constructs.MUXPANEL32C32LConstructor;
import net.flex.dci.otn.controller.nms.constructs.MUXPANELConstructor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;

/**
 * @version 1.0
 * @date 6/26/2025 4:24 PM
 */
public enum CustomMuxEquipTypeHandler {

    CMUX64 {
        @Override
        public boolean supportsMux() {
            return true;
        }

        @Override
        public String getMpoTpId(String tpId, String muxTpId, String vendorSpecific) {
            return CMUX64Constructor.getMPOTpId(tpId, muxTpId);
        }


        @Override
        public TerminationPoint virtualize(TerminationPoint tp, String tpId) {
            return CMUX64Constructor.virtualizeMPOTP(tp, tpId);
        }
    },
    MUXPANEL {
        @Override
        public boolean supportsMux() {
            return true;
        }

        @Override
        public String getMpoTpId(String tpId, String muxTpId, String vendorSpecific) {
            return CMUX64Constructor.getMPOTpId(tpId, muxTpId);
        }

        @Override
        public TerminationPoint virtualize(TerminationPoint tp, String tpId
        ) {
            return MUXPANELConstructor.virtualizeMPOTP(tp, tpId);
        }
    },
    MUX {
        @Override
        public boolean supportsMux() {
            return true;
        }

        @Override
        public String getMpoTpId(String tpId, String muxTpId, String vendorSpecific) {
            if (vendorSpecific.equals(MUXPANEL32C32L)) {
                return MUXPANEL32C32LConstructor.getMPOTpId(tpId, muxTpId);
            }
            return CMUX64Constructor.getMPOTpId(tpId, muxTpId);
        }

        @Override
        public TerminationPoint virtualize(TerminationPoint tp, String tpId) {
            return MUXPANELConstructor.virtualizeMPOTP(tp, tpId);
        }
    },
    IRA {
        @Override
        public boolean supportsMux() {
            return false;
        }

        @Override
        public String getMpoTpId(String tpId, String muxTpId, String vendorSpecific) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TerminationPoint virtualize(TerminationPoint tp, String tpId) {
            // IRA也用MUXPANELConstructor来虚拟
            return MUXPANELConstructor.virtualizeMPOTP(tp, tpId);
        }
    },
    FMUX32 {
        @Override
        public boolean supportsMux() {
            return true;
        }

        @Override
        public String getMpoTpId(String tpId, String muxTpId, String vendorSpecific) {
            return CMUX64Constructor.getMPOTpId(tpId, muxTpId);
        }

        @Override
        public TerminationPoint virtualize(TerminationPoint tp, String tpId) {
            return MUXPANELConstructor.virtualizeMPOTP(tp, tpId);
        }
    },
    TILA {
        @Override
        public boolean supportsMux() {
            return false;
        }

        @Override
        public String getMpoTpId(String tpId, String muxTpId, String vendorSpecific) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TerminationPoint virtualize(TerminationPoint tp, String tpId) {
            return MUXPANELConstructor.virtualizeMPOTP(tp, tpId);
        }
    };

    public abstract boolean supportsMux();

    public abstract String getMpoTpId(String tpId, String muxTpId, String vendorSpecific);

    public abstract TerminationPoint virtualize(TerminationPoint tp, String tpId);
}
