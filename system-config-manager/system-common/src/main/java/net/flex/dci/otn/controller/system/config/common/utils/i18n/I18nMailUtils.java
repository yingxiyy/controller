package net.flex.dci.otn.controller.system.config.common.utils.i18n;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.Constants;
import net.flex.dci.otn.controller.system.config.common.enums.I18nMailCode;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * @version 1.0
 * @date 10/12/2023 1:25 PM
 */
@Slf4j
public class I18nMailUtils {

    private static final String RESOURCE_PATH = "i18n/mail";

    private static final ReloadableResourceBundleMessageSource resourceBundle;

    private static Locale locale;

    static {
        resourceBundle = new ReloadableResourceBundleMessageSource();
        resourceBundle.setDefaultEncoding(StandardCharsets.UTF_8.toString());
        resourceBundle.setBasename(RESOURCE_PATH);
    }

    public static String getMessage(String code) {
        return getMessage(code, null, code);
    }

    public static String getMessage(String code, String[] args, String defaultMessage) {
        String content;
        try {
            content = resourceBundle.getMessage(code, args, locale);
        } catch (Exception ex) {
            log.error("international general parameter get failed ====>{}", ex.getMessage(), ex);
            content = defaultMessage;
        }
        return content;
    }

    public static void setLangTag(String lang) {
        if (lang == null) {
            locale = Locale.getDefault();
        } else {
            String[] langTag = lang.split(Constants.UNDER_LINE);
            locale = new Locale(langTag[0], langTag[1]);
        }
    }


    public static String getAlarmEmailSubject() {
        return getMessage(I18nMailCode.AlarmMailSubject.name());
    }

    public static String getSuccessCheckEmailFormatter() {
        return getMessage(I18nMailCode.SuccessEmailCheckFormatter.name());
    }

    public static String getFailedCheckEmailFormatter() {
        return getMessage(I18nMailCode.FailedEmailCheckFormatter.name());
    }


}
