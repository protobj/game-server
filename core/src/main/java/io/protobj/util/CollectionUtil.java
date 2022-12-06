package io.protobj.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectionUtil {

    public static <K, T> void addElement(Map<K, List<T>> map, K key, T value) {
        List<T> ts = map.get(key);
        if (ts == null) {
            ts = new ArrayList<>();
            final List<T> old = map.putIfAbsent(key, ts);
            if (old != null) {
                ts = old;
            }
        }
        ts.add(value);
    }
}
