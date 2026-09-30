package net.flex.dci.otn.controller.nms.nms.convertors;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/23 10:37
 */
@Slf4j
@Component
public class NmsOutputConverters {

    private final Map<NMSConvertType, AbstractNmsOutputConverters<?, ?>> convertersMap = new HashMap<>();

    public NmsOutputConverters(List<AbstractNmsOutputConverters<?, ?>> converters) {
        converters.stream().forEach(converter -> {
            convertersMap.put(converter.convertType(), converter);
        });
    }


    /**
     * convert 2 nms defined out put
     *
     * @param list
     * @param <T>
     * @param <R>
     * @return
     */
    public <T extends DataObject, R extends DataObject> List<T> convert2NmsOutput(List<R> list) {
        log.debug("start to convert the db data to nms rpc api out put");
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        AbstractNmsOutputConverters<T, R> nmsOutputConverters = getOutputConverters(list);
        List<T> result = nmsOutputConverters.convert2NmsOutput(list);
        return result;
    }


    /**
     * convert 2 nms defined out put
     *
     * @param list
     * @param <T>
     * @param <R>
     * @return
     */
    public <T extends DataObject, R extends DataObject> List<T> convert2FullNmsOutput(
            List<R> list) {
        log.debug("start to convert the db data to nms rpc api out put");
        if (list.isEmpty()) {
            return new ArrayList<>();
        }
        AbstractNmsOutputConverters<T, R> nmsOutputConverters = getOutputConverters(list);
        List<T> result = nmsOutputConverters.convert2FullNmsOutput(list);
        return result;
    }

    /**
     * @param list
     * @param <R>
     * @param <T>
     * @return
     */
    private <R, T> AbstractNmsOutputConverters<T, R> getOutputConverters(
            List<R> list) {
        log.debug("start to get nms out put converters based on list type");
        List<DataObject> dataObjects = (List<DataObject>) list;
        NMSConvertType convertType = NMSConvertType.getConvertType(dataObjects.get(0));
        return (AbstractNmsOutputConverters<T, R>) convertersMap.get(convertType);
    }
}
