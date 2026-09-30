package net.flex.dci.otn.controller.implement.common.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.slf4j.Logger;

public final class NoIpConfigWriteLogger {

    // No-IP writes skip the adapter, so keep a JVM-lifetime device view for field overlap verification.
    private static final Map<String, List<CrossConnections>> NO_IP_DEVICE_XCS = new ConcurrentHashMap<>();

    private NoIpConfigWriteLogger() {
    }

    public static List<NoIpDeviceError> logSkippedWrite(Logger log, String scene, ImplActionType actionType, Node node) {
        Physical physical = physical(node);
        if (physical == null) {
            log.warn("NO_IP_CONFIG_WRITE scene={} action={} node={} physical=null",
                    scene, actionType, node == null || node.getNodeId() == null ? null : node.getNodeId().getValue());
            return Collections.emptyList();
        }

        List<CrossConnections> xcList = physical.getCrossConnections() == null
                ? Collections.emptyList() : physical.getCrossConnections();
        List<CrossConnections> wssXcList = xcList.stream()
                .filter(xc -> xc != null && xc.getWssChannel() != null)
                .collect(Collectors.toList());
        String nodeId = node.getNodeId().getValue();

        // 无 IP 缓存只记录本 JVM 见过的请求，不代表设备真实状态，不能作为下发失败依据。
        // 所有调用者统一保持 writeWithoutIP 的跳过设备、视为成功语义。
        List<NoIpDeviceError> diagnostics = updateNoIpDeviceState(
                log, scene, actionType, node, physical, xcList, wssXcList, nodeId);
        for (NoIpDeviceError diagnostic : diagnostics) {
            log.warn("NO_IP_CONFIG_DIAGNOSTIC scene={} action={} node={} message={}",
                    scene, actionType, nodeId, diagnostic.getMessage());
        }
        return Collections.emptyList();
    }

    private static List<NoIpDeviceError> updateNoIpDeviceState(Logger log, String scene, ImplActionType actionType, Node node,
            Physical physical, List<CrossConnections> xcList, List<CrossConnections> wssXcList, String nodeId) {
        List<NoIpDeviceError> errors = new ArrayList<>();
        NO_IP_DEVICE_XCS.compute(nodeId, (id, current) -> {
            if (current == null && (xcList.isEmpty() || isRemoveScene(scene))) {
                log.info("NO_IP_CONFIG_WRITE scene={} action={} node={} name={} tpCount={} eqCount={} ilCount={} xcCount={} wssXcCount={} ocmCount={} xcs={} deviceXcCount={} deviceWssXcCount={} deviceXcs={}",
                        scene,
                        actionType,
                        nodeId,
                        physical.getFriendlyName(),
                        node.getTerminationPoint() == null ? 0 : node.getTerminationPoint().size(),
                        physical.getEquipments() == null ? 0 : physical.getEquipments().size(),
                        physical.getInternalLinks() == null ? 0 : physical.getInternalLinks().size(),
                        xcList.size(),
                        wssXcList.size(),
                        physical.getOCMGripGroups() == null ? 0 : physical.getOCMGripGroups().size(),
                        summarizeXcs(xcList),
                        0,
                        0,
                        Collections.emptyList());
                return null;
            }
            List<CrossConnections> deviceXcs = current == null ? new ArrayList<>() : new ArrayList<>(current);
            if (isRemoveScene(scene)) {
                if (current != null) {
                    errors.addAll(validateRemoveXcs(deviceXcs, xcList));
                }
                // 缺失项仅告警，仍移除缓存中存在的项，与无 IP 成功返回保持一致。
                removeXcs(deviceXcs, xcList);
            } else {
                upsertXcs(deviceXcs, xcList);
                if (!wssXcList.isEmpty()) {
                    errors.addAll(findWssOverlap(log, scene, actionType, nodeId, deviceXcs));
                }
            }
            List<CrossConnections> deviceWssXcList = deviceXcs.stream()
                    .filter(xc -> xc != null && xc.getWssChannel() != null)
                    .collect(Collectors.toList());

            log.info("NO_IP_CONFIG_WRITE scene={} action={} node={} name={} tpCount={} eqCount={} ilCount={} xcCount={} wssXcCount={} ocmCount={} xcs={} deviceXcCount={} deviceWssXcCount={} deviceXcs={}",
                    scene,
                    actionType,
                    nodeId,
                    physical.getFriendlyName(),
                    node.getTerminationPoint() == null ? 0 : node.getTerminationPoint().size(),
                    physical.getEquipments() == null ? 0 : physical.getEquipments().size(),
                    physical.getInternalLinks() == null ? 0 : physical.getInternalLinks().size(),
                    xcList.size(),
                    wssXcList.size(),
                    physical.getOCMGripGroups() == null ? 0 : physical.getOCMGripGroups().size(),
                    summarizeXcs(xcList),
                    deviceXcs.size(),
                    deviceWssXcList.size(),
                    summarizeXcs(deviceXcs));

            return deviceXcs;
        });
        return errors;
    }

    private static boolean isRemoveScene(String scene) {
        return scene != null && scene.contains("removeResource");
    }

    private static void upsertXcs(List<CrossConnections> deviceXcs, List<CrossConnections> xcList) {
        for (CrossConnections xc : xcList) {
            String xcId = xcId(xc);
            if (xcId == null) {
                continue;
            }
            deviceXcs.removeIf(exist -> xcId.equals(xcId(exist)));
            deviceXcs.add(new CrossConnectionsBuilder(xc).build());
        }
    }

