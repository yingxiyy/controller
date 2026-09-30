package net.flex.dci.otc.controller.otdr.scan;

import static net.flex.dci.otc.controller.otdr.utils.OtdrUtils.generateTaskId;
import static net.flex.dci.otc.controller.otdr.utils.OtdrUtils.getMonitorDirectionStr;

import com.alibaba.fastjson.JSONObject;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.otdr.domain.OtdrScanDetail;
import net.flex.dci.otc.controller.otdr.model.OTDRScanParameters;
import net.flex.dci.otc.controller.otdr.notification.OtdrScanStateNotification;
import net.flex.dci.otc.controller.otdr.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;

/**
 * @version 1.0
 * @date 12/1/2023 4:00 PM
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractOTDRScanner implements IOTDRScanner {


    protected final OTDRRpc otdrRpc;

    protected final TerminationPointDao terminationPointDao;

    protected final PhyLinkDao phyLinkDao;

    protected final OtdrDaoService otdrDaoService;

    protected final OtdrScanStateNotification otdrNotificationService;


    protected boolean recordOtdrResultInProgress(String nodeId, String nodeName, String resultId,
            String monitorPortId, String monitorName, MonitorDirection monitorDirection,
            ScanMode scanMode,
            OTDRScanParameters otdrScanParameters) {
        boolean isSource = false;
        try {
            log.debug("record the ne:{} scan parameters :{} otdr task in progress", nodeName,
                    otdrScanParameters);
            Date current = new Date();
            String monitorPorName = terminationPointDao.getTpPhysical(
                            PhysicalTpIdNamingRule.getNodeId(monitorPortId), monitorPortId)
                    .getFriendlyName();
            OtdrResultRecord otdrResultRecord = new OtdrResultRecord();
            otdrResultRecord.setNeId(nodeId);
            otdrResultRecord.setResultId(resultId);
            otdrResultRecord.setTpId(monitorPortId);
            otdrResultRecord.setTpName(monitorPorName);
            otdrResultRecord.setMonitorDirection(monitorDirection.getIntValue());
            otdrResultRecord.setScanResult(OtdrScanResultType.INPROGRESS.getIntValue());
            otdrResultRecord.setStartTime(current);
            BigInteger taskId = generateTaskId(current);
            otdrResultRecord.setTaskId(taskId);
            otdrResultRecord.setScanMode(scanMode.name());
            otdrResultRecord.setScanParameters(JSONObject.toJSONString(otdrScanParameters));
            List<Link> phyLinks = phyLinkDao.listAllPhyLinksUnderTp(monitorPortId);
            //assemble there have only one phy link
            Link link = phyLinks.get(0);
            if (link != null) {
                if (link.getAugmentation(Link1.class) != null
                        && link.getAugmentation(Link1.class).getPhysical() != null) {
                    Physical phy = link.getAugmentation(Link1.class).getPhysical();
                    if (phy.getProvider() != null) {
                        BigDecimal baseDistance = phy.getProvider().getDistance();
                        if (baseDistance != null) {
                            otdrResultRecord.setBaseDistance(baseDistance.doubleValue());
                        }
                        BigDecimal azAtt =
                                phy.getProvider().getAttenuationAz() == null ? new BigDecimal(0)
                                        : phy.getProvider().getAttenuationAz();
                        BigDecimal zaAtt =
                                phy.getProvider().getAttenuationZa() == null ? new BigDecimal(0)
                                        : phy.getProvider().getAttenuationZa();

                        isSource = link.getSource().getSourceTp().getValue()
                                .equals(monitorPortId);
                        if (isSource) {
                            if (monitorDirection == MonitorDirection.OUT
                                    && azAtt != null) {
                                otdrResultRecord.setBaseLoss(azAtt.doubleValue());
                            } else if (monitorDirection == MonitorDirection.IN
                                    && azAtt != null) {
                                otdrResultRecord.setBaseLoss(zaAtt.doubleValue());
                            }
                        } else {
                            if (monitorDirection == MonitorDirection.OUT
                                    && azAtt != null) {
                                otdrResultRecord.setBaseLoss(zaAtt.doubleValue());
                            } else if (monitorDirection == MonitorDirection.IN
                                    && azAtt != null) {
                                otdrResultRecord.setBaseLoss(azAtt.doubleValue());
                            }
                        }
                        String monitorDirectionStr = getMonitorDirectionStr(isSource,
                                monitorDirection);
                        otdrResultRecord.setMonitorDirectionStr(monitorDirectionStr);
                    }

                }
            }
            OtdrResultRecord record = otdrDaoService.save(otdrResultRecord);
            log.debug("record otdr result record id:{}", record.getId());
            boolean finalIsSource = isSource;
            AsynchronousExecutor.execute(
                    () -> sendInProgressNotification(nodeId, nodeName, monitorName, monitorPortId,
                            monitorDirection, finalIsSource));
        } catch (Exception ex) {
            log.error("failed to record the otdr result ,the reason is: " + ex.getMessage(), ex);
        }
        return isSource;
    }

    private void sendInProgressNotification(String nodeId, String nodeName, String monitorName,
            String monitorPortId, MonitorDirection monitorDirection, Boolean isSource) {
        log.debug("send otdr scan in progress notification!");
        OtdrScanDetail otdrScanDetail = OtdrScanDetail.builder()
                .state(OtdrScanResultType.INPROGRESS).monitorName(monitorName)
                .nodeName(nodeName)
                .direction(OtdrUtils.getMonitorDirection(isSource, monitorDirection))
                .monitorPortId(monitorPortId).nodeId(nodeId).build();
        otdrNotificationService.sendOtdrScanStateNotification(otdrScanDetail);
    }

//    private String getMonitorDirectionStr(boolean isSource, MonitorDirection monitorDirection) {
//        log.debug("to get monitor direction str");
//        int sourceFlag = isSource ? 1 : 0;
//        int monitorDirectionFlag = 0;
//        if (isSource) {
//            monitorDirectionFlag = sourceFlag & monitorDirection.getIntValue();
//        } else {
//            monitorDirectionFlag = sourceFlag ^ monitorDirection.getIntValue();
//        }
//        return Objects.requireNonNull(OtdrMonitorDirection.getDirection(
//                monitorDirectionFlag)).getDirectionStr();
//    }
}
