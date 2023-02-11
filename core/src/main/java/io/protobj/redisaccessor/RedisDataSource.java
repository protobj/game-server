package io.protobj.redisaccessor;

import io.protobj.Module;
import io.protobj.redisaccessor.config.RedisConfig;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public interface RedisDataSource {

    void init(List<Module> moduleList, RedisConfig redisConfig) throws Exception;

    Mono<List<byte[]>> scan(int limit, byte[] patternBytes);


    Mono<Map<byte[], FieldValue>> hgetall(byte[] key);


    Mono<Long> del(byte[] key);

    Mono<Long> hdel(byte[] key, byte[]... fields);

    Mono<Boolean> hset(byte[] key, byte[] field, FieldValue value);

    Mono<String> hmset(byte[] key, Map<byte[], FieldValue> map);

    void setAutoFlush(boolean autoFlush);

    void flush();

    Mono<?> close();
}
