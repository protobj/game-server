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
//import org.apache.commons.pool2.BasePooledObjectFactory;
//import org.apache.commons.pool2.PooledObject;
//import org.apache.commons.pool2.impl.DefaultPooledObject;
//import org.apache.commons.pool2.impl.GenericObjectPool;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//import java.util.Map;
//
//public class LettuceRedisClusterPoolDataSource implements RedisDataSource {
//
//    private GenericObjectPool<RedisConn> connectionPool;
//
//    public class RedisConn implements AutoCloseable {
//        final RedisAdvancedClusterReactiveCommands<byte[], byte[]> connection;
//
//        public RedisConn(RedisAdvancedClusterReactiveCommands<byte[], byte[]> connection) {
//            this.connection = connection;
//        }
//
//        public RedisAdvancedClusterReactiveCommands<byte[], byte[]> getConnection() {
//            return connection;
//        }
//
//        @Override
//        public void close() throws Exception {
//            connectionPool.returnObject(this);
//        }
//    }
//
//    @Override
//    public void init(RedisConfig redisConfig) {
//        RedisClusterClient redisClusterClient = RedisClusterClient
//                .create("redis://%s:%d/0".formatted(redisConfig.getHost(), redisConfig.getPort()));
//        connectionPool = new GenericObjectPool<>(new BasePooledObjectFactory<>() {
//            @Override
//            public RedisConn create() {
//                StatefulRedisClusterConnection<byte[], byte[]> redisConnection = redisClusterClient.connect(
//                        RedisCodec.of(ByteArrayCodec.INSTANCE, CompressionCodec.valueCompressor(
//                                ByteArrayCodec.INSTANCE, CompressionCodec.CompressionType.GZIP)));
//                if (!StringUtil.isNullOrEmpty(redisConfig.getPasswd())) {
//                    redisConnection.sync().auth(redisConfig.getPasswd());
//                }
//                return new RedisConn(redisConnection.reactive());
//            }
//
//            @Override
//            public PooledObject<RedisConn> wrap(RedisConn redisConn) {
//                return new DefaultPooledObject<>(redisConn);
//            }
//        });
//    }
//
//    @Override
//    public Mono<List<byte[]>> scan(int limit, byte[] patternBytes) {
//        try (RedisConn pool = connectionPool.borrowObject()) {
//            return pool.getConnection().scan(ScanArgs.Builder.limit(limit).match(patternBytes))
//                    .map(KeyScanCursor::getKeys);
//        } catch (Throwable e) {
//            return Mono.error(e);
//        }
//    }
//
//    @Override
//    public Mono<Map<byte[], byte[]>> hgetall(byte[] key) {
//        try (RedisConn pool = connectionPool.borrowObject()) {
//            return pool.getConnection().hgetall(key).collectMap(KeyValue::getKey, KeyValue::getValue);
//        } catch (Throwable e) {
//            return Mono.error(e);
//        }
//    }
//
//    @Override
//    public Mono<Long> del(byte[] key) {
//        try (RedisConn pool = connectionPool.borrowObject()) {
//            return pool.getConnection().del(key);
//        } catch (Throwable e) {
//            return Mono.error(e);
//        }
//    }
//
//    @Override
//    public Mono<Long> hdel(byte[] key, byte[]... fields) {
//        try (RedisConn pool = connectionPool.borrowObject()) {
//            return pool.getConnection().hdel(key, fields);
//        } catch (Throwable e) {
//            return Mono.error(e);
//        }
//    }
//
//    @Override
//    public Mono<Boolean> hset(byte[] key, byte[] field, byte[] value) {
//        try (RedisConn pool = connectionPool.borrowObject()) {
//            return pool.getConnection().hset(key, field, value);
//        } catch (Throwable e) {
//            return Mono.error(e);
//        }
//    }
//
//    @Override
//    public Mono<String> hset(byte[] key, Map<byte[], byte[]> map) {
//        try (RedisConn pool = connectionPool.borrowObject()) {
//            return pool.getConnection().hmset(key, map);
//        } catch (Throwable e) {
//            return Mono.error(e);
//        }
//    }
//
//    @Override
//    public void close() {
//        if (connectionPool != null) {
//            connectionPool.close();
//        }
//    }
//}
