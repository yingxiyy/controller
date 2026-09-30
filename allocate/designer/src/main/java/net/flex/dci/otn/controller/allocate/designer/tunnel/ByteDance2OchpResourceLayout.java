package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bone2.0 OCHP layout from slides 16 and 17. A 32-channel siteLink may use only
 * M1D1-M32D32, while a 64-channel siteLink may additionally use M33D33-M64D64.
 * Keep this rule here because each protection leg can use a different siteLink capacity.
 */
final class ByteDance2OchpResourceLayout extends DefaultOchpResourceLayout {

    private static final Pattern MD_PORT_PATTERN = Pattern.compile("M(\\d+)D(\\d+)",
            Pattern.CASE_INSENSITIVE);

    private final int bandwidth;

    ByteDance2OchpResourceLayout(int bandwidth) {
        if (bandwidth != 32 && bandwidth != 64) {
            throw new IllegalArgumentException("ByteDance2.0 OCHP bandwidth must be 32 or 64");
        }
        this.bandwidth = bandwidth;
    }

    @Override
    public boolean allowsMdPort(String portName) {
        Matcher matcher = MD_PORT_PATTERN.matcher(portName);
        if (!matcher.matches()) {
            return false;
        }
        int muxIndex = Integer.parseInt(matcher.group(1));
        int demuxIndex = Integer.parseInt(matcher.group(2));
        return muxIndex == demuxIndex && muxIndex >= 1 && muxIndex <= bandwidth;
    }
}
