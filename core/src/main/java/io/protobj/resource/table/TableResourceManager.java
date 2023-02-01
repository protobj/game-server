package io.protobj.resource.table;

import io.protobj.BeanContainer;
import io.protobj.IServer;
import io.protobj.resource.ResourceConfig;
import io.protobj.resource.ResourceManager;
import io.protobj.util.ReflectUtil;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;


public class TableResourceManager {

    private final ResourceManager resourceManager;
    private final Map<Class<?>, TableContainer<?, ?>> resourceMap = new ConcurrentHashMap<>();

    public TableResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public void loadTableResource(IServer server, Reflections reflections, boolean reload) {
        Set<Field> resourceContainerAnno = reflections.getFieldsAnnotatedWith(Storage.class);
        ResourceConfig resourceConfig = resourceManager.getResourceConfig();
        Map<Class<?>, TableContainer<?, ?>> loadedMap = new HashMap<>();
        //初始读取
        for (Field field : resourceContainerAnno) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            Class idType = (Class) parameterizedType.getActualTypeArguments()[0];
            Class resourceType = (Class) parameterizedType.getActualTypeArguments()[1];
            //是否存在配置文件
            Path path = Path.of(reload ? resourceConfig.getReloadPath() : resourceConfig.getResourcePath(), resourceType.getSimpleName(), ".json");
            if (!path.toFile().exists()) {
                if (!reload) {
                    throw new RuntimeException("file not exists:" + path);
                }
                continue;
            }
            Set<Field> allFields = ReflectionUtils.getAllFields(resourceType, it -> it.getAnnotation(Id.class) != null);
            if (allFields.size() != 1) {
                throw new RuntimeException("resource[%s] id field have zero or many".formatted(resourceType.getName()));
            }
            Class<?> type = allFields.iterator().next().getType();
            if (!validateIdType(type, idType)) {
                throw new RuntimeException("id type error :%s->%s".formatted(idType.getSimpleName(), type.getSimpleName()));
            }
            TableContainer<?, ?> tableContainer = new TableContainer<>(resourceType);
            tableContainer.load(path, resourceConfig.getJson());
            loadedMap.put(resourceType, tableContainer);
        }
        //依赖管理
        afterLoaded(loadedMap, it -> {
            TableContainer<?, ?> tableContainer = loadedMap.get(it);
            if (tableContainer == null) {
                tableContainer = resourceMap.get(it);
            }
            return tableContainer;
        });
        afterLoaded(resourceMap, loadedMap::get);
        //加载完成回调
        for (TableContainer<?, ?> value : loadedMap.values()) {
            for (Object o : value.values()) {
                IResource resource = (IResource) o;
                resource.afterLoad(this.resourceManager);
            }
        }
        //重设变量
        for (Field field : resourceContainerAnno) {
            ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
            Class resourceType = (Class) parameterizedType.getActualTypeArguments()[1];
            TableContainer<?, ?> tableContainer = loadedMap.get(resourceType);
            try {
                field.set(server.getBeanByType(field.getDeclaringClass()), tableContainer);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        resourceMap.putAll(loadedMap);
    }

    private void afterLoaded(Map<Class<?>, TableContainer<?, ?>> loadedMap, Function<Class<?>, TableContainer<?, ?>> containerFunction) {
        for (Map.Entry<Class<?>, TableContainer<?, ?>> containerEntry : loadedMap.entrySet()) {
            Class<?> type = containerEntry.getKey();
            TableContainer<?, ?> container = containerEntry.getValue();
            Set<Field> allFields = ReflectionUtils.getAllFields(type, it -> it.isAnnotationPresent(Dependency.class));

            for (Field allField : allFields) {
                allField = ReflectUtil.makeAccessible(allField);
                TableContainer<?, ?> tableContainer = containerFunction.apply(allField.getType());
                if (loadedMap != resourceMap && tableContainer == null) {
                    throw new RuntimeException("unknown resource:%s".formatted(allField.getType().getName()));
                }
                Dependency dependency = allField.getAnnotation(Dependency.class);
                Class<?> fieldType = allField.getType();
                boolean byId = dependency.by().equals("@Id");
                Predicate<Field> fieldPredicate = byId ? it -> it.isAnnotationPresent(Id.class) : it -> it.getName().equals(dependency.by());
                Field valueField = ReflectionUtils.getAllFields(fieldType, fieldPredicate).iterator().next();
                for (Object value : container.values()) {
                    try {
                        Object o = allField.get(value);
                        if (o == null) {
                            continue;
                        }
                        Object o1 = valueField.get(o);
                        if (byId) {
                            //重设属性
                            allField.set(value, tableContainer.getResource(o1, true));
                        } else {
                            allField.set(value, tableContainer.getUnique(allField.getName(), o1, true));
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
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

}
