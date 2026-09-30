package net.flex.dci.otn.controller.allocate.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.NeYangModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Data
@Slf4j
public class AllocatorConfig {

    @Value("${server.yangModel}")
    private String yangModel;

    /**
     * 部署级频率分配顺序。未配置时保持原有的从低到高行为。
     */
    @Value("${server.frequency-allocation-order:ASCENDING}")
    private FrequencyAllocationOrder frequencyAllocationOrder = FrequencyAllocationOrder.ASCENDING;

    private List<String> supportedList =  Arrays.stream(NeYangModel.values())
            .map(Enum::name)
            .collect(Collectors.toList());

    @PostConstruct
    public void validate() {
        if (!isValid(yangModel)) {
            log.error("Invalid yangModel config value: " + yangModel);
            log.error("supported value is {}", supportedList);
            // 推荐抛出异常，Spring Boot 应该会中止启动
            throw new IllegalArgumentException("Invalid configuration: myapp.yangModel=" + yangModel);

            // 或者强制退出 JVM
            // System.exit(1);
        }
    }

    private boolean isValid(String value) {
        return value != null &&  (supportedList.contains(value));
    }

    public NeYangModel getYangModel() {
        if (yangModel == null)
            yangModel = "Tencent";
        return NeYangModel.valueOf(yangModel);
    }
    public String getYangModelString() {
        return this.yangModel;
    }
}
