package net.flex.dci.otc.controller.otdr.components.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.otdr.components.PhyLinkAttenuation;
import net.flex.dci.otc.controller.otdr.model.link.PhyLinkInfo;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 9/7/2023 2:11 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkAttenuationImpl implements PhyLinkAttenuation {

    private final PhyLinkDao phyLinkDao;

    @Override
    public void setBasePhyLinkAttenuationValue(PhyLinkInfo linkInfo, String monitorTpId, Double loss, StartOtdrParameter.MonitorDirection monitorDirection) {
        log.debug("set base phy link attenuation value");
        String linkId = linkInfo.getLinkId();
        phyLinkDao.updateLinkProviderZAAttenuation(linkId, loss);
        phyLinkDao.updateLinkProviderAZAttenuation(linkId, loss);
//        String srcTp = linkInfo.getSrcTpId();
//        String destTp = linkInfo.getDestTpId();
//        if (srcTp.equals(monitorTpId)) {
//            setBaseAttenuationForPhyLinkBySrcTpAndDirection(linkId, loss, monitorDirection);
//        } else if (destTp.equals(monitorTpId)) {
//            setBaseAttenuationForPhyLinkByDestTpAndDirection(linkId, loss, monitorDirection);
//        }
    }

    /**
     * set base attenuation for phy link by dest tp and direction
     *
     * @param linkId
     * @param loss
     * @param monitorDirection
     */
//    private void setBaseAttenuationForPhyLinkByDestTpAndDirection(String linkId, Double loss, StartOtdrParameter.MonitorDirection monitorDirection) {
//        log.debug("set src attenuation by dest tp ");
//        if (monitorDirection.equals(StartOtdrParameter.MonitorDirection.OUT)) {
//            //z->a
//            phyLinkDao.updateLinkProviderZAAttenuation(linkId, loss);
//        } else {
//            //a->z
//            phyLinkDao.updateLinkProviderAZAttenuation(linkId, loss);
//        }
//    }
//
//    private void setBaseAttenuationForPhyLinkBySrcTpAndDirection(String linkId, Double loss, StartOtdrParameter.MonitorDirection monitorDirection) {
//        log.debug("set src attenuation by source tp ");
//        if (monitorDirection.equals(StartOtdrParameter.MonitorDirection.OUT)) {
//            //a-z
//            phyLinkDao.updateLinkProviderAZAttenuation(linkId, loss);
//        } else {
//            //z->a
//            phyLinkDao.updateLinkProviderZAAttenuation(linkId, loss);
//        }
//    }
}
