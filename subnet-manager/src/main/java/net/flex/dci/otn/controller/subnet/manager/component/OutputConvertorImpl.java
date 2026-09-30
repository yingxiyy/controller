package net.flex.dci.otn.controller.subnet.manager.component;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.FlatNodeDto;
import net.flex.dci.otn.controller.subnet.manager.dto.output.PageResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.SubNetNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.TreeNodeSearchResultDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/1/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class OutputConvertorImpl implements OutputConvertor {

    @Override
    public SubNetNode convertSubNetTreeNode2SubNetNode(SubNetTreeNode subNetTreeNode) {
        String fullPaths = buildFullPath(subNetTreeNode);
        String fullPathIds = buildFullPathIds(subNetTreeNode);
        boolean isLeaf = subNetTreeNode.getIsLeaf();
        SubNetNode subNetNode = SubNetNode.builder()
                .parentId(subNetTreeNode.getParentId())
                .order(subNetTreeNode.getOrder())
                .level(subNetTreeNode.getLevel())
                .parentPathNames(subNetTreeNode.getPathNames())
                .parentPathIds(subNetTreeNode.getPathIds())
                .name(subNetTreeNode.getName())
                .subnetId(subNetTreeNode.getSubNetId())
                .fullPathIds(fullPathIds)
                .fullPathName(fullPaths)
                .hasChildren(!isLeaf)
                .build();
        return subNetNode;
    }

    @Override
    public List<SubNetNode> convertSubNetTreeNodes2SubNetNodes(List<SubNetTreeNode> children) {
        return children.stream().map(this::convertSubNetTreeNode2SubNetNode)
                .collect(Collectors.toList());
    }

    @Override
    public List<TreeNodeSearchResultDto> convert2SearchResults(
            List<SubNetTreeNode> subNetTreeNodes) {
        List<TreeNodeSearchResultDto> searchResult = subNetTreeNodes.stream()
                .map(this::convert2SearchResult)
                .collect(
                        Collectors.toList());
        return searchResult;
    }

    @Override
    public PageResult<FlatNodeDto> convert2FlatNodePaged(Page<SubNetTreeNode> subNetTreeNodePage) {

        List<FlatNodeDto> flatNodeDTOs = subNetTreeNodePage.getContent().stream()
                .map(this::convert2FlatNodeDto).collect(
                        Collectors.toList());
        return PageResult.<FlatNodeDto>builder()
                .content(flatNodeDTOs)
                .total(subNetTreeNodePage.getTotalElements())
                .pages(subNetTreeNodePage.getTotalPages())
                .size(subNetTreeNodePage.getSize())
                .page(subNetTreeNodePage.getNumber())
                .build();
    }

    private FlatNodeDto convert2FlatNodeDto(SubNetTreeNode subNetTreeNode) {
        String parentName = subNetTreeNode.getParentId() == null ? null
                : subNetTreeNode.getPathNames().get(subNetTreeNode.getLevel() - 1);
        return FlatNodeDto.builder()
                .id(subNetTreeNode.getId())
                .subNetId(subNetTreeNode.getSubNetId())
                .level(subNetTreeNode.getLevel())
                .order(subNetTreeNode.getOrder())
                .createBy(subNetTreeNode.getCreateBy())
                .hasChildren(!subNetTreeNode.getIsLeaf())
                .createTime(subNetTreeNode.getCreateTimestamp())
                .updateTime(subNetTreeNode.getUpdateTimestamp())
                .parentId(subNetTreeNode.getParentId())
                .parentName(parentName)
                .name(subNetTreeNode.getName())
                .isLeaf(subNetTreeNode.getIsLeaf())
                .fullPath(String.join("/", subNetTreeNode.getPathNames()))
                .updateBy(subNetTreeNode.getUpdateBy())
                .build();
    }

    private TreeNodeSearchResultDto convert2SearchResult(SubNetTreeNode node) {
        TreeNodeSearchResultDto treeNodeSearchResultDto = TreeNodeSearchResultDto.builder()
                .parentId(node.getParentId())
                .subNetId(node.getSubNetId())
                .level(node.getLevel())
                .name(node.getName())
                .pathNames(node.getPathNames())
                .pathIds(node.getPathIds())
                .fullPath(String.join("/", node.getPathNames()) + "/" + node.getName())
                .build();
        return treeNodeSearchResultDto;
    }

    private String buildFullPathIds(SubNetTreeNode subNetTreeNode) {
        StringBuilder sb = new StringBuilder();
        if (!CollectionUtils.isEmpty(subNetTreeNode.getPathIds())) {
            sb.append(String.join("/", subNetTreeNode.getPathIds()));
        }
        return sb.toString();
    }

    private String buildFullPath(SubNetTreeNode subNetTreeNode) {
        StringBuilder sb = new StringBuilder();
        if (!CollectionUtils.isEmpty(subNetTreeNode.getPathNames())) {
            sb.append(String.join("/", subNetTreeNode.getPathNames()));
        }
        return sb.toString();
    }
}
