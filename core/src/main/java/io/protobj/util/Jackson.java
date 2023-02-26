package io.protobj.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import io.protobj.Json;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

public enum Jackson implements Json {
    INSTANCE,;
    private final ObjectMapper objectMapper = new ObjectMapper();

    Jackson() {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true);
        objectMapper.configure(JsonWriteFeature.QUOTE_FIELD_NAMES.mappedFeature(), false);
    }

    @Override
    public String encode(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T decode(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object decode(String json, Type type) {
        final JavaType javaType = objectMapper.getTypeFactory().constructType(type);
        try {
            return objectMapper.readValue(json, javaType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T decode(String json, TypeReference<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonNode readTree(File file) throws IOException {
        try {
            return objectMapper.readTree(file);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JsonNode readTree(InputStream input) throws IOException {
        try {
            return objectMapper.readTree(input);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
