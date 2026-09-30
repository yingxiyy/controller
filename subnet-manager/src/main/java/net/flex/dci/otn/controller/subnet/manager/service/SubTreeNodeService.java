package net.flex.dci.otn.controller.subnet.manager.service;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otn.controller.subnet.manager.dto.CreateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.UpdateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.output.DeleteResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.FlatNodeDto;
import net.flex.dci.otn.controller.subnet.manager.dto.output.PageResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.SubNetNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.TreeNodeSearchResultDto;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
public interface SubTreeNodeService {


    SubNetNode createSubNetNode(CreateNodeReq createNodeReq, HttpServletRequest request);

    SubNetNode buildAllTree(String parentId);

    List<SubNetNode> getChildrenByParentId(String parentId);

    PageResult<FlatNodeDto> getFlatListPaged(String parentId, String keyword, int page, int size,
            String sortBy, String sortOrder);

    List<TreeNodeSearchResultDto> searchNodes(String keyword);

    SubNetNode updateNodeNameWithPath(UpdateNodeReq updateNodeReq, HttpServletRequest request);

    DeleteResult deleteSubNetNode(String subnetId, boolean force, HttpServletRequest request);

    List<SubNetNode> buildAllTreeWithUnassign(String parentId);
}
