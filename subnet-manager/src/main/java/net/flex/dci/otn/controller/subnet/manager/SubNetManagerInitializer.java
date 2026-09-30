package net.flex.dci.otn.controller.subnet.manager;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.ROOT_NODE_ID;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.ROOT_NODE_NAME;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.SYSTEM;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import org.springframework.stereotype.Component;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubNetManagerInitializer {

    private final SubNetTreeNodeDao subnetTreeNodeDao;


    public void initializing() {
        log.info("initializing subnet tree root node");
        Optional<SubNetTreeNode> rootTreeNode = subnetTreeNodeDao.findBySubNetId(ROOT_NODE_ID);
        if (rootTreeNode.isPresent()) {
            log.info("subnet tree root node initialized");
            return;
        }
        SubNetTreeNode subnetTreeNode = new SubNetTreeNode();
        subnetTreeNode.setSubNetId(ROOT_NODE_ID);
        subnetTreeNode.setName(ROOT_NODE_NAME);
        subnetTreeNode.setLevel(0);
        subnetTreeNode.setOrder(0);
        subnetTreeNode.setIsLeaf(false);
        List<String> rootPathIds = new ArrayList<>();
        rootPathIds.add(ROOT_NODE_ID);  // 根节点的pathIds只包含自己

        List<String> rootPathNames = new ArrayList<>();
        rootPathNames.add(ROOT_NODE_NAME);  // 根节点的pathNames只包含自己
        Long currentTime = System.currentTimeMillis();
        subnetTreeNode.setCreateTimestamp(currentTime);
        subnetTreeNode.setUpdateTimestamp(currentTime);
        subnetTreeNode.setCreateBy(SYSTEM);
        subnetTreeNode.setUpdateBy(SYSTEM);
        subnetTreeNode.setPathIds(rootPathIds);
        subnetTreeNode.setPathNames(rootPathNames);
        subnetTreeNodeDao.save(subnetTreeNode);
        log.info("initializing the subnet root node:{}", ROOT_NODE_NAME);
    }

}
