package net.flex.dci.otn.controller.allocate.designer.model.site;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NonNull;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.info.ReallocateEquipment;

@Builder
@Data
public class ReallocateDataModel {

    @NonNull
    List<Node> reusedNodesSnapshot;
    @NonNull
    List<Node> reusedNodesSnapshotOp;//生成bom时使用

    @NonNull
    Map<String, String> reallocateEquipMap;  //<oldEquipId,newEquipId>

    @NonNull
    Map<String, Map<String, String>> reallocateNodeEquipMap;//<oldNodeId,<oldEquipId,newEquipId>>
    @NonNull
    Map<String, List<ReallocateEquipment>> reallocateMapInsideCompute;//（重用compute结果里面的node）的场景, <newNodeId,>

    @Default
    Map<String, String> reallocateEquipVendorMap= Collections.EMPTY_MAP;  //<newEquipId,newVendorType>, if equip not moved, then equipId is oldEquipId
}
