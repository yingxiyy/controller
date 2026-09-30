package net.flex.dci.otn.controller.subnet.manager.service;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.ROOT_NODE_ID;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.UNASSIGN_SUBNET;
import static net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils.getOperator;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dto.BulkUpdateResult;
import net.flex.dci.otc.mongo.dto.DeleteResultDto;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.component.OutputConvertorImpl;
import net.flex.dci.otn.controller.subnet.manager.component.SnowflakeIdGenerator;
import net.flex.dci.otn.controller.subnet.manager.component.notification.SubnetChangeNotify;
import net.flex.dci.otn.controller.subnet.manager.component.resource.SubnetChangeResourceSynchronizer;
import net.flex.dci.otn.controller.subnet.manager.component.taskinfo.SubnetTaskInfoBuilder;
import net.flex.dci.otn.controller.subnet.manager.dto.CreateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.UpdateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.output.DeleteResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.FlatNodeDto;
import net.flex.dci.otn.controller.subnet.manager.dto.output.PageResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.SubNetNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.TreeNodeSearchResultDto;
import net.flex.dci.otn.controller.subnet.manager.validator.SubNetTreeNodeValidator;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class SubTreeNodeServiceImpl implements SubTreeNodeService {

    private final SubNetTreeNodeValidator subNetTreeNodeValidator;

    private final SnowflakeIdGenerator idGenerator;

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final OutputConvertorImpl outputConvertor;

    private final SubnetChangeResourceSynchronizer subnetChangeResourceSynchronizer;

    private final SubnetTaskInfoBuilder subnetTaskInfoBuilder;

    private final SubnetChangeNotify subnetChangeNotify;


    @Override
    public SubNetNode createSubNetNode(CreateNodeReq createNodeReq, HttpServletRequest request) {
        log.info("create subNet node the request createNodeRequest parentId is:{} and name is:{}",
                createNodeReq.getParentSubNetId(), createNodeReq.getName());
        String author = getOperator(request);
        log.debug("create author is:{} createTimestamp is:{}", author, System.currentTimeMillis());
        subNetTreeNodeValidator.validateCreateSubNetNode(createNodeReq);
        String parentSubNetId = createNodeReq.getParentSubNetId();
        String subNetName = createNodeReq.getName().trim();
        SubNetTreeNode subNetTreeNode = recordNewTreeNode(parentSubNetId, subNetName, author);
        SubNetNode subNetNode = outputConvertor.convertSubNetTreeNode2SubNetNode(subNetTreeNode);
        return subNetNode;
    }

    @Override
    public SubNetNode buildAllTree(String parentId) {
        log.info("get current tree node total sub tree the parentId:{}", parentId);
        if (null == parentId) {
            return buildFullTree();
        }
        SubNetTreeNode rootNode = subNetTreeNodeDao.findBySubNetId(parentId)
                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node is not existed"));
        List<SubNetTreeNode> allDescendants = subNetTreeNodeDao.getAllDescendants(
                rootNode.getSubNetId());
        Map<String, SubNetTreeNode> nodeMap = allDescendants.stream()
                .collect(Collectors.toMap(SubNetTreeNode::getId,
                        Function.identity()));
        return buildTreeNode(rootNode, nodeMap);
    }

    @Override
    public List<SubNetNode> getChildrenByParentId(String parentId) {
        log.info("get current tree node children the sub tree node is:{}", parentId);
        List<SubNetTreeNode> children = _getChildrenByParentId(parentId);
        return outputConvertor.convertSubNetTreeNodes2SubNetNodes(children);
    }

    @Override
    public PageResult<FlatNodeDto> getFlatListPaged(String parentId, String keyword, int page,
            int size, String sortBy, String sortOrder) {
        log.info(
                "get flat list paged,the parentId:{} keyword:{} page:{} size:{} sortBy:{} sortOrder:{}",
                parentId, keyword, page, size, sortBy, sortOrder);
        Page<SubNetTreeNode> subNetTreeNodePage = subNetTreeNodeDao.getFlatListPaged(parentId,
                keyword, page, size, sortBy, sortOrder);
        PageResult<FlatNodeDto> flatNodeDtoPageResult = outputConvertor.convert2FlatNodePaged(
                subNetTreeNodePage);
        return flatNodeDtoPageResult;
    }


    @Override
    public List<TreeNodeSearchResultDto> searchNodes(String keyword) {
        log.info("search nodes by keyword:{}", keyword);
        if (!StringUtils.hasText(keyword)) {
            log.warn("current keyword is null,do nothing");
            return Collections.emptyList();
        }
        List<SubNetTreeNode> subNetTreeNodes = searchNodeByKeyword(keyword);
        return outputConvertor.convert2SearchResults(subNetTreeNodes);
    }

    @Override
    public SubNetNode updateNodeNameWithPath(UpdateNodeReq updateNodeReq,
            HttpServletRequest request) {
        String author = getOperator(request);
        log.info("start to update node name with path,the request is :{} author is:{}",
                updateNodeReq, author);
        subNetTreeNodeValidator.validateUpdateNodeReq(updateNodeReq);
        String newName = updateNodeReq.getName();
        String subnetId = updateNodeReq.getSubnetId();
        SubNetTreeNode updateSubNetTreeNode = updateNodeName(newName, subnetId, author);
        return outputConvertor.convertSubNetTreeNode2SubNetNode(updateSubNetTreeNode);
    }


    @Override
    public DeleteResult deleteSubNetNode(String subnetId, boolean force,
            HttpServletRequest request) {
        log.info("delete sub net node the subnet id:{}", subnetId);
        String author = getOperator(request);
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId).orElseThrow(
                () -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + subnetId + " is not found"));
        //todo:have relative service or not
        subNetTreeNodeValidator.checkHaveRelativeResource(subnetId);

        if (subNetTreeNode.getSubNetId().equals(ROOT_NODE_ID)) {
            log.error("current sub net tree node is root node,do not support to delete");
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "Cannot delete root subnet nodes.");
        }
        List<SubNetTreeNode> descendants = subNetTreeNodeDao.getAllDescendants(subnetId);
        if (!CollectionUtils.isEmpty(descendants) && descendants.size() > 1) {
            log.error("current subnet tree node is not leaf have child which can't be deleted");
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "Subnet deletion blocked due to existing child subnets.");

        }
        String subNetName = subNetTreeNode.getName();
        String parentId = subNetTreeNode.getParentId();
        TaskInfoMessage taskInfoMessage = subnetTaskInfoBuilder.buildDeleteSubnetTaskInfoMessage(
                author, subNetName, subnetId);
        try {
            DeleteResult deleteResult = new DeleteResult();
            deleteResult.setSubnetId(subnetId);
            deleteResult.setName(subNetName);
            deleteResult.setLeaf(subNetTreeNode.getIsLeaf());
            deleteResult = deleteSingleLeafSubnet(subnetId, deleteResult);
            SubNetTreeNode parentSubnetNode = null;
            if (parentId != null && deleteResult.isSuccess()) {
                parentSubnetNode = updateParentLeafStatus(parentId);
            }
            subnetChangeNotify.sendSubnetDeleteNotify(parentSubnetNode, subNetTreeNode);
            log.info("subnet delete success:{}", deleteResult);
            subnetTaskInfoBuilder.buildSuccessRemoveSubnetTaskInfo(taskInfoMessage, deleteResult);
            return deleteResult;
        } catch (Exception ex) {
            log.error("failed to delete the subnet id:{} error:{}", subnetId, ex.getMessage(), ex);
            subnetTaskInfoBuilder.buildFailedRemoveSubnetTaskInfo(taskInfoMessage, ex.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to delete the subnet:" + subNetName + " the reason is:"
                            + ex.getMessage(), ex);
        }

    }

    @Override
    public List<SubNetNode> buildAllTreeWithUnassign(String parentId) {

        SubNetNode realTree = buildAllTree(parentId);
        //add unassign subnet
        SubNetNode unassignedNode = new SubNetNode();
        unassignedNode.setSubnetId(UNASSIGN_SUBNET);
        unassignedNode.setName(UNASSIGN_SUBNET);
        unassignedNode.setChildren(new ArrayList<>());
        unassignedNode.setLevel(0);
        List<SubNetNode> filterTree = new ArrayList<>();
        filterTree.add(unassignedNode);
        if (realTree != null) {
            filterTree.add(realTree);
        }
        return filterTree;
    }

    /**
     * update parentLeaf status
     *
     * @param parentId
     */
    private SubNetTreeNode updateParentLeafStatus(String parentId) {
        log.info("update parent leaf status parent id is:{}", parentId);
        try {
            SubNetTreeNode parentNode = subNetTreeNodeDao.findBySubNetId(parentId)
                    .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "current subnet node  " + parentId + " is not found"));
            boolean hasDescendants = subNetTreeNodeDao.hasDescendants(parentId);
            if (!hasDescendants) {
                parentNode.setIsLeaf(true);
                subNetTreeNodeDao.save(parentNode);
                //todo:send notification
            }
            return parentNode;
        } catch (Exception e) {
            log.error("failed to update the parent subnet status，parentId: {}", parentId, e);
            return null;
        }
    }

    /**
     * delete single leaf subnet
     *
     * @param subnetId
     * @param result
     * @return
     */
    private DeleteResult deleteSingleLeafSubnet(String subnetId, DeleteResult result) {
        log.info("start delete subnet id:{}", subnetId);
        DeleteResultDto dbDeleteResult = subNetTreeNodeDao.deleteSubnetBySubnetId(
                subnetId);

        boolean selfDeleted = dbDeleteResult.getDeletedCount() > 0;
        result.setSuccess(selfDeleted);
        result.setSelfDeleted(selfDeleted);
        result.setTotalDeleted(selfDeleted ? 1 : 0);
        result.setMessage(selfDeleted ? "subnet delete success" : "subnet delete failed");
        return result;
    }

    private SubNetTreeNode updateNodeName(String newName, String subnetId, String author) {
        log.debug("modify subnet new name:{} subnetId:{}", newName, subnetId);
        TaskInfoMessage taskInfoMessage = subnetTaskInfoBuilder.buildModifySubnetNameTaskInfo(
                author,
                newName, subnetId);
        try {
            SubNetTreeNode newNode = doUpdateNodeName(newName, subnetId, author);
            subnetTaskInfoBuilder.buildSuccessModifySubNetTaskInfo(taskInfoMessage, newNode);
            return newNode;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            subnetTaskInfoBuilder.buildFailedModifySubNetTaskInfo(taskInfoMessage, errorMsg);
            if (e instanceof CommonException) {
                throw e;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "modify SubNetTreeNode failed: " + errorMsg, e);
            }
        }
    }

    private SubNetTreeNode doUpdateNodeName(String newName, String subnetId, String author) {
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId).orElseThrow(
                () -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + subnetId + " is not found"));
        String beforeUpdate = subNetTreeNode.toString();
        try {
            SubNetTreeNode updateSingleNode = updateSingleNode(subNetTreeNode, author, newName);
            updateDescendants(subNetTreeNode, subNetTreeNode.getName(), newName, author);
            logOperation(author, subnetId, subNetTreeNode.getName(), newName, beforeUpdate,
                    updateSingleNode.toString());
            return updateSingleNode;
        } catch (Exception ex) {
            log.error("update subnet failed,subnetId:{},newName:{},error:{}", subnetId, newName,
                    ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Update subNet node name failed the reason is:" + ex.getMessage());
        }
    }

    @Transactional(value = "mongoTransactionManager", rollbackFor = Exception.class, propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public SubNetTreeNode updateSingleNode(SubNetTreeNode subNetTreeNode, String author,
            String newName) {
        log.debug("update single node name the subnetId:{} author is:{}",
                subNetTreeNode.getSubNetId(), author);
        List<String> fullPathNames = subNetTreeNode.getPathNames();
        List<String> fullPathIds = subNetTreeNode.getPathIds();
        for (int i = 0; i < fullPathIds.size(); i++) {
            if (fullPathIds.get(i).equals(subNetTreeNode.getSubNetId())
                    && i < fullPathNames.size()) {
                fullPathNames.set(i, newName);
                break;
            }
        }
        SubNetTreeNode updateSubNetTreeNode = JSON.parseObject(JSON.toJSONString(subNetTreeNode),
                SubNetTreeNode.class);
        updateSubNetTreeNode.setPathNames(fullPathNames);
        updateSubNetTreeNode.setUpdateBy(author);
        updateSubNetTreeNode.setUpdateTimestamp(System.currentTimeMillis());
        updateSubNetTreeNode.setName(newName);
        SubNetTreeNode updateSubnetTreeNode = subNetTreeNodeDao.save(updateSubNetTreeNode);
        String oldName = subNetTreeNode.getName();
        subnetChangeNotify.sendSubnetModifyNotify(updateSubnetTreeNode);
        subnetChangeResourceSynchronizer.synchronizeUpdateSubnetName(
                subNetTreeNode.getSubNetId(),
                oldName, newName, author);
        return updateSubnetTreeNode;
    }

    private void updateDescendants(SubNetTreeNode subNetTreeNode, String oldName, String newName,
            String author) {
        boolean hasDescendants = subNetTreeNodeDao.hasDescendants(subNetTreeNode.getSubNetId());
        if (hasDescendants) {
            BulkUpdateResult bulkUpdate = subNetTreeNodeDao.updateDescendantsPathNames(
                    subNetTreeNode.getSubNetId(), oldName, newName, author);
            log.info("bulk update descendants matched:{} modified:{} duration:{} ms",
                    bulkUpdate.getMatchedCount(), bulkUpdate.getModifiedCount(),
                    bulkUpdate.getDuration());
            List<SubNetTreeNode> descendants = subNetTreeNodeDao.getAllDescendants(
                    subNetTreeNode.getSubNetId());
            descendants.forEach(subnetChangeNotify::sendSubnetModifyNotify);
        } else {
            log.info("no descendants need to update");
        }
    }

    /**
     * record operation log
     */
    private void logOperation(String author, String nodeId, String oldName,
            String newName, String beforeUpdate, String afterUpdate) {

        log.info("node update success - author: {}, subNet ID: {}, oldName: {}, newName: {}",
                author, nodeId, oldName, newName);

        // 记录详细变更
        log.debug("update detail - before: {}, after: {}", beforeUpdate, afterUpdate);
    }


    /**
     * search node by key word
     *
     * @param keyword
     * @return
     */
    private List<SubNetTreeNode> searchNodeByKeyword(String keyword) {
        log.debug("searchNode by keyword:{}", keyword);
        List<SubNetTreeNode> subNetTreeNodes = subNetTreeNodeDao.searchNodesByKeywordRegex(keyword);
        return subNetTreeNodes;
    }

    private List<SubNetTreeNode> _getChildrenByParentId(String parentId) {
        List<SubNetTreeNode> children = new ArrayList<>();
        if (parentId == null) {
            parentId = ROOT_NODE_ID;
        }
        children = subNetTreeNodeDao.findByParentIdOrderByOrderAsc(parentId);
        return children;
    }

    private SubNetNode buildFullTree() {
        List<SubNetTreeNode> allTreeNodes = subNetTreeNodeDao.findAll();
        SubNetTreeNode root = allTreeNodes.stream().filter(node -> node.getParentId() == null)
                .findFirst().orElseThrow(
                        () -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                "can not find the root node"));
        Map<String, SubNetTreeNode> nodeMap = allTreeNodes.stream()
                .collect(Collectors.toMap(SubNetTreeNode::getId,
                        Function.identity()));
        return buildTreeNode(root, nodeMap);
    }

    private SubNetNode buildTreeNode(SubNetTreeNode root, Map<String, SubNetTreeNode> nodeMap) {
        SubNetNode subNetNode = outputConvertor.convertSubNetTreeNode2SubNetNode(root);
        List<SubNetTreeNode> childrenSubNetTreeNodes = nodeMap.values().stream()
                .filter(node -> root.getSubNetId().equals(node.getParentId()))
                .sorted(Comparator.comparing(SubNetTreeNode::getOrder))
                .collect(Collectors.toList());
        if (!childrenSubNetTreeNodes.isEmpty()) {
            List<SubNetNode> children = childrenSubNetTreeNodes.stream()
                    .map(child -> buildTreeNode(child, nodeMap))
                    .collect(Collectors.toList());
            subNetNode.setChildren(children);
        } else {
            subNetNode.setChildren(Collections.emptyList());
        }
        return subNetNode;
    }

    private SubNetTreeNode recordNewTreeNode(String parentSubNetId, String subNetName,
            String author) {
        log.debug("record a new subnet node:{} parentSubNetId:{}", subNetName, parentSubNetId);
        TaskInfoMessage taskInfoMessage = subnetTaskInfoBuilder.buildCreateSubNetTaskInfo(author,
                subNetName, parentSubNetId);
        try {
            SubNetTreeNode newNode = doCreateSubnetNode(parentSubNetId, subNetName, author);
            subnetTaskInfoBuilder.buildSuccessCreateSubNetTaskInfo(taskInfoMessage, newNode);
            return newNode;
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            subnetTaskInfoBuilder.buildFailedCreateSubNetTaskInfo(taskInfoMessage, errorMsg);
            if (e instanceof CommonException) {
                throw e;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "create SubNetTreeNode failed: " + errorMsg, e);
            }
        }
    }

    private SubNetTreeNode doCreateSubnetNode(String parentSubNetId, String subNetName,
            String author) {

        SubNetTreeNode parentSubNetNode = subNetTreeNodeDao.findBySubNetId(parentSubNetId)
                .orElse(null);
        if (parentSubNetNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "current subnet node parentNode: " + parentSubNetId + " is not found");
        }
        if (subNetTreeNodeDao.existsByParentIdAndName(parentSubNetId, subNetName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "subnet parent node already have name '" + subNetName + "' subnet node");
        }
        List<String> parentPathIds = parentSubNetNode.getPathIds();
        List<String> parentPathNames = parentSubNetNode.getPathNames();

        List<String> currentPathNames = new ArrayList<>();
        if (!CollectionUtils.isEmpty(parentPathNames)) {
            currentPathNames.addAll(parentPathNames);
        }
        currentPathNames.add(subNetName);
        List<String> currentPathIds = new ArrayList<>();
        if (!CollectionUtils.isEmpty(parentPathIds)) {
            currentPathIds.addAll(parentPathIds);
        }
        String currentSubNetId = idGenerator.nextBase62Id();
        currentPathIds.add(currentSubNetId);

        int level = parentSubNetNode.getLevel() + 1;
        int order = calculateOrder(parentSubNetId);

        SubNetTreeNode subNetTreeNode = new SubNetTreeNode();
        subNetTreeNode.setSubNetId(currentSubNetId);
        subNetTreeNode.setName(subNetName);
        subNetTreeNode.setLevel(level);
        subNetTreeNode.setCreateBy(author);
        subNetTreeNode.setUpdateBy(author);
        subNetTreeNode.setCreateTimestamp(System.currentTimeMillis());
        subNetTreeNode.setUpdateTimestamp(System.currentTimeMillis());
        subNetTreeNode.setParentId(parentSubNetId);
        subNetTreeNode.setPathIds(currentPathIds);
        subNetTreeNode.setPathNames(currentPathNames);
        subNetTreeNode.setOrder(order);
        if (parentSubNetNode.getIsLeaf()) {
            parentSubNetNode.setIsLeaf(false);
            subNetTreeNodeDao.save(parentSubNetNode);
        }

        SubNetTreeNode createSubTreeNode = subNetTreeNodeDao.save(subNetTreeNode);
        subnetChangeNotify.sendSubnetCreateNotify(parentSubNetNode, createSubTreeNode);
        return createSubTreeNode;
    }

    private int calculateOrder(String parentSubNetId) {
        log.debug("start to calculateOrder by parentSubNetId:{}", parentSubNetId);
        String parentId = StringUtils.hasText(parentSubNetId) ? parentSubNetId : null;
        Integer maxOrder = subNetTreeNodeDao.findMaxOrderByParentId(parentId);
        return maxOrder;

    }
}
