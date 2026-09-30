package net.flex.dci.otn.controller.resource.statistic.core.query.detail;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otc.mongo.mdoel.lldp.ServiceInfoDto;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.LLdpInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.InventoryExportData;
import net.flex.dci.otn.controller.resource.statistic.dto.query.BaseQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CircuitQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;
import net.flex.dci.otn.controller.resource.statistic.export.LLDPCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;
import net.flex.dci.otn.controller.resource.statistic.utils.ResourceConvertor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.lldp.attributes.Lldp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * 2026/7/13
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class LLDPQuery extends AbstractMaterialQuery<LLDPCsv> {

    private final LLdpInventoryManager lLdpInventoryManager;


    @Override
    public InventoryType inventoryScope() {
        return InventoryType.LLDP;
    }

    @Override
    public InventoryExportData<LLDPCsv> queryData(UnifiedResourceQueryParam query) {
        log.debug("query data for the unified export request:{}", query);
        List<String> subnetIds = query.getSubnet();
        List<String> tunnelIds = query.getTunnelIds();
        List<ServiceInfoDto> serviceInfos = tunnelDao.fetchAllLLDPInfos(tunnelIds,
                subnetIds, new ArrayList<>());
        Map<String, TerminationPoint> terminationPointMap = batchGetTerminationPoint(serviceInfos);
//        List<LLDPInfo> lldpInfos = constructLLdpInfo(serviceInfos, terminationPointMap);
        return null;
    }

    @Override
    public InventoryExportData<?> queryData(BaseQueryDTO queryDTO,
            List<String> subnet) {
        return null;
    }


    @Override
    public void queryStream(BaseQueryDTO baseQueryDTO, List<String> subnet,
            Consumer<List<?>> rowConsumer) {
        log.debug("query data stream for the request query:{}", baseQueryDTO);
        CircuitQueryDTO circuitQueryParam = (CircuitQueryDTO) baseQueryDTO;
        List<String> siteLink = circuitQueryParam.getSiteLink();
        List<String> finalTunnelIds = resolveTunnelIds(subnet, siteLink);
        long total = 0L;
        try (Stream<LinkStateDto> stream = tunnelDao.fetchTunnelStateInfo(
                finalTunnelIds, subnet)) {
            Iterator<LinkStateDto> iterator = stream.iterator();
            List<LinkStateDto> batches = new ArrayList<>(BATCH_SIZE);
            while (iterator.hasNext()) {
                batches.add(iterator.next());
                if (batches.size() >= BATCH_SIZE) {
                    total += flushBatch(batches, rowConsumer);
                    batches = new ArrayList<>(BATCH_SIZE);
                }
            }
            if (!batches.isEmpty()) {
                total += flushBatch(batches, rowConsumer);
            }
        }
        log.info("tunnel stream export finished, total records:{}", total);

    }

    /**
     * fetch the tunnel lldp infos
     *
     * @param batches
     * @param rowConsumer
     * @return
     */
    private long flushBatch(List<LinkStateDto> batches, Consumer<List<?>> rowConsumer) {
        log.debug("flush batch the tunnel info");
        List<LLDPInfo> tunnelDetails = lLdpInventoryManager.retrieveAllTunnelLLdpInfos(batches);
        List<LLDPCsv> lldpCsvs = ResourceConvertor.convert2LLdpCsv(tunnelDetails);
        rowConsumer.accept(lldpCsvs);
        return batches.size();
    }


    private Lldp getTpLLdp(TerminationPoint tp) {
        Physical tpPhysical = tp.getAugmentation(
                TerminationPoint1.class).getPhysical();
        return tpPhysical.getLldp();
    }

    private Map<String, TerminationPoint> batchGetTerminationPoint(
            List<ServiceInfoDto> serviceInfos) {
        log.debug("batch get termination Point infos:{}", serviceInfos.size());
        Set<String> tpIds = serviceInfos.stream().flatMap(
                        serviceInfoDto -> Stream.of(serviceInfoDto.getSourceTpId(),
                                serviceInfoDto.getDestinationTpId()))
                .collect(Collectors.toSet());
        Set<String> neIds = tpIds.stream().map(PhysicalTpIdNamingRule::getNodeId)
                .collect(
                        Collectors.toSet());
        Map<String, Map<String, TerminationPoint>> neTpMap = terminationPointDao.batchGetOpNeTpMap(
                neIds, tpIds);
        return neTpMap.values().stream()
                .flatMap(innerMap -> innerMap.entrySet().stream())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1
                ));
    }

    @Override
    protected Class<LLDPCsv> getClazz() {
        return LLDPCsv.class;
    }
}
