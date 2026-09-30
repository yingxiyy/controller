package net.flex.dci.otc.controller.status;

import com.alibaba.fastjson.JSON;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import net.flex.dci.otc.controller.status.core.processor.NeStateChangeProcessor;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 8/3/2023 10:51 AM
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private NeStateChangeProcessor neStateChangeProcessor;
    @Autowired
    private PhyNodeDao phyNodeDao;

    @GetMapping("/test")
    public String hello(@RequestBody String neId)
            throws UnsupportedEncodingException {
//        neStateChangeProcessor.process(neId);
        List<NodeInfoDto> phyNodeInfo = phyNodeDao.listConfigNodeINfoByIds(
                Collections.singletonList(neId));
        String sessionInfo = "this is admin hello:";
        return JSON.toJSONString(phyNodeInfo);
    }
}
