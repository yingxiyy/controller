package net.flex.dci.otn.controller.nms.nms.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.component.dimension.DimensionView;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteReachabilityInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteReachabilityOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSubnetDimensionalViewInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSubnetDimensionalViewOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.reachability.output.Reachability;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.subnet.dimensional.view.output.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 2026/3/25
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class TopologyDimension extends BaseNms {

    @Autowired
    private DimensionView dimensionView;

    private static final String GET_SUBNET_DIMENSIONAL_VIEW_CMD = "nms:get-subnet-dimensional-view";

    private static final String GET_SITE_REACHABILITY = "nms:get-site-reachability";

    public TopologyDimension(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        String returnValue = null;
        if (cmd.equals(GET_SUBNET_DIMENSIONAL_VIEW_CMD)) {
            returnValue = getDimensionView(cmd, requestBody);
        } else if (cmd.equals(GET_SITE_REACHABILITY)) {
            returnValue = getSiteReachability(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException(
                    "unsupported cmd for topology dimension nms operations ");
        }
        return returnValue;
    }

    /**
     * get site reachability
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getSiteReachability(String cmd, String requestBody) {
        log.info("get site reachability request body:{}", requestBody);
        try {
            GetSiteReachabilityInput input = parseInput(cmd, requestBody,
                    GetSiteReachabilityInput.class);
            Reachability reachability = dimensionView.getSiteReachability(input);
            GetSiteReachabilityOutputBuilder outputBuilder = new GetSiteReachabilityOutputBuilder();
            outputBuilder.setReachability(reachability);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("Failed to get site reachability:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to get site reachability " + ex.getMessage());
        }
    }

    /**
     * get dimension view topology
     *
     * @param cmd
     * @param requestBody
     * @return
     */
    private String getDimensionView(String cmd, String requestBody) {
        log.info("get dimension view topology request body:{}", requestBody);
        try {
            GetSubnetDimensionalViewInput getSubnetDimensionalViewInput = parseInput(cmd,
                    requestBody,
                    GetSubnetDimensionalViewInput.class);
            Topology topology = dimensionView.getSubnetDimensionView(getSubnetDimensionalViewInput);
            GetSubnetDimensionalViewOutputBuilder outputBuilder = new GetSubnetDimensionalViewOutputBuilder();
            outputBuilder.setTopology(topology);
            return serializeDataObject(cmd, outputBuilder.build());
        } catch (Exception ex) {
            log.error("failed to get dimension view topology :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get dimension view topology " + ex.getMessage());
        }
    }
}
