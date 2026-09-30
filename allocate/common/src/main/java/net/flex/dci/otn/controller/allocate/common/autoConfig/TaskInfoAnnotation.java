package net.flex.dci.otn.controller.allocate.common.autoConfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

@Configuration
@Slf4j
public class TaskInfoAnnotation implements ImportSelector {

  @Override
  public String[] selectImports(AnnotationMetadata annotationMetadata) {
    Map<String, Object> attributes = annotationMetadata.getAnnotationAttributes(
        TaskInfoService.class.getName());
    if (attributes == null) {
      log.info("@EnableDciClient is not used on the main program");
      return new String[0];
    }
    return new String[]{TaskInfoAutoConfigure.class.getName()};
  }
}