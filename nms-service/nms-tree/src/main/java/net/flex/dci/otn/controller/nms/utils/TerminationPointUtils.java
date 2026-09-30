package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otn.controller.nms.utils.Constants.MPO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.dto.MpoTpInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;

/**
 *
 * @version 1.0
 * @date 9/3/2025 3:36 PM
 */
@Slf4j
public class TerminationPointUtils {

    private static final Map<EquipType, Integer> mpoEquipPerMap = new HashMap<>();

    static {
        mpoEquipPerMap.put(EquipType.MUX, 8);
        mpoEquipPerMap.put(EquipType.CMUX64, 8);
        mpoEquipPerMap.put(EquipType.MUX32CL, 8);
        mpoEquipPerMap.put(EquipType.MUXPANEL, 8);
    }

    public static List<String> getMpoTpRefTps(List<MpoTpInfo> mpoTpInfos) {
        log.debug("get mpo tp refTps:{}", mpoTpInfos);
        List<String> tpIds = new ArrayList<>();
        for (MpoTpInfo mpoTpInfo : mpoTpInfos) {
            List<String> mpoRefTpIds = getMpoRefTpS(mpoTpInfo.getTpId(), mpoTpInfo.getEquipType());
            tpIds.addAll(mpoRefTpIds);
        }
        return tpIds;
    }

    private static List<String> getMpoRefTpS(String mpoId, EquipType equipType) {
        //temp method
        String[] ids = mpoId.split(MPO);
        int mpoIndex = Integer.parseInt(ids[1]) - 1;
        Integer mdPerMpo = mpoEquipPerMap.get(equipType);
        return IntStream.rangeClosed(1, mdPerMpo)
                .mapToObj(i -> {
                    int mdId = i + mpoIndex * mdPerMpo;
                    return ids[0] + "M" + mdId + "D" + mdId;
                })
                .collect(Collectors.toList());
    }
}
