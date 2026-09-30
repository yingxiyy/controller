package net.flex.dci.otn.controller.idc.manager.utils;

import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.idc.manager.common.i18n.I18nMessageUtil;
import net.flex.dci.otn.controller.idc.manager.config.I18Properties;

/**
 * @version 1.0
 * @date 2022/1/28 15:21
 */
@Slf4j
public class ValidateUtils {


    /**
     * @param data
     * @param clazz
     */
    public static <T> void validateData(T data,
            Class<?> clazz) {
        String language = SpringBeanFinder.getBean(I18Properties.class).getLanguage();
        Validator validator = SpringBeanFinder.getBean(Validator.class);
        Set<ConstraintViolation<T>> validate = validator.validate(data, clazz);
        if (validate.size() > 0) {
            String exceptionMsg = validate.stream()
                    .map(ConstraintViolation::getMessage)
                    .map(msg -> {
                        return I18nMessageUtil.getMessage(language, msg);
                    })
                    .collect(Collectors.joining(","));
            throw new ValidationException(exceptionMsg);
        }
    }

    /**
     * @param data
     */
    public static <T> void validateData(T data) {
        String language = SpringBeanFinder.getBean(I18Properties.class).getLanguage();
        Validator validator = SpringBeanFinder.getBean(Validator.class);
        Set<ConstraintViolation<T>> validate = validator.validate(data);
        if (validate.size() > 0) {
            String exceptionMsg = validate.stream()
                    .map(ConstraintViolation::getMessage)
                    .map(msg -> {
                        return I18nMessageUtil.getMessage(language, msg);
                    })
                    .collect(Collectors.joining(","));
            throw new ValidationException(exceptionMsg);
        }
    }


}
