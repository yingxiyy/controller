package net.flex.dci.otn.controller.resource.statistic.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 *
 * 2025/10/30
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Configuration
public class WebConfiguration extends WebMvcConfigurationSupport {

    static {
        JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(converter -> converter instanceof MappingJackson2HttpMessageConverter);
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();

        FastJsonConfig config = new FastJsonConfig();

        converter.setFastJsonConfig(config);
        converters.add(converter);
        List<MediaType> supportedMediaType = new ArrayList<>();
        supportedMediaType.add(MediaType.APPLICATION_JSON);
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
