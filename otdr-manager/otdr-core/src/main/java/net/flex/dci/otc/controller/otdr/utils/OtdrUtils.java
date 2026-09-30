package net.flex.dci.otc.controller.otdr.utils;

import static net.flex.dci.otc.common.constants.AuthConstant.BLANK;
import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.DEFAULT_DAILY_TIME;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import net.flex.dci.otc.controller.otdr.model.OtdrCurrentDetail;
import net.flex.dci.otc.controller.otdr.model.OtdrEvents;
import net.flex.dci.otc.controller.otdr.model.OtdrWaveForm;
import net.flex.dci.otc.controller.otdr.model.link.PhyLinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

/**
 * @version 1.0
 * @date 2022/9/2 14:29
 */
@Slf4j
public class OtdrUtils {

    public static final String TIME_REGEX = "([0-1]?[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9])$";
    private static final String TIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss'Z'XXX";

    public static String getTime(String time) {
        if (time == null) {
            return DEFAULT_DAILY_TIME;
        }
        Pattern pattern = Pattern.compile(TIME_REGEX);
        Matcher matcher = pattern.matcher(time);
        boolean flag = matcher.matches();
        if (!flag) {
            return DEFAULT_DAILY_TIME;
        }
        return time;
    }

    public static BigInteger generateTaskId(Date current) {
        long time = current.getTime();
        int i = (int) (Math.random() * 900) + 100;
        String taskIdStr = String.valueOf(time)
                + i;
        return BigInteger.valueOf(Long.parseLong(taskIdStr));
    }

    public static OtdrCurrentDetail getDefaultOTDRDetail(String monitorTpId,
            MonitorDirection monitorDirection) {
        OtdrCurrentDetail otdrCurrentDetail = new OtdrCurrentDetail();
        otdrCurrentDetail.setMonitorPort(monitorTpId);
        otdrCurrentDetail.setMonitorDirection(
                monitorDirection.name());
        OtdrEvents otdrEvents = new OtdrEvents();
        otdrEvents.setEvents(new ArrayList<>());
        otdrCurrentDetail.setEvents(otdrEvents);
        OtdrWaveForm otdrWaveForm = new OtdrWaveForm();
        otdrWaveForm.setData(BLANK);
        otdrWaveForm.setDataLength(0L);
        otdrCurrentDetail.setWaveForm(otdrWaveForm);
        return otdrCurrentDetail;
    }

    public static PhyLinkInfo getPhyLinkBrieflyInfo(Link refPhyLink) {
        log.debug("to get the phy link briefly info for the ref phylink,the link id is:{}",
                refPhyLink.getLinkId().getValue());
        String linkId = refPhyLink.getLinkId().getValue();
        String srcTp = refPhyLink.getSource().getSourceTp().getValue();
        String destTp = refPhyLink.getDestination().getDestTp().getValue();
        return PhyLinkInfo.builder().linkId(linkId).srcTpId(srcTp).destTpId(destTp).build();
    }

    public static Long convertTimeString2Timestamp(String time) {
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(TIME_FORMATTER);
            Date date = simpleDateFormat.parse(time);
            return date.getTime();
        } catch (ParseException ex) {
            log.error("failed to parse the date,the exception is:{}", ex.getMessage(), ex);
            return null;
        }
    }

    public static String getNodeName(Node ne) {
        Physical nePhysical = ne.getAugmentation(
                Node1.class).getPhysical();
        return nePhysical.getFriendlyName();
    }

    public static String getMonitorDirectionStr(boolean isSource,
            MonitorDirection monitorDirection) {
        log.debug("to get monitor direction str");
        int flag = (isSource ? 1 : 0) ^ monitorDirection.getIntValue();

        return Objects.requireNonNull(OtdrMonitorDirection.getDirection(flag)).getDirectionStr();
    }


    public static OtdrMonitorDirection getMonitorDirection(boolean isSource,
            MonitorDirection monitorDirection) {
        log.debug("to get monitor direction ");
        int flag = (isSource ? 1 : 0) ^ monitorDirection.getIntValue();

        return OtdrMonitorDirection.getDirection(flag);
    }
}
