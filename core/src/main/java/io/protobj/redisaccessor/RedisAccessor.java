package io.protobj.redisaccessor;

import io.protobj.Module;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface RedisAccessor {
    void init(List<Module> moduleList) throws Exception;

    Mono<Map<KeyId, Map<FieldId, FieldValue>>> getAllAsync(KeyDesc keyDesc);

    Mono<Long> delete(FieldValue fieldValue);

    Mono<Long> delete(KeyId keyId);

    Mono<FieldValue> save(FieldValue fieldValue);

    Mono<?> close();
}
