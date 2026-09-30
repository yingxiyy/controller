package net.flex.dci.otn.controller.allocate.annotation;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import org.springframework.data.mongodb.core.mapping.Document;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Document
public @interface TaskInfoEnable {
  String title();

  TaskInfoMessage.ResourceType resourceType();

  TaskInfoMessage.ActionType actionType();
}
