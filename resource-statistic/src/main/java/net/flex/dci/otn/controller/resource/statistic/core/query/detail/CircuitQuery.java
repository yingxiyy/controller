package net.flex.dci.otn.controller.resource.statistic.core.query.detail;

import static net.flex.dci.otn.controller.resource.statistic.core.utils.ResourceStatisticConstants.BATCH_SIZE;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.InventoryType;
import net.flex.dci.otn.controller.resource.statistic.core.manager.TunnelInventoryManager;
import net.flex.dci.otn.controller.resource.statistic.dto.query.BaseQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CircuitQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.export.TunnelCsv;
import net.flex.dci.otn.controller.resource.statistic.rest.TunnelDetail;
import net.flex.dci.otn.controller.resource.statistic.utils.ResourceConvertor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * 2026/7/21
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class CircuitQuery extends AbstractMaterialQuery<TunnelCsv> {

    private final TunnelInventoryManager inventoryManager;

    @Override
    protected Class<TunnelCsv> getClazz() {
        return TunnelCsv.class;
    }

    @Override
    public InventoryType inventoryScope() {
        return InventoryType.TUNNEL;
    }

    @Override
    public void queryStream(BaseQueryDTO baseQueryDTO, List<String> subnet,
            Consumer<List<?>> rowConsumer) {
        log.debug("query tunnel by baseQuery dto:{}", baseQueryDTO);
        CircuitQueryDTO queryParam = (CircuitQueryDTO) baseQueryDTO;
        List<String> siteLinkIds = queryParam.getSiteLink();
        List<String> refTunnelIds = resolveTunnelIds(subnet, siteLinkIds);
        long total = 0L;
        try (Stream<Tunnel> stream = tunnelDao.fetchTunnelInfo(
                refTunnelIds, subnet)) {
            Iterator<Tunnel> iterator = stream.iterator();
            List<Tunnel> batches = new ArrayList<>(BATCH_SIZE);
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

    private long flushBatch(List<Tunnel> batches, Consumer<List<?>> rowConsumer) {
        log.debug("flush batch the tunnel info");
        List<TunnelDetail> tunnelDetails = inventoryManager.enrichTunnels(batches);
        List<TunnelCsv> tunnelCsvs = ResourceConvertor.convert2TunnelCsv(tunnelDetails);
        rowConsumer.accept(tunnelCsvs);
        return batches.size();
    }


}
