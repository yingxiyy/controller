package net.flex.dci.otn.controller.nms.nms.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.nms.nms.handler.ViewTopologyHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2025/8/15
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class TopologyView extends BaseNms {

    private final String GET_SITE_VIEW_TOPOLOGY = "nms:get-site-view-topology";

    @Autowired
    private ViewTopologyHandler viewTopologyHandler;

    public TopologyView(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        String returnValue = null;
        if (cmd.equals(GET_SITE_VIEW_TOPOLOGY)) {
            returnValue = getSiteViewTopology(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException(
                    "unsupported nms operation method for site view topology");
        }
        return returnValue;
    }

    private String getSiteViewTopology(String cmd, String requestBody) {
        log.debug("start to get site view topology cmd:{},requestBody is:{}", cmd, requestBody);
        GetSiteViewTopologyInput input =
                !StringUtils.hasText(requestBody) ? null : parseInput(cmd, requestBody,
                        GetSiteViewTopologyInput.class);
        GetSiteViewTopologyOutput getSiteViewTopologyOutput = viewTopologyHandler.getSiteViewTopology(
                input);
        return serializeDataObject(cmd, getSiteViewTopologyOutput);
    }
}
