package net.flex.dci.otc.controller.ne.manager.components.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/14/2025 11:59 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeManagerEventNotificationImpl implements NeManagerEventNotification {

    private final PhyNodeDao phyNodeDao;

    @Override
    public void notifyNeManageFailed(Exception exception) {
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().error(true).title(BroadCastConstant.MANAGE_NE)
                        .message(exception.getMessage())
                        .build());
    }

    @Override
    public void notifyNeResourceManageFailed(Exception exception) {
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.NE_RESOURCE_MANAGER).error(true)
                        .message(exception.getMessage()).build());
    }

    @Override
    public void notifyNeDataMergeSuccess(String neId) {
        log.debug("notify ne data merge success,neId:{}", neId);
        String friendlyName = phyNodeDao.getFriendlyName(neId);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.NE_RESOURCE_MANAGER).message(
                                String.format("success to merge data from : %s(%s)", friendlyName,
                                        neId))
                        .error(false).build());
    }


    @Override
    public void broadcastNeSyncing(String neId, String neName) {
        log.debug("send notification to current ne syncing,ne id:{}", neId);
//        String friendlyName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.MANAGE_NE).message(
                                String.format("start to synchronize ne : %s(%s)", neName,
                                        neId))
                        .error(false).build());
    }

    @Override
    public void notifyRegisterNeSuccess(String neId) {
        String friendlyName = phyNodeDao.getFriendlyName(neId);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.MANAGE_NE).message(
                                String.format("success to register ne : %s(%s)", friendlyName,
                                        neId))
                        .error(false).build());
    }

    @Override
    public void notifyUnregisterNeSuccess(String neId) {
        String friendlyName = phyNodeDao.getFriendlyName(neId);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.MANAGE_NE).message(
                                String.format("success to unregister ne : %s(%s)", friendlyName,
                                        neId))
                        .error(false).build());
    }
}
