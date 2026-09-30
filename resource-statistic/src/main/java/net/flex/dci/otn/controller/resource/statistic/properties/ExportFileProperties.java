package net.flex.dci.otn.controller.resource.statistic.properties;

import java.io.Serializable;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Configuration
@Data
@ConfigurationProperties(prefix = "export.file")
public class ExportFileProperties implements Serializable {

    private String path;

    private Integer ttl = 3;
}
