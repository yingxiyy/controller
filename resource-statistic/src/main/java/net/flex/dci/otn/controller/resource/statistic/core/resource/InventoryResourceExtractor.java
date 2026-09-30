package net.flex.dci.otn.controller.resource.statistic.core.resource;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class InventoryResourceExtractor {

    private final EquipmentResource equipmentResource;

    private final NeResource neResource;

    private final TerminationPointResource terminationPointResource;

    private final TransceiverResource transceiverResource;


    public List<String> extractRelativeNeIds(String siteId, List<String> planeId) {
        return neResource.extractRelativeNeIds(siteId, planeId);
    }

    public List<String> extractConnectionRelativeCardIds(String siteLinkId, String tunnelId) {
        return equipmentResource.extractRelativeCardIdsFromConnect(siteLinkId, tunnelId);
    }

    public List<String> extractConnectionRelativeTpIds(String siteLinkId, String tunnelId) {

        return terminationPointResource.extractConnectionRelativeTpIds(siteLinkId, tunnelId);
    }

    public List<String> extractConnectionRelativeTransceiverIds(String siteLinkId,
            String tunnelId) {
        return transceiverResource.extractConnectionRelativeTransceiverIds(siteLinkId, tunnelId);
    }

}
