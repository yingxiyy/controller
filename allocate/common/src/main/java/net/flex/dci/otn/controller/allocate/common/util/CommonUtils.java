package net.flex.dci.otn.controller.allocate.common.util;

import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

import java.util.ArrayList;
import java.util.List;

public class CommonUtils {
    private static final String YANG_MODEL = "yang-model";
    private static final String DEFAULT_BAND = "C";

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
        if (properties == null) {
            properties = new PropertiesBuilder().build();
        }
        List<Property> propList = properties.getProperty();
        if (propList == null) {
            propList = new ArrayList<>();
        }
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

    public static boolean isSiteLinkAvailableForOch(Link siteLink, long lowerFrequency, long upperFrequency) {
        if (siteLink == null || siteLink.getAugmentation(Link1.class) == null
                || siteLink.getAugmentation(Link1.class).getSite() == null) {
            return false;
        }

        return isFrequencyRangeAvailable(siteLink.getAugmentation(Link1.class).getSite().getAvailable(),
                lowerFrequency, upperFrequency);
    }

    public static boolean isFrequencyRangeAvailable(List<Available> availables,
                                                    long lowerFrequency, long upperFrequency) {
        if (availables == null) {
            return false;
        }

        // Binding reuses an existing OCH, so the exact lower/upper spectrum must
        // be fully contained in one current siteLink available range.
        return availables.stream()
                .anyMatch(available -> available.getLowerFrequency().getValue().longValue() <= lowerFrequency
                        && available.getUpperFrequency().getValue().longValue() >= upperFrequency);
    }
}
