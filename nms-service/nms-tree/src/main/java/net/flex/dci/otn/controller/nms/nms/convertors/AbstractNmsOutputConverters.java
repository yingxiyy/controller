package net.flex.dci.otn.controller.nms.nms.convertors;

import static net.flex.dci.otn.controller.nms.utils.Constants.NONE;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.ApsXCCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 2022/11/6 10:59
 */
@Slf4j
public abstract class AbstractNmsOutputConverters<T, R> implements INmsOutputConverters<T, R> {


    @Autowired
    protected DciTopologyCacheManager dciTopologyCacheManager;

    public abstract NMSConvertType convertType();

    protected String getApsActivePath(String apsXCId) {
        if (apsXCId == null) {
            return NONE;
        }
        log.debug("get active path");
        ApsXCCache apsXCCache = dciTopologyCacheManager.getValue(apsXCId);
        if (null == apsXCCache) {
            return NONE;
        }
        return apsXCCache.getActivePath() == null ? ApsPath.PRIMARY.name()
                : apsXCCache.getActivePath();
    }

}
