package net.flex.dci.otn.controller.allocate.common.autoConfig;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(TaskInfoAnnotation.class)
@Documented
@Inherited
public @interface TaskInfoService {
}
