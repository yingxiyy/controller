package net.flex.dci.otn.controller.auth.configuration;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @version 1.0
 * @date 2022/4/16 16:11
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurationSupport {

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        Iterator<HttpMessageConverter<?>> iterator = converters.iterator();
        while (iterator.hasNext()) {
            HttpMessageConverter<?> converter = iterator.next();
            if (converter instanceof MappingJackson2HttpMessageConverter) {
                iterator.remove();
            }
        }
        FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
        FastJsonConfig config = new FastJsonConfig();
        config.setSerializerFeatures(
                SerializerFeature.WriteNullStringAsEmpty,
                SerializerFeature.WriteNullBooleanAsFalse,
                SerializerFeature.PrettyFormat,
                SerializerFeature.WriteNullNumberAsZero,
                SerializerFeature.WriteNullBooleanAsFalse,
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
