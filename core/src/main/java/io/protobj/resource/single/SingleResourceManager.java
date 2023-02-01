package io.protobj.resource.single;

import com.fasterxml.jackson.databind.JsonNode;
import io.protobj.BeanContainer;
import io.protobj.IServer;
import io.protobj.resource.ResourceConfig;
import io.protobj.resource.ResourceManager;
import io.protobj.util.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.*;

public class SingleResourceManager {

    private static final Logger log = LoggerFactory.getLogger(SingleResourceManager.class);

    private final ResourceManager resourceManager;

    public SingleResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }


    public void loadSingleResource(IServer server, Reflections reflections, boolean reload) {
        ResourceConfig resourceConfig = resourceManager.getResourceConfig();
        Set<Field> singleFields = reflections.getFieldsAnnotatedWith(Single.class);
        Map<String, SingleFileDesc> singleFileDescMap = readSingleResourceFile(reload);
        for (Field singleField : singleFields) {
            Single annotation = singleField.getAnnotation(Single.class);
            if (StringUtils.isEmpty(annotation.file())) {
                throw new RuntimeException("single:[%s:%s] file not set ".formatted(singleField.getDeclaringClass().getName(), singleField.getName()));
            }
            if (StringUtils.isEmpty(annotation.value())) {
                throw new RuntimeException("single:[%s:%s] value not set ".formatted(singleField.getDeclaringClass().getName(), singleField.getName()));
            }
            singleField.setAccessible(true);
            Object beanByType = server.getBeanByType(singleField.getDeclaringClass());
            if (beanByType == null) {
                log.warn("bean has singleValue field but not found instance:{} field:{}", singleField.getDeclaringClass().getName(), singleField.getName());
                continue;
            }
            SingleValue singleValue = null;
            try {
                singleValue = (SingleValue) singleField.get(beanByType);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (singleValue == null) {
                Type valueType;
                try {
                    ParameterizedType genericType = (ParameterizedType) singleField.getGenericType();
                    valueType = genericType.getActualTypeArguments()[0];
                } catch (Throwable e) {
                    Class<?> type = singleField.getType();
                    Field field = ReflectionUtils.getFields(type, it -> it.getName().equals("value")).stream().toList().get(0);
                    valueType = field.getGenericType();
                }
                try {
                    Class<?> type = singleField.getType();
                    Constructor<?> constructor = type.getConstructor(Type.class);
                    singleValue = (SingleValue) constructor.newInstance(valueType);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            String fileName = annotation.file() + "_single.json";
            String result = getSingleStringValue(singleFileDescMap, annotation, fileName);
            if (result == null) {
                continue;
            }
            singleValue.setSource(annotation.file() + "." + annotation.value(), result, resourceConfig.getJson());
            singleField.setAccessible(true);
            try {
                singleField.set(beanByType, singleValue);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static String getSingleStringValue(Map<String, SingleFileDesc> singleFileDescMap, Single annotation, String fileName) {
        return Optional.of(singleFileDescMap)
                .map(it -> it.get(fileName))
                .map(SingleFileDesc::getValueDescMap)
                .map(it -> it.get(annotation.value()))
                .map(SingleValueDesc::getValue)
                .orElse(null);
    }

    private Map<String, SingleFileDesc> readSingleResourceFile(boolean reload) {
        ResourceConfig resourceConfig = resourceManager.getResourceConfig();
        Map<String, SingleFileDesc> singleFileDescMap = new HashMap<>();
        String resourcePath = reload ? resourceConfig.getReloadPath() : resourceConfig.getResourcePath();
        try {
            List<Path> paths = FileUtil.getPaths(it -> it.getFileName().endsWith("_single.json"), resourcePath);
            for (Path path : paths) {
                File file = path.toFile();
                JsonNode jsonNode = resourceConfig.getJson().readTree(file);
                Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
                String name = file.getName();
                SingleFileDesc singleFileDesc = new SingleFileDesc();
                singleFileDesc.setFileName(name);
                singleFileDesc.setLastModified(file.lastModified());
                singleFileDesc.setValueDescMap(new HashMap<>());
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> nodeEntry = fields.next();
                    SingleValueDesc singleValueDesc = new SingleValueDesc(nodeEntry.getKey(), nodeEntry.getValue().toString());
                    singleFileDesc.getValueDescMap().put(singleValueDesc.getName(), singleValueDesc);
                }
                singleFileDescMap.put(name, singleFileDesc);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return singleFileDescMap;
    }
}
