package io.protobj.redisaccessor.serializer;

import io.lettuce.core.codec.RedisCodec;
import io.protobj.IServer;
import io.protobj.Module;
import io.protobj.redisaccessor.FieldDesc;
import io.protobj.redisaccessor.FieldValue;
import io.protobj.redisaccessor.anno.Value;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.*;

public class FieldValueCodec implements RedisCodec<byte[], FieldValue> {

    private final Map<Short, Schema<?>> schemaMap = new HashMap<>();

    public FieldValueCodec(List<Module> moduleList) {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.addScanners(Scanners.SubTypes);
        for (Module module : moduleList) {
            configurationBuilder.forPackages(module.getClass().getPackage().getName() + "." + IServer.ENTITY_PACKAGE);
        }
        Reflections reflections = new Reflections(configurationBuilder);
        Set<Class<? extends FieldValue>> subTypesOf = reflections.getSubTypesOf(FieldValue.class);
        for (Class<? extends FieldValue> aClass : subTypesOf) {
            if (Modifier.isAbstract(aClass.getModifiers()) || Modifier.isInterface(aClass.getModifiers())) {
                continue;
            }
            if (aClass.getAnnotation(Value.class) == null) {
                continue;
            }
            try {
                FieldValue value = aClass.getConstructor().newInstance();
                FieldDesc fieldDesc = value.fieldDesc();
                short id = getMsgId(fieldDesc);
                Schema<?> schema = RuntimeSchema.getSchema(aClass);
                schemaMap.put(id, schema);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private short getMsgId(FieldDesc fieldDesc) {
        short fieldType = fieldDesc.fieldType();
        int i = fieldDesc.keyDesc().keyType();
        return (short) ((i << 8) | ((byte) fieldType));
    }

    @Override
    public byte[] decodeKey(ByteBuffer bytes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FieldValue decodeValue(ByteBuffer bytes) {
        short keyId = bytes.getShort();
        Schema valueSchema = schemaMap.get(keyId);
        byte[] byteArray = new byte[bytes.remaining()];
        bytes.get(byteArray);
        Object message = valueSchema.newMessage();
        ProtostuffIOUtil.mergeFrom(byteArray, message, valueSchema);
        return (FieldValue) message;
    }

    @Override
    public ByteBuffer encodeKey(byte[] key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer encodeValue(FieldValue value) {
        try {
            short msgId = getMsgId(value.fieldDesc());
            Schema valueSchema = schemaMap.get(msgId);
            byte[] bytes = new byte[258];
            bytes[0] = (byte) (msgId >> 8);
            bytes[1] = (byte) msgId;
            int size;
            LinkedBuffer buffer = LinkedBuffer.use(bytes, 2);
            try {
                size = ProtostuffIOUtil.writeTo(buffer, value, valueSchema);
            } finally {
                buffer.clear();
            }
            return ByteBuffer.wrap(bytes, 0, size + 2);
        } catch (ConcurrentModificationException e) {
            return encodeValue(value);
        }
    }

}
