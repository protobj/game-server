package io.protobj;

import com.fasterxml.jackson.core.type.TypeReference;

import java.lang.reflect.Type;

public interface Json {

    String encode(Object obj);

    <T> T decode(String json, Class<T> valueType);

    Object decode(String json, Type type);

    <T> T decode(String json, TypeReference<T> valueType);
}
