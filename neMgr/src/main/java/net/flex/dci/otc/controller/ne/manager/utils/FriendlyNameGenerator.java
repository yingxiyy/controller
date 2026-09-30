package net.flex.dci.otc.controller.ne.manager.utils;

import static net.flex.dci.otc.common.constants.Constants.DOT;
import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.common.constants.Constants.UNDER_LINE;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.dto.SlotInfo;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.apache.commons.lang3.StringUtils;

/**
 * @version 1.0
 * @date 2022/3/29 15:15
 */
@Slf4j
public class FriendlyNameGenerator {

    public static String generate(String prefix, SlotInfo slotInfo) {
        StringBuilder stringBuilder = new StringBuilder(prefix);
        stringBuilder.append(HYPHEN);
        stringBuilder.append(slotInfo.getShelf());
        stringBuilder.append(HYPHEN);
        stringBuilder.append(slotInfo.getSlot());
        if (slotInfo.getPort() != null) {
            stringBuilder.append(HYPHEN);
            stringBuilder.append(slotInfo.getPort());
        }
        return stringBuilder.toString();
    }

    public static String generate(String tpId, String eqName) {

        String arr[] = tpId.split(POUND);
        if (arr.length != 4) {
            return null;
        }
        String tpSegment = arr[3];
        if (StringUtils.isBlank(eqName)) {
            eqName = arr[2];
        }
        String splits[] = tpSegment.split(HYPHEN);
        String tpFriendName = new StringBuilder().append(eqName).append(HYPHEN)
                .append(splits[splits.length - 1]).toString();
        return tpFriendName;
    }


    public static String generateTeleMapperName(InstanceDetails instanceDetails) {
        log.debug("generate telemetry mapper name:{}", instanceDetails.getId());
        String mapperIp = instanceDetails.getHostIp();
        String module = instanceDetails.getModule();
        int port = instanceDetails.getPort();
        StringBuilder builder = new StringBuilder();
        builder.append(module).append(UNDER_LINE)
                .append(mapperIp.replace(DOT, UNDER_LINE)).append(UNDER_LINE)
                .append(port);
        return builder.toString();
    }
}
