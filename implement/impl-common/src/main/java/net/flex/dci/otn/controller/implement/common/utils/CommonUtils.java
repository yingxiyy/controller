package net.flex.dci.otn.controller.implement.common.utils;

import static net.flex.dci.otc.common.constants.Constants.UNDER_LINE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.FAILED;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.INTERVAL;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.UPLOAD_NE_HISTORY_PM;

import com.alibaba.fastjson.JSON;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 11/22/2023 3:59 PM
 */
public class CommonUtils {

    private static final String DEFAULT_BAND = "C";
    private static final String YANG_MODEL = "yang-model";

    public static DateAndTime getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String str = sdf.format(new Date());
        String str1 = str.substring(0, str.length() - 2);
        String str2 = str.substring(str.length() - 2);
        String sb = str1
                + ":"
                + str2;
        return DateAndTime.getDefaultInstance(sb);

    }

    public static NeYangModel getYangModelInProperties(Properties properties) {
        String currentModelName = PropertyTool.getValue(properties, YANG_MODEL);
        if (currentModelName != null) {
            // 成功获取到第一个有效的 YANG model，直接返回
            return NeYangModel.valueOf(currentModelName);
        }

        // 如果遍历后都未获取到有效的YANG model，返回默认值
        return NeYangModel.Tencent;
    }

    public static Properties addProperty(Properties properties, String name, String value) {
        List<Property> propList = properties.getProperty();
        PropertyTool.putKeyValue(propList, name, value);
        return new PropertiesBuilder(properties).setProperty(propList).build();
    }


    public static WDM_Band getWDMBand(Site linkAttr) {
        String bandStr = linkAttr.getLinkGroup();
        // 如果对应的 band 为空，则设置默认值
        if (bandStr == null) {
            bandStr = DEFAULT_BAND;
        }
        return WDM_Band.fromString(bandStr);
    }

    public static String generateUploadNeHistoryPmTaskName(String neName, BigInteger interval) {
        return UPLOAD_NE_HISTORY_PM + neName + UNDER_LINE + INTERVAL + UNDER_LINE + interval;
    }

    public static boolean onDGE(Physical nodeAttr, CrossConnections dbXc) {
        String sTpId = dbXc.getSourceTp().get(0).getTpRef().getValue();
        String eqId = PhysicalTpIdNamingRule.getEquipId(sTpId);
        Equipments eq = nodeAttr.getEquipments().stream()
                .filter(x -> x.getEquipmentId().equals(eqId)).findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find out eqId " + eqId));

        return eq.getEquipType().equals(EquipType.DGE);
    }

    public static String buildRequestDetail(String requestBody, String errorMessage) {
        Map<String, String> requestDetail = new HashMap<>();
        requestDetail.put("request", requestBody);
        requestDetail.put("response", errorMessage);
        return JSON.toJSONString(requestDetail);
    }

    public static String convert(FailObj failObj) {
        StringBuffer sb = new StringBuffer();
        if (failObj != null && failObj.getObject() != null) {
            for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object
                    obj : failObj.getObject()) {
                sb.append(obj.getObjectType().name() + ": ");
                sb.append(obj.getObjectId() + ", ");
                sb.append(obj.getMessageInfo() + "\n");
            }
        }
        return sb.toString();
    }

    public static String convert(String objName, FailObj failObj) {
        StringBuffer sb = new StringBuffer();
        if (failObj != null && failObj.getObject() != null) {
            for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object
                    obj : failObj.getObject()) {
                sb.append(obj.getObjectType().name() + ": ");
                sb.append(objName + ", ");
                sb.append(obj.getMessageInfo() + "\n");
            }
        }
        return sb.toString();
    }

    public static void logMessage(String title, String resourceName, String errorMessage,
            TaskInfoMessage taskInfoMessage) {

        taskInfoMessage.setResourceName(resourceName);

        String msg = String.format("%s %s ", title, resourceName);
        StringBuilder msgBuilder = new StringBuilder(msg);
        boolean isOk = false;
        if (!StringUtils.hasText(errorMessage)) {
            //创建成功
            isOk = true;
            msgBuilder.append(Constants.SUCCESSFULLY);
        } else {
            isOk = false;
            msgBuilder.append(FAILED)
                    .append(Constants.REASON_IS)
                    .append(errorMessage);

        }
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        String requestBody = taskInfoMessage.getDetail();
        String detailBody = CommonUtils.buildRequestDetail(requestBody, errorMessage);
        taskInfoMessage.setDetail(detailBody);
        if (isOk) {
            taskInfoMessage.setSuccessfully(isOk);
        } else {
            taskInfoMessage.setSuccessfully(isOk);
            taskInfoMessage.setErrorReason(FAILED);
        }
        TaskInfoMessager.sendMessage(taskInfoMessage);

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(title)
                        .message(msgBuilder.toString())
                        .error(!isOk)
                        .build());
    }
}
