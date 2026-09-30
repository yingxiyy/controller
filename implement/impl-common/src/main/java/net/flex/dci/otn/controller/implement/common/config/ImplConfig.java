package net.flex.dci.otn.controller.implement.common.config;

import lombok.Data;
import net.flex.dci.otc.common.util.NeYangModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
public class ImplConfig {

    @Value("${controller.implement.writeWithoutIP:false}")
    private boolean writeWithoutIP;

    @Value("${controller.implement.disableDeImplSiteLink:false}")
    private boolean disableDeImplSiteLink;

    @Value("${controller.implement.uploadNeParam:true}")
    private boolean uploadNeParam = true;

    @Value("${controller.implement.tunnelOverAse:false}")
    private boolean tunnelOverAse = false;

    @Value("${controller.implement.tunnelForceDeimplement:false}")
    private boolean tunnelForceDeimplement = false;

    @Value("${controller.implement.stepPool.coreSize:24}")
    private int stepPoolCoreSize = 24;

    @Value("${controller.implement.stepPool.maxSize:32}")
    private int stepPoolMaxSize = 32;

    @Value("${controller.implement.stepPool.queueSize:256}")
    private int stepPoolQueueSize = 256;
//    private final DefaultValue defaultValue;
//    public ImplConfig(DefaultValue defaultValue) {
//        this.defaultValue = defaultValue;
//    }


    @Value("${server.yangModel}")
    private String yangModel;

    public NeYangModel getYangModel() {
        if (yangModel == null)
            yangModel = "Tencent";
        return NeYangModel.valueOf(yangModel);
    }

//    public String getDefaultValue(String key) {
//        return defaultValue.getConfig(yangModel).get(key);
//    }
//
//    public Map<String, String> getAllDefaultValues() {
//        return defaultValue.getConfig(yangModel);
//    }
}
