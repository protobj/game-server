package io.protobj.resource.table;

import com.fasterxml.jackson.databind.JsonNode;
import io.protobj.Json;
import io.protobj.util.CollectionUtil;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static io.protobj.util.ReflectUtil.makeAccessible;
import static io.protobj.util.ReflectUtil.newInstance;

public class TableContainer<K, V> {
    private final Class<V> resourceClass;

    private long lastModified;

    private volatile Map<K, V> resourceMap;

    /**
     * {@link Index}
     */
    private volatile Map<String, Map<Object, List<V>>> indexResourceMap;
    /**
     * {@link Unique}
     */
    private volatile Map<String, Map<Object, V>> uniqueResourceMap;


    public TableContainer(Class<V> resourceClass) {
        this.resourceClass = resourceClass;
    }

    public synchronized TableContainer<K, V> load(Path path, Json json) {
        try {
            Map<K, V> resourceMap = new HashMap<>();
            Map<String, Map<Object, List<V>>> indexResourceMap = new HashMap<>();
            Map<String, Map<Object, V>> uniqueResourceMap = new HashMap<>();

            Field idField = ReflectionUtils.getFields(resourceClass, it -> it.getAnnotation(Id.class) != null).iterator().next();
            idField.setAccessible(true);
            Map<String, Field> uniqueFields = ReflectionUtils.getFields(resourceClass, it -> it.getAnnotation(Unique.class) != null)
                    .stream().collect(Collectors.toMap(Field::getName, t -> t));
            uniqueFields.forEach((k, v) -> {
                uniqueResourceMap.put(k, new HashMap<>());
            });
            Map<String, Field> indexFields = ReflectionUtils.getFields(resourceClass, it -> it.getAnnotation(Index.class) != null)
                    .stream().collect(Collectors.toMap(Field::getName, t -> t));
            indexFields.forEach((k, v) -> {
                indexResourceMap.put(k, new HashMap<>());
            });
            read(resourceMap, indexResourceMap, uniqueResourceMap, idField, uniqueFields, indexFields, path, json);
            this.resourceMap = resourceMap;
            this.indexResourceMap = indexResourceMap;
            this.uniqueResourceMap = uniqueResourceMap;
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "resource [%s] ", resourceClass.getSimpleName()), e);
        }
        return this;
    }

    private void read(Map<K, V> resourceMap, Map<String, Map<Object, List<V>>> indexResourceMap,
                      Map<String, Map<Object, V>> uniqueResourceMap, Field idField,
                      Map<String, Field> uniqueFields,
                      Map<String, Field> indexFields, Path path, Json json) throws Exception {
        JsonNode jsonNode = json.readTree(path.toFile());
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonNode obj = jsonNode.get(i);
            V v = readObj(obj, json);
            final K k = (K) idField.get(v);
            if (resourceMap.containsKey(k)) {
                throw new RuntimeException(String.format("资源 %s id 重复 %s", resourceClass.getSimpleName(), k.toString()));
            }
            resourceMap.put(k, v);
            for (Map.Entry<String, Field> stringFieldEntry : indexFields.entrySet()) {
                final String key = stringFieldEntry.getKey();
                final Field value = stringFieldEntry.getValue();
                final Object indexValue = value.get(v);
                final Map<Object, List<V>> kListMap = indexResourceMap.get(key);
                CollectionUtil.addElement(kListMap, indexValue, v);
            }
            for (Map.Entry<String, Field> stringFieldEntry : uniqueFields.entrySet()) {
                final String key = stringFieldEntry.getKey();
                final Field value = stringFieldEntry.getValue();
                final Object uniqueValue = value.get(v);
                final Map<Object, V> objectVMap = uniqueResourceMap.get(key);
                if (objectVMap.containsKey(uniqueValue)) {
                    throw new RuntimeException(String.format("资源 %s unique 重复 %s", resourceClass.getSimpleName(), uniqueValue));
                }
                objectVMap.put(uniqueValue, v);
            }
        }
        this.lastModified = path.toFile().lastModified();
    }

    private V readObj(JsonNode obj, Json json) {
        try {
            V v = newInstance(resourceClass);
            Iterator<Map.Entry<String, JsonNode>> fields = obj.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                Field field = makeAccessible(resourceClass.getDeclaredField(entry.getKey()));
                Dependency annotation = field.getAnnotation(Dependency.class);
                JsonNode entryValue = entry.getValue();
                if (annotation == null) {
                    field.set(v, json.decode(entryValue.toString(), field.getType()));
                } else {
                    field.set(v, parseValue(annotation, field, entryValue, json));
                }
            }
            return v;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object parseValue(Dependency annotation, Field field, JsonNode entryValue, Json json) {
        Class<?> type = field.getType();
        String by = annotation.by();
        if (IResource.class.isAssignableFrom(type)) {
            Field next;
            try {
                if (by.equals("@Id")) {
                    next = ReflectionUtils.getAllFields(type, it -> it.isAnnotationPresent(Id.class)).iterator().next();
                } else {
                    next = ReflectionUtils.getAllFields(type, it -> it.getName().equals(by)).iterator().next();
                }
            } catch (Exception e) {
                throw new RuntimeException("field not exists:%s".formatted(by));
            }
            Object tempFieldValue = newInstance(type);
            try {
                next.set(tempFieldValue, json.decode(entryValue.toString(), next.getType()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        throw new UnsupportedOperationException("type:%s".formatted(type.getName()));
    }

    public V getResource(Object id, boolean silence) {
        return Optional.ofNullable(resourceMap.get(id)).orElseThrow(() -> {
            if (!silence) {
                return new RuntimeException(String.format(
                        "资源 [{}] 缺失 id:{}", resourceClass.getSimpleName(), id));
            }
            return null;
        });
    }

    public V getUnique(String uniqueKey, Object key, boolean silence) {
        return Optional.ofNullable(uniqueResourceMap.get(uniqueKey))
                .map(map -> map.get(key))
                .orElseThrow(() -> {
                    if (!silence)
                        return new RuntimeException(String.format(
                                "getUnique 资源 [{}] {} 缺失 unique:{}", resourceClass.getSimpleName(), uniqueKey, key.toString()));
                    else
                        return null;
                });
    }

    public List<V> getIndex(String indexKey, Object key, boolean silence) {
        return Optional.ofNullable(indexResourceMap.get(indexKey))
                .map(map -> map.get(key))
                .orElseThrow(() -> {
                    if (!silence)
                        return new RuntimeException(String.format(
                                "getIndex 资源 [%s] %s 缺失 index:%s", resourceClass.getSimpleName(), indexKey, key.toString()));
                    else
                        return null;
                });
    }

    public long getLastModified() {
        return lastModified;
    }

    public Class<V> getResourceClass() {
        return resourceClass;
    }

    public Collection<V> values() {
        return resourceMap.values();
    }
}
