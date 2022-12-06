package io.protobj.redisaccessor.datasource;//package io.protobj.redisaccessor.datasource;
//
//import io.protobj.redisaccessor.config.RedisConfig;
//import io.protobj.redisaccessor.RedisDataSource;
//import io.lettuce.core.KeyScanCursor;
//import io.lettuce.core.KeyValue;
//import io.lettuce.core.ScanArgs;
//import io.lettuce.core.cluster.RedisClusterClient;
//import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
//import io.lettuce.core.cluster.api.reactive.RedisAdvancedClusterReactiveCommands;
//import io.lettuce.core.codec.ByteArrayCodec;
//import io.lettuce.core.codec.CompressionCodec;
//import io.lettuce.core.codec.RedisCodec;
//import io.netty.util.internal.StringUtil;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//import java.util.Map;
//
//public class LettuceRedisClusterDataSource implements RedisDataSource {
//
//    private RedisAdvancedClusterReactiveCommands<byte[], byte[]> connection;
//
//    @Override
//    public void init(RedisConfig redisConfig) {
//        RedisClusterClient redisClient = RedisClusterClient
//                .create("redis://%s:%d/0".formatted(redisConfig.getHost(), redisConfig.getPort()));
//        StatefulRedisClusterConnection<byte[], byte[]> redisConnection = redisClient.connect(
//                RedisCodec.of(ByteArrayCodec.INSTANCE, CompressionCodec.valueCompressor(
//                        ByteArrayCodec.INSTANCE, CompressionCodec.CompressionType.GZIP)));
//        if (!StringUtil.isNullOrEmpty(redisConfig.getPasswd())) {
//            redisConnection.sync().auth(redisConfig.getPasswd());
//        }
//        this.connection = redisConnection.reactive();
//    }
//
//    @Override
//    public Mono<List<byte[]>> scan(int limit, byte[] patternBytes) {
//        return connection.scan(ScanArgs.Builder.limit(limit).match(patternBytes))
//                .map(KeyScanCursor::getKeys);
//    }
//
//    @Override
//    public Mono<Map<byte[], byte[]>> hgetall(byte[] key) {
//        return connection.hgetall(key).collectMap(KeyValue::getKey, KeyValue::getValue);
//
//    }
//
//    @Override
//    public Mono<Long> del(byte[] key) {
//        return connection.del(key);
//    }
//
//    @Override
//    public Mono<Long> hdel(byte[] key, byte[]... fields) {
//        return connection.hdel(key, fields);
//    }
//
//    @Override
//    public Mono<Boolean> hset(byte[] key, byte[] field, byte[] value) {
//        return connection.hset(key, field, value);
//    }
//
//    @Override
//    public Mono<String> hset(byte[] key, Map<byte[], byte[]> map) {
//        return connection.hmset(key, map);
//    }
//
//    @Override
//    public void close() {
//        if (connection != null) {
//            connection.shutdown(true);
//        }
//    }
//}
