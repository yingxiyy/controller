package net.flex.dci.otn.controller.nms.nms.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.ResourceHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.springframework.stereotype.Component;

/**
 * 2025/8/4
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
public class TopologyAlarmResource extends BaseNms {

    private static final String LOCATE_RESOURCES_BY_ALARM = "nms:locate-resources-by-alarm";

    private final ResourceHandler resourceHandler;

    public TopologyAlarmResource(
            NetconfTopology netconfTopology, ResourceHandler resourceHandler) {
        super(netconfTopology);
        this.resourceHandler = resourceHandler;
    }

    @Override
    public String executeRequest(String cmd, String requestBody) throws CommonException {
        String returnValue = null;
        if (cmd.equals(LOCATE_RESOURCES_BY_ALARM)) {
            returnValue = getAlarmResourceLocate(cmd, requestBody);
        } else {
            throw new UnsupportedOperationException(
                    "unsupported cmd for alarm resource nms operations ");
        }
        return returnValue;
    }

    private String getAlarmResourceLocate(String cmd, String requestBody) {
        log.info("get alarm resource locate cmd:{},input is:{}", cmd, requestBody);
        try {
            LocateResourcesByAlarmInput input = parseInput(cmd, requestBody,
                    LocateResourcesByAlarmInput.class);
            LocateResourcesByAlarmOutput output = resourceHandler.locateResourceByAlarm(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get alarm resource located the reason is :" + ex.getMessage());
        }
    }
}
