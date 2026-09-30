package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Slf4j
public class MergeData {

  public static void merge(Object source, Object target) {
    for (Field field : target.getClass().getDeclaredFields()) {
      StringBuilder get = new StringBuilder();
      StringBuilder set = new StringBuilder();

      String firstLetter;
      String otherLetters;
      if (field.getName().equals("augmentation")) {
        get.append("get");
        set.append("add");

        firstLetter = field.getName().substring(0, 1).toUpperCase(Locale.ROOT);
        otherLetters = field.getName().substring(1);
        continue;
      } else {
        if (field.getType().getTypeName().equals(Boolean.class.getTypeName())) {
          get.append("is");
          set.append("set");
        } else {
          get.append("get");
          set.append("set");
        }
        firstLetter = field.getName().substring(1, 2).toUpperCase(Locale.ROOT);
        otherLetters = field.getName().substring(2);
      }
      get.append(firstLetter);
      get.append(otherLetters);

      set.append(firstLetter);
      set.append(otherLetters);

      try {
        Method getMethod;
        Method setMethod;
        try {
          getMethod = source.getClass().getMethod(get.toString());
          setMethod = target.getClass().getMethod(set.toString(), getMethod.getReturnType());
        } catch (Exception e) {
          log.debug("skip not supported get/set is {} {}", get.toString(), set.toString());
          continue;
        }
        getMethod.setAccessible(true);
        setMethod.setAccessible(true);
        Object value = getMethod.invoke(source);
        if (value != null) {
          if (field.getName().equals("_properties")) {
            List<Property> propList = new ArrayList<>();

            Object sourceProp = getMethod.invoke(source);
            propList.addAll(((Properties)sourceProp).getProperty());

            Method targetGetMethod = target.getClass().getMethod(get.toString());
            targetGetMethod.setAccessible(true);
            Object targetProp = targetGetMethod.invoke(target);
            if (targetProp != null && ((Properties)targetProp).getProperty() != null) {
	          for (Property prop : ((Properties)targetProp).getProperty()) {
	            if (propList.stream().filter(t -> t.getName().equals(prop.getName())).findAny()
	               .isPresent()) {
	              continue;
	            } else {
	              propList.add(prop);
	            }
	          }
            }

            setMethod.invoke(target, new PropertiesBuilder().setProperty(propList).build());
          } else if (value instanceof DataObject) { //ODL data type
            Class<? extends DataContainer> odlClass = ((DataObject) value).getImplementedInterface();
            String odlType = odlClass.getTypeName();
            String builderName = odlType + "Builder";
            Class<?> builderClass = Class.forName(builderName);
            Constructor<?> builder = builderClass.getDeclaredConstructor(odlClass);

            Method targetGetMethod = target.getClass().getMethod(get.toString());
            targetGetMethod.setAccessible(true);
            Object targetObj = targetGetMethod.invoke(target);
            Object targetInst = builder.newInstance(targetObj);
            merge(value, targetInst);

            Method buildMethod = builderClass.getMethod("build");
            setMethod.invoke(target, buildMethod.invoke(targetInst));
          } else {
            setMethod.invoke(target, value);
          }
        }
      } catch (Exception e) {
        log.error("merge {}} error", source.getClass().getSimpleName(), e);
      }
    }
  }

}

