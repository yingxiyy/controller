package net.flex.dci.otc.controller.ne.manager.core.filter.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.ELEMENT_SPECIAL;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.filter.Filter;
import org.springframework.core.env.Environment;

/**
 * @version 1.0
 * @date 2022/5/7 15:55
 */
@Slf4j
public abstract class AbstractFilter<T, R> implements Filter<T, R> {

    protected final Environment environment;

    public AbstractFilter(Environment environment) {
        this.environment = environment;
    }


    protected String getElementProperty(String key, String property, String type) {
        log.debug("in getClient property");
        String propertyName = String.format("%s.%s.%s.%s", ELEMENT_SPECIAL, key, property, type);
        String propertyValue = environment.getProperty(propertyName);
        return propertyValue;
    }

}
