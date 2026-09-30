package net.flex.dci.otn.controller.implement.common.opList;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;

public class TypeUtils {
	public static OpType fromImplementState(ImplementState implState) {
		switch (implState) {
			case Allocate:
				return OpType.DEIMPLEMENT;
			case Implement:
				return OpType.IMPLEMENT;
			default:
				return null;
		}
	}
	
	public static ImplementState fromOpType(OpType opType) {
		switch (opType) {
			case DEIMPLEMENT:
				return ImplementState.Allocate;
			case IMPLEMENT:
				return ImplementState.Implement;
			default:
				return ImplementState.Implement;
		}
	}
	
	public static AdminStatus getAdminStatus(OpType opType) {
		switch (opType) {
			case DEIMPLEMENT:
				return AdminStatus.Down;
			case IMPLEMENT:
				return AdminStatus.Up;
			default:
				return AdminStatus.Up;
		}
	}
}
