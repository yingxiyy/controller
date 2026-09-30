package net.flex.dci.otn.controller.nms.nms.convertors;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/11/7 15:00
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultNmsOutputConverters extends AbstractNmsOutputConverters {

    
    @Override
    public NMSConvertType convertType() {
        return NMSConvertType.DEFAULT;
    }

    @Override
    public List convert2NmsOutput(List nodeList) {
        return nodeList;
    }
}
