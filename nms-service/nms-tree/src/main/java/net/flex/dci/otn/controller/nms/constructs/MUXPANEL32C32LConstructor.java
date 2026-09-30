package net.flex.dci.otn.controller.nms.constructs;

import static net.flex.dci.otn.controller.nms.utils.Constants.MD_PORT_PATTERN;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @version 1.0
 * @date 10/9/2025 11:15 AM
 */
public class MUXPANEL32C32LConstructor {

    public static String getMPOTpId(String tpId, String muxChannelTpId) {
        if (muxChannelTpId == null) {
            return tpId;
        }
        Pattern pattern = Pattern.compile(MD_PORT_PATTERN);
        Matcher matcher = pattern.matcher(muxChannelTpId);
        String channelIndex = matcher.find() ? matcher.group(1) : "1";
//        char channelIndex = muxChannelTpId.charAt(muxChannelTpId.length() - 1);
        int index = Integer.parseInt(channelIndex);
        int mpoIndex = (index - 1) / 8 % 4 + 1;
        return tpId + mpoIndex;
    }
}
