package net.flex.dci.otn.controller.idc.manager.common.i18n;

import com.alibaba.druid.util.StringUtils;
import lombok.Getter;
import lombok.ToString;

/**
 * @version 1.0
 * @date 2022/2/8 15:49
 */
@Getter
@ToString
public enum LanguageEnum {

    LANGUAGE_EN_US("en_us"),

    LANGUAGE_ZH_CN("zh_cn");

    private String language;

    private LanguageEnum(String language) {
        this.language = language;
    }


    public static String getLanguageType(String language) {
        if (StringUtils.isEmpty(language)) {
            return LANGUAGE_ZH_CN.language;
        }
        for (LanguageEnum languageEnum : LanguageEnum.values()) {
            if (language.equalsIgnoreCase(languageEnum.language)) {
                return languageEnum.language;
            }
        }
        return LANGUAGE_ZH_CN.language;
    }
}
