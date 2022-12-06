package io.protobj.redisaccessor;

import io.protobj.redisaccessor.config.RedisConfig;
import reactor.core.publisher.Mono;

import java.util.Map;

public interface RedisAccessor {
    void init(RedisConfig redisConfig) throws Exception;

    Mono<Map<KeyId, Map<FieldId, FieldValue>>> getAllAsync(KeyDesc keyDesc);

    Mono<Long> delete(FieldValue fieldValue);

    Mono<Long> delete(KeyId keyId);

    Mono<FieldValue> save(FieldValue fieldValue);

    void close();
}
