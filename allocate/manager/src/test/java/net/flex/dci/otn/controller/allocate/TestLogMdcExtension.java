package net.flex.dci.otn.controller.allocate;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.MDC;

public class TestLogMdcExtension implements BeforeEachCallback, AfterEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        MDC.put("testLogName", "alloc-manager-" + context.getRequiredTestMethod().getName());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        MDC.remove("testLogName");
    }
}
