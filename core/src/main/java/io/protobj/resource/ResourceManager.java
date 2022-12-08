package io.protobj.resource;

import io.protobj.BeanContainer;
import io.protobj.Module;
import io.protobj.resource.anno.Id;
import io.protobj.resource.anno.ResourceContainer;
import io.protobj.resource.anno.Single;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.protobj.ServerInstance.SERVICE_PACKAGE;

public class ResourceManager {

    private ResourceConfig resourceConfig;
    private Map<Class<?>, io.protobj.resource.ResourceContainer<?, ?>> resourceMap = new HashMap<>();

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
                resourceMap.put(resourceType, resourceContainer = new io.protobj.resource.ResourceContainer<>(resourceType).load(path,resourceConfig.getJson()));
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

    public static class SingleData<T> {
        String key;
        T data;
        Field observerField;

        public SingleData(String key,Field observerField) {
            this.key = key;
            this.observerField = observerField;
        }
    }
}
