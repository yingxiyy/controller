package net.flex.dci.otn.controller.user.configuration;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @date 2022/4/16 16:11
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurationSupport {

    /**
     * config to support the web configuration
     *
     * @param converters
     */
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                SerializerFeature.PrettyFormat,
                SerializerFeature.WriteDateUseDateFormat,
                SerializerFeature.DisableCircularReferenceDetect);

        converter.setFastJsonConfig(config);
        converters.add(converter);
        List<MediaType> supportedMediaType = new ArrayList<>();
        supportedMediaType.add(MediaType.APPLICATION_JSON);
        supportedMediaType.add(MediaType.APPLICATION_JSON_UTF8);
        supportedMediaType.add(MediaType.APPLICATION_ATOM_XML);
        supportedMediaType.add(MediaType.APPLICATION_FORM_URLENCODED);
        supportedMediaType.add(MediaType.APPLICATION_OCTET_STREAM);
        supportedMediaType.add(MediaType.APPLICATION_PDF);
        supportedMediaType.add(MediaType.APPLICATION_RSS_XML);
        supportedMediaType.add(MediaType.APPLICATION_XHTML_XML);
        supportedMediaType.add(MediaType.APPLICATION_XML);
        supportedMediaType.add(MediaType.IMAGE_GIF);
        supportedMediaType.add(MediaType.IMAGE_JPEG);
        supportedMediaType.add(MediaType.TEXT_EVENT_STREAM);
        supportedMediaType.add(MediaType.TEXT_HTML);
        supportedMediaType.add(MediaType.TEXT_MARKDOWN);
        supportedMediaType.add(MediaType.TEXT_XML);
        supportedMediaType.add(MediaType.TEXT_PLAIN);
        converter.setSupportedMediaTypes(supportedMediaType);

    }

}
