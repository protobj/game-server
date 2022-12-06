package io.protobj.redisaccessor.datasource;

import io.protobj.redisaccessor.FieldValue;
import io.protobj.redisaccessor.RedisDataSource;
import io.protobj.redisaccessor.config.RedisConfig;
import io.protobj.redisaccessor.serializer.FieldValueCodec;
import io.lettuce.core.KeyScanCursor;
import io.lettuce.core.KeyValue;
import io.lettuce.core.RedisClient;
import io.lettuce.core.ScanArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.reactive.RedisReactiveCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.CompressionCodec;
import io.lettuce.core.codec.RedisCodec;
import io.netty.util.internal.StringUtil;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

public class LettuceRedisDataSource implements RedisDataSource {

    private RedisReactiveCommands<byte[], FieldValue> connection;

    @Override
    public void init(RedisConfig redisConfig) {
        RedisClient redisClient = RedisClient
                .create("redis://%s:%d/0".formatted(redisConfig.getHost(), redisConfig.getPort()));
        StatefulRedisConnection<byte[], FieldValue> redisConnection = redisClient.connect(
                RedisCodec.of(ByteArrayCodec.INSTANCE, CompressionCodec.valueCompressor(
                        new FieldValueCodec(redisConfig.getPkg()), CompressionCodec.CompressionType.GZIP)));
        if (!StringUtil.isNullOrEmpty(redisConfig.getPasswd())) {
            redisConnection.sync().auth(redisConfig.getPasswd());
        }
        this.connection = redisConnection.reactive();
    }

    @Override
    public Mono<List<byte[]>> scan(int limit, byte[] patternBytes) {
        return connection.scan(ScanArgs.Builder.limit(limit).match(patternBytes))
                .map(KeyScanCursor::getKeys);
    }

    @Override
    public Mono<Map<byte[], FieldValue>> hgetall(byte[] key) {
        return connection.hgetall(key).collectMap(KeyValue::getKey, KeyValue::getValue);

    }

    @Override
    public Mono<Long> del(byte[] key) {
        return connection.del(key);
    }

    @Override
    public Mono<Long> hdel(byte[] key, byte[]... fields) {
        return connection.hdel(key, fields);
    }

    @Override
    public Mono<Boolean> hset(byte[] key, byte[] field, FieldValue value) {
        return connection.hset(key, field, value);
    }

    @Override
    public Mono<String> hmset(byte[] key, Map<byte[], FieldValue> map) {
        return connection.hmset(key, map);
    }

    @Override
    public void setAutoFlush(boolean autoFlush) {
        connection.setAutoFlushCommands(false);
    }

    @Override
    public void flush() {
        connection.flushCommands();
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.shutdown(true);
        }
    }

}
