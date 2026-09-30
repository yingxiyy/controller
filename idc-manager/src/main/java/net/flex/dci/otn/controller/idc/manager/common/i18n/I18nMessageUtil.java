package net.flex.dci.otn.controller.idc.manager.common.i18n;

import java.io.IOException;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * @version 1.0
 * @date 2022/2/8 16:15
 */
public class I18nMessageUtil {

    private static MessageSourceAccessor messageSourceAccessor;

    private static final String PATH_PARENT = "classpath:i18n/";

    private static final String SUFFIX = ".properties";

    private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private I18nMessageUtil() {

    }


    private static void initMessageSourceAccessor(String language) throws IOException {
        language = LanguageEnum.getLanguageType(language);
        Resource resource = resourcePatternResolver.getResource(PATH_PARENT + language + SUFFIX);
        String fileName = resource.getURL().toString();
        int lastIndex = fileName.lastIndexOf(".");
        String baseName = fileName.substring(0, lastIndex);

        ReloadableResourceBundleMessageSource reloadableResourceBundleMessageSource = new ReloadableResourceBundleMessageSource();
        reloadableResourceBundleMessageSource.setBasename(baseName);
        reloadableResourceBundleMessageSource.setCacheSeconds(5);
        reloadableResourceBundleMessageSource.setDefaultEncoding("UTF-8");
        messageSourceAccessor = new MessageSourceAccessor(reloadableResourceBundleMessageSource);
    }


    public static String getMessage(String language, String message, Object... args) {
        try {
            initMessageSourceAccessor(language);
            return messageSourceAccessor.getMessage(message, args,
                    LocaleContextHolder.getLocale());
        } catch (IOException ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }
}
