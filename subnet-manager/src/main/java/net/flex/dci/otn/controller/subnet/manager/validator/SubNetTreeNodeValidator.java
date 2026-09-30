package net.flex.dci.otn.controller.subnet.manager.validator;

import java.util.Optional;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.CreateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.UpdateNodeReq;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubNetTreeNodeValidator {

    private final SubNetTreeNodeDao subnetTreeNodeDao;

    private final PhyNodeDao phyNodeDao;

    private final SiteLinkDao siteLinkDao;

    private final TunnelDao tunnelDao;

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9\\u4e00-\\u9fa5\\-_\\s]{1,50}$");

    public void validateCreateSubNetNode(CreateNodeReq createNodeReq) {
        log.debug("validate create subnet node request :{}", createNodeReq);
        String name = createNodeReq.getName();
        name = name.trim();
        if (!StringUtils.hasText(name)) {
            log.error("create  subnet  name should not be null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "create  subnet name should not be null");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Subnet name length must be between 2 and 50 characters.");
        }

        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Subnet names can only contain Chinese characters, English letters, digits, underscores, hyphens, and spaces.");
        }
        String parentNodeId = createNodeReq.getParentSubNetId();
        if (!StringUtils.hasText(parentNodeId)) {
            log.error("create subnet node relative parentNode should not be null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "create  subnet node parentId should not be null");
        }
        boolean isExisted = subnetTreeNodeDao.existsBySubNetId(parentNodeId);
        if (!isExisted) {
            log.error("create subnet node relative parentNode :{} is not existed", parentNodeId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "create subnet node relative parentNode is not existed");
        }


    }

    public void validateUpdateNodeReq(UpdateNodeReq updateNodeReq) {
        log.debug("validate update node req:{}", updateNodeReq);
        String name = updateNodeReq.getName();
        name = name.trim();
        String subNetId = updateNodeReq.getSubnetId();
        Optional<SubNetTreeNode> subNetTreeNode = subnetTreeNodeDao.findBySubNetId(subNetId);
        if (!subNetTreeNode.isPresent()) {
            log.error("current update node :{} is not existed", subNetId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "update subnet node:" + subNetId + " is not existed in db");
        }
        SubNetTreeNode subNet = subNetTreeNode.get();
        String parentId = subNet.getParentId();
        if (!StringUtils.hasText(name)) {
            log.error("update  subnet node name should not be null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "update subnet node node should not be null");
        }
        if (name.length() < 2 || name.length() > 50) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Subnet name length must be between 2 and 50 characters.");
        }

        if (!VALID_NAME_PATTERN.matcher(name).matches()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Subnet names can only contain Chinese characters, English letters, digits, underscores, hyphens, and spaces.");
        }

        if (isNameDuplicateInSameParent(parentId, name, subNetId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "A node named '" + name + "' already exists under the same parent node.");
        }


    }

    private boolean isNameDuplicateInSameParent(String parentId, String name, String subNetId) {
        log.debug("is name duplicated in same parent:{} name:{} subnetId:{}", parentId, name,
                subNetId);
        boolean duplicated = subnetTreeNodeDao.isNameDuplicateInSameParent(parentId, name,
                subNetId);
        return duplicated;
    }

    public void checkHaveRelativeResource(String subnetId) {
        log.debug("check current subnet have relative resource subnet is:{}", subnetId);

        boolean exitsTunnel = tunnelDao.existsByPlaneId(subnetId);
        boolean exitsSiteLink = siteLinkDao.existsByPlaneId(subnetId);
        boolean exitsPhyNode = phyNodeDao.existsByPlaneId(subnetId);
        if (exitsPhyNode || exitsSiteLink || exitsTunnel) {
            SubNetTreeNode subnet = subnetTreeNodeDao.findBySubNetId(subnetId).orElse(null);
            if (subnet == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + subnetId + " is not found");
            }
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "subnet[" + subnet.getName() + "] has relative resources, cannot delete");
        }
    }
}
