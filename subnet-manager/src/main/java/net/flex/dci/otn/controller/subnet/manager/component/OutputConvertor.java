package net.flex.dci.otn.controller.subnet.manager.component;

import java.util.List;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.FlatNodeDto;
import net.flex.dci.otn.controller.subnet.manager.dto.output.PageResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.SubNetNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.TreeNodeSearchResultDto;
import org.springframework.data.domain.Page;

/**
 * 2026/1/11
 *
 * @author musa
 * @version 1.0
 **/
public interface OutputConvertor {

    SubNetNode convertSubNetTreeNode2SubNetNode(SubNetTreeNode subNetTreeNode);

    List<SubNetNode> convertSubNetTreeNodes2SubNetNodes(List<SubNetTreeNode> children);

    List<TreeNodeSearchResultDto> convert2SearchResults(List<SubNetTreeNode> subNetTreeNodes);

    PageResult<FlatNodeDto> convert2FlatNodePaged(Page<SubNetTreeNode> subNetTreeNodePage);
}
