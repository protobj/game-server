package io.protobj.services;

import io.protostuff.Schema;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceCall {


    private final Map<Integer, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private final Map<Integer, Schema<?>> schemaMap = new ConcurrentHashMap<>();

    public ServiceCache discovery(int st) {
        return serviceCacheMap.get(st);
    }
}
