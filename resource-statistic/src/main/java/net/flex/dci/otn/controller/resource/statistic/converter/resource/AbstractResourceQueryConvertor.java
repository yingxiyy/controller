package net.flex.dci.otn.controller.resource.statistic.converter.resource;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.resource.statistic.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractResourceQueryConvertor implements IResourceQueryConvertor {

    @Autowired
    protected SubNetTreeNodeDao subNetTreeNodeDao;

    @Autowired
    protected SiteLinkDao siteLinkDao;

    @Autowired
    protected PhyNodeDao phyNodeDao;
    @Autowired
    protected SiteNodeDao siteNodeDao;


    protected List<String> getSubnetNames(List<String> subnet) {
        log.debug("resolve the subnet the subnet:{}", subnet);
        if (subnet == null || subnet.isEmpty()) {
            subnet.add(Constants.DEFAULT_SUBNET_ID);
        }
        List<SubNetTreeNode> subnetTrees = subNetTreeNodeDao.getSubNetBySubnetIds(
                subnet);
        List<String> subnetNames = subnetTrees.stream().map(SubNetTreeNode::getName).collect(
                Collectors.toList());
        return subnetNames;
    }
}
