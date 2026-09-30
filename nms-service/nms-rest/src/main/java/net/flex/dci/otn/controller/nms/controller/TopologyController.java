package net.flex.dci.otn.controller.nms.controller;

import static net.flex.dci.otn.controller.nms.utils.NetConfWildCard.NETWORK_TOPOLOGY_CONFIG_WILDCARD;
import static net.flex.dci.otn.controller.nms.utils.NetConfWildCard.NETWORK_TOPOLOGY_OPERATIONAL_WILDCARD;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/14 10:31
 */
@RestController
@Slf4j
public class TopologyController {

    @RequestMapping(value = NETWORK_TOPOLOGY_OPERATIONAL_WILDCARD, method = {RequestMethod.GET,
            RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
    public ResponseEntity<Object> executeOperationTopologyCmd() {
        return new ResponseEntity<>("this is topology request", HttpStatus.OK);
    }


    @RequestMapping(value = NETWORK_TOPOLOGY_CONFIG_WILDCARD, method = {RequestMethod.GET,
            RequestMethod.POST, RequestMethod.DELETE, RequestMethod.PUT})
    public ResponseEntity<Object> executeConfigTopologyCmd() {
        return new ResponseEntity<>("this is topology request", HttpStatus.OK);
    }

//    @RequestMapping(method = RequestMethod.POST)
//    public ResponseEntity<Object> setNetworkTopologyTree() {
//        return new ResponseEntity<>("this is topology request", HttpStatus.OK);
//    }
}
