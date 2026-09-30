package net.flex.dci.otn.controller.subnet.manager.component.validator;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.ROOT_NODE_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo.MigrationSiteLink;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo.MigrationTunnel;
import net.flex.dci.otn.controller.subnet.manager.dto.SubNetMigrationReq;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2026/2/9
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubnetRequestValidatorImpl implements SubnetRequestValidator {

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final SiteLinkDao siteLinkDao;

    private final TunnelDao tunnelDao;

    @Override
    public void validatorMigration(SubNetMigrationReq req) {
        MigrationTunnelInfo migrationTunnelInfo = req.getMigrationTunnel();
        MigrationSiteLinkInfo migrationSiteLinkInfo = req.getMigrationSiteLink();
        if (migrationTunnelInfo == null && migrationSiteLinkInfo == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "migration siteLink or tunnel should not be null");
        }

        Set<String> allTargetSubnets = new HashSet<>();
        List<String> allSiteLinkIds = new ArrayList<>();
        List<String> allTunnelIds = new ArrayList<>();

        if (migrationSiteLinkInfo != null && !migrationSiteLinkInfo.getMigration().isEmpty()) {
            List<MigrationSiteLink> migrationSiteLink = migrationSiteLinkInfo.getMigration();
            migrationSiteLink.forEach(siteLink -> {
                String siteLinkId = siteLink.getSiteLinkId();
                String targetSubnet = siteLink.getTargetSubnet();

                if (!StringUtils.hasText(siteLinkId)) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "SiteLink ID is required.");
                }
                if (!StringUtils.hasText(targetSubnet)) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "Target subnet is required for siteLink: " + siteLinkId);
                }
                if (targetSubnet.equals(ROOT_NODE_ID)) {
                    throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                            "Root node does not support subnet migration operation.");
                }

                allSiteLinkIds.add(siteLinkId);
                allTargetSubnets.add(targetSubnet);

            });
        }

        if (migrationTunnelInfo != null && !migrationTunnelInfo.getMigration().isEmpty()) {
            List<MigrationTunnel> migrationTunnels = migrationTunnelInfo.getMigration();
            migrationTunnels.forEach(tunnelMigration -> {
                String tunnelId = tunnelMigration.getTunnelId();
                String targetSubnet = tunnelMigration.getTargetSubnet();

                if (!StringUtils.hasText(tunnelId)) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "Tunnel ID is required.");
                }
                if (!StringUtils.hasText(targetSubnet)) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "Target subnet is required for tunnel: " + tunnelId);
                }
                if (targetSubnet.equals(ROOT_NODE_ID)) {
                    throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                            "Root node does not support subnet migration operation.");
                }

                allTunnelIds.add(tunnelId);
                allTargetSubnets.add(targetSubnet);
            });
        }

        List<SubNetTreeNode> subNetTreeNodes = subNetTreeNodeDao.getSubNetBySubnetIds(
                new ArrayList<>(allTargetSubnets));
        Set<String> existingSubnetIds = subNetTreeNodes.stream()
                .map(SubNetTreeNode::getSubNetId)
                .collect(Collectors.toSet());

        List<String> nonExistentSubnets = allTargetSubnets.stream()
                .filter(subnetId -> !existingSubnetIds.contains(subnetId))
                .collect(Collectors.toList());

        if (!nonExistentSubnets.isEmpty()) {
            log.error("subnet migration failed not existed subnet: {}", nonExistentSubnets);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "subnet not existed: " + String.join(", ", nonExistentSubnets));
        }

        if (!allSiteLinkIds.isEmpty()) {
            List<String> existedSiteLinkIds = siteLinkDao.existLinkBySiteLinkIds(allSiteLinkIds);
            List<String> notExistSiteLinkIds = allSiteLinkIds.stream()
                    .filter(siteLinkId -> !existedSiteLinkIds.contains(siteLinkId))
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
            if (!notExistSiteLinkIds.isEmpty()) {
                String errorMsg = String.format("SiteLink ids: %s are not found.",
                        String.join(",", notExistSiteLinkIds));
                log.warn("{} during subnet migration, siteLinkIds: {}", errorMsg, allSiteLinkIds);
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, errorMsg);
            }
        }

        if (!allTunnelIds.isEmpty()) {
            List<String> tunnelIds = tunnelDao.retrieveAllTunnelIdsByTunnelIds(allTunnelIds);
            List<String> notExistTunnelIds = allTunnelIds.stream()
                    .filter(tunnelId -> !tunnelIds.contains(tunnelId))
                    .collect(Collectors.toList());
            if (!notExistTunnelIds.isEmpty()) {
                String errorMsg = String.format("tunnel ids: %s are not found.",
                        String.join(",", notExistTunnelIds));
                log.warn("{} during subnet migration, Tunnel : {}", errorMsg, allSiteLinkIds);
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, errorMsg);
            }
        }

    }
}
