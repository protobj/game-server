package io.protobj.resource;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import io.protobj.resource.anno.Id;
import io.protobj.resource.anno.Index;
import io.protobj.resource.anno.Unique;
import io.protobj.util.CollectionUtil;
import org.reflections.ReflectionUtils;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResourceContainer<K, V> {
    private final Class<V> resourceClass;

    private long lastModified;

    private volatile Map<K, V> resourceMap;

    /**
     * {@link io.protobj.resource.anno.Index}
     */
    private volatile Map<String, Map<Object, List<V>>> indexResourceMap;
    /**
     * {@link io.protobj.resource.anno.Unique}
     */
    private volatile Map<String, Map<Object, V>> uniqueResourceMap;


    public ResourceContainer(Class<V> resourceClass) {
        this.resourceClass = resourceClass;
    }

    public ResourceContainer<K, V> load(Path path) {
        try {
            Map<K, V> resourceMap = new HashMap<>();
            Map<String, Map<Object, List<V>>> indexResourceMap = new HashMap<>();
            Map<String, Map<Object, V>> uniqueResourceMap = new HashMap<>();

            Field idField = ReflectionUtils.getFields(resourceClass, it -> it.getAnnotation(Id.class) != null).iterator().next();
            Map<String, Field> uniqueFields = ReflectionUtils.getFields(resourceClass, it -> it.getAnnotation(Unique.class) != null)
                    .stream().collect(Collectors.toMap(t -> t.getAnnotation(Unique.class).value(), t -> t));
            uniqueFields.forEach((k, v) -> {
                uniqueResourceMap.put(k, new HashMap<>());
            });
            Map<String, Field> indexFields = ReflectionUtils.getFields(resourceClass, it -> it.getAnnotation(Index.class) != null)
                    .stream().collect(Collectors.toMap(t -> t.getAnnotation(Index.class).value(), t -> t));
            indexFields.forEach((k, v) -> {
                indexResourceMap.put(k, new HashMap<>());
            });
            read(resourceMap, indexResourceMap, uniqueResourceMap, idField, uniqueFields, indexFields, path);
            this.resourceMap = resourceMap;
            this.indexResourceMap = indexResourceMap;
            this.uniqueResourceMap = uniqueResourceMap;
        } catch (Exception e) {
            throw new RuntimeException(String.format(
                    "resource [%s] not found", resourceClass.getSimpleName()), e);
        }
        return this;
    }


    private void read(Map<K, V> resourceMap, Map<String, Map<Object, List<V>>> indexResourceMap,
                      Map<String, Map<Object, V>> uniqueResourceMap, Field idField,
                      Map<String, Field> uniqueFields,
                      Map<String, Field> indexFields, Path path) throws Exception {
        String s = Files.readString(path);
        List<Any> anies = JsonIterator.deserialize(s).asList();
        for (int i = 0; i < anies.size(); i++) {
            V v = anies.get(i).as(resourceClass);
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


    public V getResource(K id, boolean silence) {
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
}
