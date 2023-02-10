package io.protobj.redisaccessor;

import io.protobj.redisaccessor.config.RedisConfig;
import io.protobj.redisaccessor.datasource.LettuceRedisDataSource;
import io.protobj.redisaccessor.util.PersistenceRedisHelper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultRedisAccessor implements RedisAccessor {

    protected String namespace;//作为key的前缀 namespace

    protected RedisDataSource dataSource;

    private final RedisConfig redisConfig;

    public DefaultRedisAccessor(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    @Override
    public void init() throws Exception {
        this.namespace = "{" + redisConfig.getNamespace() + "}";
        this.dataSource = new LettuceRedisDataSource();
        dataSource.init(redisConfig);
    }

    protected Map<FieldId, FieldValue> loadAll(KeyId keyId, Map<byte[], FieldValue> re) {
        return re.values().stream().collect(Collectors.toMap(FieldValue::createFieldId, t -> {
            t.initKeyId(keyId);
            return t;
        }));
    }

    @Override
    public Mono<Map<KeyId, Map<FieldId, FieldValue>>> getAllAsync(KeyDesc keyDesc) {
        if (keyDesc.many()) {
            String pattern = PersistenceRedisHelper.getManyKeyIdPattern(namespace, keyDesc);
            byte[] patternBytes = pattern.getBytes(StandardCharsets.UTF_8);
            Mono<List<byte[]>> scan = dataSource.scan(Integer.MAX_VALUE, patternBytes);
            return scan.flatMap(bytes -> {
                List<Mono<Map<FieldId, FieldValue>>> list = new ArrayList<>();
                for (byte[] key : bytes) {
                    KeyId keyId = keyDesc.createId(namespace, key);
                    Mono<Map<FieldId, FieldValue>> map = dataSource.hgetall(key).map(t -> loadAll(keyId, t));
                    list.add(map);
                }
                return Flux.concat(list).collectMap(it -> it.values().iterator().next().keyId());
            });
        } else {
            String key = PersistenceRedisHelper.getSingleKeyId(namespace, keyDesc);
            byte[] bytesKey = key.getBytes(StandardCharsets.UTF_8);
            KeyId keyId = keyDesc.createId(namespace, bytesKey);
            return dataSource.hgetall(bytesKey).map(t -> loadAll(keyId, t)).map(it -> Collections.singletonMap(keyId, it));
        }
    }

    @Override
    public Mono<Long> delete(FieldValue fieldValue) {
        return dataSource.hdel(fieldValue.keyId().byteKey(), fieldValue.fieldId().byteId());
    }

    @Override
    public Mono<Long> delete(KeyId keyId) {
        return dataSource.del(keyId.byteKey());
    }

    @Override
    public Mono<FieldValue> save(FieldValue fieldValue) {
        return dataSource.hset(fieldValue.keyId().byteKey(), fieldValue.fieldId().byteId(), fieldValue)
                .map(it -> fieldValue);
    }

    @Override
    public void close() {
        dataSource.close();
    }

    public String getNamespace() {
        return namespace;
    }
}