    private static void removeXcs(List<CrossConnections> deviceXcs, List<CrossConnections> xcList) {
        for (CrossConnections xc : xcList) {
            String xcId = xcId(xc);
            if (xcId != null) {
                deviceXcs.removeIf(exist -> xcId.equals(xcId(exist)));
            }
        }
    }

    private static List<NoIpDeviceError> validateRemoveXcs(List<CrossConnections> deviceXcs, List<CrossConnections> xcList) {
        List<NoIpDeviceError> errors = new ArrayList<>();
        for (CrossConnections xc : xcList) {
            String xcId = xcId(xc);
            if (xcId == null) {
                continue;
            }
            boolean exists = deviceXcs.stream().anyMatch(exist -> xcId.equals(xcId(exist)));
            if (!exists) {
                // 此处包含电层 XC；缓存缺失不等于真实设备缺失，不能统一称为 WSS。
                errors.add(new NoIpDeviceError(xcId, "XC not observed in no-IP diagnostic cache: " + xcId));
            }
        }
        return errors;
    }

    private static String xcId(CrossConnections xc) {
        if (xc == null || xc.getCrossConnectionId() == null) {
            return null;
        }
        return xc.getCrossConnectionId().getValue();
    }

    private static Physical physical(Node node) {
        if (node == null || node.getAugmentation(Node1.class) == null) {
            return null;
        }
        return node.getAugmentation(Node1.class).getPhysical();
    }

    private static List<String> summarizeXcs(List<CrossConnections> xcList) {
        List<String> summary = new ArrayList<>();
        for (CrossConnections xc : xcList) {
            summary.add(formatXc(xc));
        }
        return summary;
    }

    private static String formatXc(CrossConnections xc) {
        if (xc == null) {
            return "null";
        }
        return String.format("%s[%s,%s] fixed=%s admin=%s impl=%s src=%s dst=%s desc=%s",
                xc.getCrossConnectionId() == null ? null : xc.getCrossConnectionId().getValue(),
                lower(xc),
                upper(xc),
                xc.isFixed(),
                xc.getAdminState(),
                xc.getImplementState(),
                sourceTps(xc),
                destinationTps(xc),
                xc.getDescription());
    }

    private static List<String> sourceTps(CrossConnections xc) {
        if (xc.getSourceTp() == null) {
            return Collections.emptyList();
        }
        return xc.getSourceTp().stream().map(NoIpConfigWriteLogger::formatSourceTp)
                .collect(Collectors.toList());
    }

    private static List<String> destinationTps(CrossConnections xc) {
        if (xc.getDestinationTp() == null) {
            return Collections.emptyList();
        }
        return xc.getDestinationTp().stream().map(NoIpConfigWriteLogger::formatDestinationTp)
                .collect(Collectors.toList());
    }

    private static String formatSourceTp(SourceTp tp) {
        return String.format("%s/%s",
                tp.getTpRef() == null ? null : tp.getTpRef().getValue(),
                tp.getSlot());
    }

    private static String formatDestinationTp(DestinationTp tp) {
        return String.format("%s/%s",
                tp.getTpRef() == null ? null : tp.getTpRef().getValue(),
                tp.getSlot());
    }

    private static List<NoIpDeviceError> findWssOverlap(Logger log, String scene, ImplActionType actionType,
            String nodeId, List<CrossConnections> wssXcList) {
        List<NoIpDeviceError> errors = new ArrayList<>();
        for (int i = 0; i < wssXcList.size(); i++) {
            CrossConnections left = wssXcList.get(i);
            for (int j = i + 1; j < wssXcList.size(); j++) {
                CrossConnections right = wssXcList.get(j);
                if (overlap(left, right)) {
                    // 仅提示缓存中的频率重叠，不模拟真实设备失败。
                    log.warn("NO_IP_WSS_XC_OVERLAP scene={} action={} node={} left={} right={}",
                            scene, actionType, nodeId, formatXc(left), formatXc(right));
                    String objectId = xcId(right) == null ? xcId(left) : xcId(right);
                    errors.add(new NoIpDeviceError(objectId, String.format(
                            "WSS XC overlap on no-IP node %s, scene=%s, action=%s, left=%s, right=%s",
                            nodeId, scene, actionType, formatXc(left), formatXc(right))));
                }
            }
        }
        return errors;
    }

    private static boolean overlap(CrossConnections left, CrossConnections right) {
        Long leftLower = lower(left);
        Long leftUpper = upper(left);
        Long rightLower = lower(right);
        Long rightUpper = upper(right);
        if (leftLower == null || leftUpper == null || rightLower == null || rightUpper == null) {
            return false;
        }
        return leftLower < rightUpper && rightLower < leftUpper;
    }

    private static Long lower(CrossConnections xc) {
        if (xc == null || xc.getWssChannel() == null || xc.getWssChannel().getLowerFrequency() == null) {
            return null;
        }
        return xc.getWssChannel().getLowerFrequency().getValue().longValue();
    }

    private static Long upper(CrossConnections xc) {
        if (xc == null || xc.getWssChannel() == null || xc.getWssChannel().getUpperFrequency() == null) {
            return null;
        }
        return xc.getWssChannel().getUpperFrequency().getValue().longValue();
    }

    public static class NoIpDeviceError {
        private final String objectId;
        private final String message;

        private NoIpDeviceError(String objectId, String message) {
            this.objectId = objectId;
            this.message = message;
        }

        public String getObjectId() {
            return objectId;
        }

        public String getMessage() {
            return message;
        }
    }
}
