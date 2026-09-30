package net.flex.dci.otc.controller.status.util;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.controller.status.util.Constants.SITE_LINK_PATTERN;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.controller.status.core.enums.AlarmSeverityCode;
import net.flex.dci.otc.controller.status.core.enums.AlignStatusCode;
import net.flex.dci.otc.controller.status.core.enums.OperationStatusCode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @version 1.0
 * @date 2022/4/4 12:32
 */
@Slf4j
public class StatusUtil {


    public static <T> Set<T> getIntersectionSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.intersection(before, after);
        return diff;
    }

    public static <T> Set<T> getDifferenceSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.difference(before, after);
        return diff;
    }

    public static <T> Set<T> getUnionSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.union(before, after);
        return diff;
    }

    public static boolean containsAll(Set<String> primaryLinkIds, List<String> destLinkIds) {
        return primaryLinkIds.containsAll(destLinkIds);
    }

    public static AlarmSeverity calculateAlarmSeverity(List<Alarm> alarms) {
        Integer code = 0b0000000;
        for (Alarm alarm : alarms) {
            AlarmSeverity severity = AlarmSeverity.valueOf(alarm.getSeverity());
            code |= AlarmSeverityCode.getSeverityCode(severity);
        }
        return AlarmSeverityCode.getSeverity(code);
    }

    public static String getPhyNeId(String id) {
        log.debug("start to get ne id,ref key is :{}", id);
        String[] keys = id.split(POUND);
        return keys[0] + POUND + keys[1];
    }

    public static AlignmentStatusType calculateAlignStatus(AlignmentStatusType oldAlignStatus,
            AlignmentStatusType newAlignStatus) {
        log.debug("calculate align state");
        int updateCode = AlignStatusCode.getAlignStatusCode(oldAlignStatus)
                | AlignStatusCode.getAlignStatusCode(newAlignStatus);
        return AlignStatusCode.getAlignmentStatusType(updateCode);
    }

    public static AlarmSeverity calculateAlarmSeverity(AlarmSeverity oldAlarmState,
            AlarmSeverity newAlarmState) {
        log.debug("start to calculate alarm state");
        int currentCode = AlarmSeverityCode.getSeverityCode(oldAlarmState);
        int newCode = AlarmSeverityCode.getSeverityCode(newAlarmState);
        int updateCode = currentCode | newCode;
        return AlarmSeverityCode.getSeverity(updateCode);
    }

    public static AlarmSeverity calculateAlarmSeverityByPhyNodes(List<Node> phyNodes) {
        AtomicReference<AlarmSeverity> alarmSeverity = new AtomicReference<>(AlarmSeverity.Unknown);
        phyNodes.forEach(node -> {
            AlarmSeverity nodeSeverity =
                    node.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                            .getPhysical()
                            .getAlarmState();
            alarmSeverity.set(calculateAlarmSeverity(alarmSeverity.get(), nodeSeverity));
        });

        return alarmSeverity.get();
    }

    public static AlarmSeverity calculateAlarmStateByList(List<AlarmSeverity> alarmSeverities) {
        if (alarmSeverities.isEmpty()) {
            return AlarmSeverity.Cleared;
        }
        int combinedCode = 0;
        for (AlarmSeverity severity : alarmSeverities) {
            combinedCode |= AlarmSeverityCode.getSeverityCode(severity);
        }
        return AlarmSeverityCode.getSeverity(combinedCode);
    }

    public static AlignmentStatusType calculateAlignStatus(
            List<AlignmentStatusType> alignmentStatusTypes) {
        if (alignmentStatusTypes.isEmpty()) {
            return AlignmentStatusType.Unknown;
        }
        int combinedCode = 0;
        for (AlignmentStatusType alignmentStatusType : alignmentStatusTypes) {
            combinedCode |= AlignStatusCode.getAlignStatusCode(alignmentStatusType);
        }
        return AlignStatusCode.getAlignmentStatusType(combinedCode);
    }

    public static boolean isSiteLinkId(String linkRef) {
        Pattern pattern = Pattern.compile(SITE_LINK_PATTERN);
        Matcher matcher = pattern.matcher(linkRef);
        return matcher.find();
    }

    public static OperStatus calculateOperState(OperStatus sourceOper,
            OperStatus destOper) {
        log.debug("calculate the operation state ");
        Integer sourceCode = OperationStatusCode.getOperationStatusCode(sourceOper);
        Integer destCode = OperationStatusCode.getOperationStatusCode(destOper);
        Integer resultCode = sourceCode | destCode;
        return OperationStatusCode.getOperStatusType(resultCode);
    }

    public static OperStatus calculateOperState(List<OperStatus> operStatuses) {
        if (operStatuses.isEmpty()) {
            return OperStatus.Unknown;
        }
        int combinedCode = 0;
        for (OperStatus operStatus : operStatuses) {
            combinedCode |= OperationStatusCode.getOperationStatusCode(operStatus);
        }
        return OperationStatusCode.getOperStatusType(combinedCode);
//        AtomicReference<OperStatus> currentState = new AtomicReference<>(OperStatus.Unknown);
//        operStatuses.forEach(operStatus -> {
//            currentState.set(calculateOperState(currentState.get(), operStatus));
//        });
//        return currentState.get();
    }

    public static String getPhyNodeRefSubnetId(Node node) {
        String subnetId = node.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
                .getPhysical().getPlaneId();
        return subnetId;
    }
}
