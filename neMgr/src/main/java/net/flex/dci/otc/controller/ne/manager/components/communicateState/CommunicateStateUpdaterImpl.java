package net.flex.dci.otc.controller.ne.manager.components.communicateState;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.RegisterError.CONNECTION_TIME_OUT_ERROR;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.RegisterError.LOGIN_FAILED_ERROR;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.RegisterError.UNREACHABLE_ERROR;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.springframework.stereotype.Component;

/**
 * 2026/8/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class CommunicateStateUpdaterImpl implements CommunicateStateUpdater {

    private final PhyNodeDao phyNodeDao;


    @Override
    public void syncing(String neId) {
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId, CommunicationStatusType.Syncing);
    }

    @Override
    public void synced(String neId) {
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId, CommunicationStatusType.SyncFinished);
    }

    @Override
    public void syncFailed(String neId, Throwable t) {
        String message = t.getMessage();
        CommunicationStatusType communicationStatus = CommunicationStatusType.SyncFailed;
        if (message.contains(UNREACHABLE_ERROR)) {
            communicationStatus = CommunicationStatusType.IpUnreachable;
        } else if (message.contains(LOGIN_FAILED_ERROR)) {
            communicationStatus = CommunicationStatusType.LoginFail;
        } else if (message.contains(CONNECTION_TIME_OUT_ERROR)) {
            communicationStatus = CommunicationStatusType.EnumconnectionTimeOut;
        }
        log.info("update current ne :{} communicate status:{}", neId, communicationStatus);
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
                communicationStatus);
    }

    @Override
    public void syncFailed(String neId) {
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId, CommunicationStatusType.SyncFailed);
    }

    @Override
    public void notManaged(String neId) {

    }

    @Override
    public void broken(String nodeId) {
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(nodeId, CommunicationStatusType.Broken);
    }

    @Override
    public void loginFail(String neId) {
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId, CommunicationStatusType.LoginFail);

    }
}
