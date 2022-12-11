package io.protobj.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.protobj.BeanContainer;
import io.protobj.Module;
import io.protobj.resource.anno.Id;
import io.protobj.resource.anno.ResourceContainer;
import io.protobj.resource.anno.Single;
import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.protobj.ServerInstance.SERVICE_PACKAGE;

public class ResourceManager {

    private final ResourceConfig resourceConfig;
    private final Map<Class<?>, io.protobj.resource.ResourceContainer<?, ?>> resourceMap = new HashMap<>();

    private final Map<String, SingleValueRecord> recordMap = new HashMap<>();

    public ResourceManager(ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
    }

    public void loadResource(List<Module> moduleList, BeanContainer beanContainer, boolean reload) {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.FieldsAnnotated);
        for (Module module : moduleList) {
            configurationBuilder.forPackages(module.getClass().getPackage().getName() + "." + SERVICE_PACKAGE);
        }
        Reflections reflections = new Reflections(configurationBuilder);
        Set<Field> resourceContainerAnno = reflections.getFieldsAnnotatedWith(ResourceContainer.class);
        for (Field field : resourceContainerAnno) {
            Class<?> declaringClass = field.getDeclaringClass();
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            Class idType = (Class) parameterizedType.getActualTypeArguments()[0];
            Class resourceType = (Class) parameterizedType.getActualTypeArguments()[1];

            Object bean = beanContainer.getBeanByType(declaringClass);
            io.protobj.resource.ResourceContainer<?, ?> resourceContainer = resourceMap.get(resourceType);
            if (resourceContainer == null) {
                Set<Field> allFields = ReflectionUtils.getAllFields(resourceType, it -> it.getAnnotation(Id.class) != null);
                if (allFields.size() != 1) {
                    throw new RuntimeException("resource[%s] id field have zeor or many".formatted(resourceType.getName()));
                }
                Class<?> type = allFields.iterator().next().getType();
                if (!validateIdType(type, idType)) {
                    throw new RuntimeException("id type error :%s->%s".formatted(idType.getSimpleName(), type.getSimpleName()));
                }
                Path path = Path.of(resourceConfig.getResourcePath(), resourceType.getSimpleName(), ".json");
                resourceMap.put(resourceType, resourceContainer = new io.protobj.resource.ResourceContainer<>(resourceType).load(path, resourceConfig.getJson()));
            } else {

            }
            if (!field.canAccess(bean)) {
                field.setAccessible(true);
            }
            try {
                field.set(bean, resourceContainer);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        Set<Field> singleFields = reflections.getFieldsAnnotatedWith(Single.class);
        for (Field singleField : singleFields) {
            Single annotation = singleField.getAnnotation(Single.class);
            if (StringUtils.isEmpty(annotation.file())) {
                throw new RuntimeException("single:[%s:%s] file not set ".formatted(singleField.getDeclaringClass().getName(), singleField.getName()));
            }
            if (StringUtils.isEmpty(annotation.value())) {
                throw new RuntimeException("single:[%s:%s] value not set ".formatted(singleField.getDeclaringClass().getName(), singleField.getName()));
            }
            SingleValueRecord singleValueRecord = new SingleValueRecord(annotation.file(), annotation.value(), singleField);
            SingleValueRecord old = recordMap.get(singleValueRecord.getFullName());
            if (old != null) {
                throw new RuntimeException("single field  [%s:%s] is already set in [%s:%s]".formatted(singleField.getDeclaringClass().getName(), singleField.getName()
                        , old.observerField.getDeclaringClass().getName(), old.observerField.getName()
                ));
            }
            String file = annotation.file();
            try {
                Path of = Path.of(resourceConfig.getResourcePath(), file, "_single.json");
                JsonNode jsonNode = resourceConfig.getJson().readTree(of.toFile());
                Class<?> type = singleField.getType();
                Object decode = resourceConfig.getJson().decode(jsonNode.toString(), type);
                Object beanByType = beanContainer.getBeanByType(singleField.getDeclaringClass());
                singleField.setAccessible(true);
                singleField.set(beanByType, decode);
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean validateIdType(Class<?> type, Class idType) {
        if (type == int.class || type == Integer.class) {
            return idType == Integer.class;
        } else if (type == short.class || type == Short.class) {
            return idType == Short.class;
        } else if (type == long.class || type == Long.class) {
            return idType == Long.class;
        } else if (type == String.class && idType == String.class) {
            return true;
        }
        return false;
    }

    public static class SingleValueRecord {
        private String fileName;
        private String key;
        Object data;
        Field observerField;

        public SingleValueRecord(String fileName, String key, Field observerField) {
            this.fileName = fileName;
            this.key = key;
            this.observerField = observerField;
        }

        public String getFullName() {
            return fileName + "." + key;
        }
    }
}
