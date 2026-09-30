package net.flex.dci.otn.controller.implement.tunnel;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class RequireTestLogNameFilter extends Filter<ILoggingEvent> {

    @Override
    public FilterReply decide(ILoggingEvent event) {
        String testLogName = event.getMDCPropertyMap().get("testLogName");
        return testLogName == null || testLogName.isEmpty() ? FilterReply.DENY : FilterReply.NEUTRAL;
    }
}
