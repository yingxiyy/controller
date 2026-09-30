package net.flex.dci.otc.controller.otdr.scan;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.otdr.model.OTDRScanParameters;
import net.flex.dci.otc.controller.otdr.model.terminationPoint.RealOTDRScanInfo;
import net.flex.dci.otc.controller.otdr.notification.OtdrScanStateNotification;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import net.flex.dci.otc.controller.scanner.port.helper.ScannerPortHelper;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/1/2023 4:16 PM
 */
@Component
@Slf4j
public class TelecomModelOTDRScanner extends AbstractOTDRScanner {

    private final PhyLinkDao phyLinkDao;

    private final ScannerPortHelper scannerPortHelper;

    public TelecomModelOTDRScanner(OTDRRpc otdrRpc, TerminationPointDao terminationPointDao,
            PhyLinkDao phyLinkDao, OtdrDaoService otdrDaoService,
            OtdrScanStateNotification otdrNotificationService, PhyLinkDao phyLinkDao1,
            ScannerPortHelper scannerPortHelper) {
        super(otdrRpc, terminationPointDao, phyLinkDao, otdrDaoService, otdrNotificationService);
        this.phyLinkDao = phyLinkDao1;
        this.scannerPortHelper = scannerPortHelper;
    }


    @Override
    public NeYangModel supportYangModel() {
        return NeYangModel.ChinaTelecom;
    }

    @Override
    public String startOTDR(Node ne, String monitorName, String monitorPortId,
            ScanMode scanMode, MonitorDirection monitorDirection,
            OTDRScanParameters otdrScanParameters,
            Adapter adapter, Link otsLink,
            TaskInfoMessage taskInfoMessage) {
        log.info(
                "china telecom otdr scan job start,the current business port is :{},ref node id is:{}",
                monitorPortId, ne.getNodeId().getValue());
        TerminationPoint monitorPort = getCurrentMonitorPort(ne.getTerminationPoint(),
                monitorPortId);
        RealOTDRScanInfo realOTDRScanInfo = getRealOTDRMonitorPortAndNe(ne, monitorPort,
                monitorDirection);
        return null;
    }

    private RealOTDRScanInfo getRealOTDRMonitorPortAndNe(Node ne, TerminationPoint monitorPort,
            MonitorDirection monitorDirection) {
        log.debug("get real otdr scan port and ne ");
        String friendlyName = monitorPort.getAugmentation(TerminationPoint1.class).getPhysical()
                .getFriendlyName();
        String monitorPortId = monitorPort.getTpId().getValue();
        TerminationPoint otdrPort = scannerPortHelper.getRealOTDRScanPortFromBusinessCard(
                ne, monitorPort.getTpId().getValue(), monitorDirection);
        List<Link> links = phyLinkDao.listAllPhyLinksUnderTp(
                otdrPort.getTpId().getValue());
        Optional<Link> otdrLinkOptional = links.stream()
                .filter(link -> link.getAugmentation(Link1.class).getPhysical().getLinkType()
                        .equals(
                                LinkType.OtdrLink)).findFirst();
        if (!otdrLinkOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.NO_OTDR_LINK_ERROR, String.format(
                    "The ne %s monitor port:%s should be established a fiber connection before proceeding with OTDR",
                    friendlyName, monitorPortId));
        }
        Link otdrLink = otdrLinkOptional.get();
        List<String> refLinkTpIds = Arrays.asList(otdrLink.getSource().getSourceTp().getValue(),
                otdrLink.getDestination().getDestTp()
                        .getValue());
        String realOtdrPortId = refLinkTpIds.stream()
                .filter(tpId -> !tpId.equals(otdrPort.getTpId().getValue())).findFirst().get();
        String realNeId = PhysicalTpIdNamingRule.getNodeId(realOtdrPortId);
        return RealOTDRScanInfo.builder().OTDRMonitorPort(realOtdrPortId).node(realNeId).build();
    }

    /**
     * get current monitor port from business card
     *
     * @param terminationPoint
     * @param monitorPortId
     * @return
     */
    private TerminationPoint getCurrentMonitorPort(List<TerminationPoint> terminationPoint,
            String monitorPortId) {
        log.debug("get request scan otdr port,the scan monitor port id is:{}", monitorPortId);
        Optional<TerminationPoint> terminationPointOptional = terminationPoint.stream()
                .filter(tp -> tp.getTpId().getValue().equals(monitorPortId)).findAny();
        if (!terminationPointOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the monitor port id :%s is not existed", monitorPortId));
        }
        return terminationPointOptional.get();
    }


}
