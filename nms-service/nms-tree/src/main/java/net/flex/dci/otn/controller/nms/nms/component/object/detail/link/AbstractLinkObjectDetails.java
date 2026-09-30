package net.flex.dci.otn.controller.nms.nms.component.object.detail.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 8/27/2023 8:35 PM
 */
@Slf4j
public abstract class AbstractLinkObjectDetails implements LinkRefObjectDetails {

    @Autowired
    protected DciTopologyCacheManager dciTopologyCacheManager;

    @Autowired
    protected SubNetTreeNodeDao subNetTreeNodeDao;

    protected SubNetTreeNode getSubnet(String subnetId) {
        log.debug("get subnet tree node by id:{}", subnetId);
        if (!StringUtils.hasText(subnetId)) {
            return null;
        }
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId)
                .orElseThrow(() -> new CommonException(
                        CommonExceptionType.NOT_FOUND_ERROR,
                        "subnet tree node " + subnetId + " is not found "));
        return subNetTreeNode;
    }
}
