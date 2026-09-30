package net.flex.dci.otn.controller.subnet.manager.controller;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.subnet.manager.dto.CreateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.UpdateNodeReq;
import net.flex.dci.otn.controller.subnet.manager.dto.output.FlatNodeDto;
import net.flex.dci.otn.controller.subnet.manager.dto.output.PageResult;
import net.flex.dci.otn.controller.subnet.manager.dto.output.SubNetNode;
import net.flex.dci.otn.controller.subnet.manager.dto.output.TreeNodeSearchResultDto;
import net.flex.dci.otn.controller.subnet.manager.service.SubTreeNodeServiceImpl;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@RequestMapping(value = "/restconf/subnet")
@RequiredArgsConstructor
@Slf4j
public class SubNetTreeController {

    private final SubTreeNodeServiceImpl subTreeNodeService;

    @GetMapping(value = "/subtree", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> getSubNetTree(@RequestParam(required = false) String parentId) {
        SubNetNode subNetNode = subTreeNodeService.buildAllTree(parentId);
        return new ResponseEntity<>(Result.ok(subNetNode), HttpStatus.OK);
    }

    @GetMapping(value = "/filter-subtree", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> getFilterSubtree(@RequestParam(required = false) String parentId) {
        List<SubNetNode> subNetNode = subTreeNodeService.buildAllTreeWithUnassign(parentId);
        return new ResponseEntity<>(Result.ok(subNetNode), HttpStatus.OK);
    }


    @GetMapping(value = "/children", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> getLazySubNetTree(@RequestParam(required = false) String parentId) {
        List<SubNetNode> children = subTreeNodeService.getChildrenByParentId(parentId);
        return new ResponseEntity<>(Result.ok(children), HttpStatus.OK);
    }


    @GetMapping(value = "/flat", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> getSubNetTreeFlat(
            @RequestParam(name = "parentId", required = false) String parentId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(required = false, defaultValue = "level") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortOrder
    ) {
        page = page <= 0 ? 0 : page;
        size = size <= 0 ? 20 : size;

        PageResult<FlatNodeDto> result = subTreeNodeService.getFlatListPaged(parentId, keyword,
                page, size, sortBy, sortOrder);
        return new ResponseEntity<>(Result.ok(result), HttpStatus.OK);
    }


    @GetMapping(value = "/search", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> searchNodes(
            @RequestParam(name = "keyword", required = false) String keyword
    ) {
        List<TreeNodeSearchResultDto> searchResultDtos = subTreeNodeService.searchNodes(keyword);
        return new ResponseEntity<>(Result.ok(searchResultDtos), HttpStatus.OK);
    }


    @PostMapping(value = "/nodes", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> addNodes(@RequestBody CreateNodeReq createNodeReq,
            HttpServletRequest request) {
        SubNetNode subNetNode = subTreeNodeService.createSubNetNode(createNodeReq, request);
        return new ResponseEntity<>(Result.ok(subNetNode), HttpStatus.OK);
    }

    @PutMapping(value = "/node/name", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> updateSubTreeNodeName(@RequestBody UpdateNodeReq updateNodeReq,
            HttpServletRequest request) {
        SubNetNode subNetNode = subTreeNodeService.updateNodeNameWithPath(updateNodeReq, request);
        return new ResponseEntity<>(Result.ok(subNetNode), HttpStatus.OK);
    }

}
