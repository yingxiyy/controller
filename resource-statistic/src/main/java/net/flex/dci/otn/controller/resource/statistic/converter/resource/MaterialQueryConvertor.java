package net.flex.dci.otn.controller.resource.statistic.converter.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.query.MaterialQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.MaterialQueryDisplayDto;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.QueryTaskDisplayDto;
import net.flex.dci.otn.controller.resource.statistic.enums.DeviceType;
import net.flex.dci.otn.controller.resource.statistic.enums.MaterialCategory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class MaterialQueryConvertor extends AbstractResourceQueryConvertor {

    @Override
    public QueryTaskDisplayDto resolveDisplay(UnifiedQueryParam queryParam) {
        log.debug("resolve the circuit query display :{}",
                queryParam.getQueryCondition().getMaterialQuery());
        MaterialQueryDTO materialQueryDTO = queryParam.getQueryCondition().getMaterialQuery();
        List<String> subnet = queryParam.getSubnet();
        List<String> subnetNames = getSubnetNames(subnet);
        DeviceType deviceType = materialQueryDTO.getDeviceType();
        MaterialCategory materialCategory = materialQueryDTO.getMaterialCategory();
        List<String> siteNames = getSiteNames(materialQueryDTO.getSite());
        List<String> deviceNames = getDeviceNames(materialQueryDTO.getDeviceId());
        return MaterialQueryDisplayDto.builder().materialCategory(materialCategory.name())
                .deviceType(deviceType.name())
                .subnetNames(subnetNames)
                .siteNames(siteNames)
                .deviceNames(deviceNames)
                .build();
    }

    private List<String> getDeviceNames(List<String> deviceId) {
        log.debug("get device names by neId:{}", deviceId);
        if (CollectionUtils.isEmpty(deviceId)) {
            return new ArrayList<>();
        }
        List<Node> lightNode = phyNodeDao.listLightConfigPhyNodeByIds(
                deviceId);
        List<String> nodeNames = lightNode.stream()
                .map(node -> node.getAugmentation(Node1.class).getPhysical().getFriendlyName())
                .collect(
                        Collectors.toList());
        return nodeNames;
    }

    private List<String> getSiteNames(List<String> site) {
        log.debug("get site name by site id:{}", site);
        if (CollectionUtils.isEmpty(site)) {
            return new ArrayList<>();
        }
        List<Node> sites = siteNodeDao.listAllLightNodeByIds(site);
        List<String> siteNames = sites.stream().map(siteNode -> siteNode.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                        .getSite().getFriendlyName())
                .collect(Collectors.toList());
        return siteNames;
    }

    @Override
    public ResourceQueryType resourceQueryType() {
        return ResourceQueryType.Material;
    }
}
