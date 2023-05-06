package io.protobj.services.transport.codec;

import io.protobj.services.api.Message;
import io.protobj.services.transport.api.ContentCodec;
import io.protobj.services.transport.api.HeadersCodec;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtobufIOUtil;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ProtostuffCodec implements HeadersCodec, ContentCodec {
    @Override
    public void encode(OutputStream stream, Message.Content value) throws IOException {
        //noinspection rawtypes
        Schema schema = RuntimeSchema.getSchema(value.getClass());
        //noinspection unchecked
        ProtobufIOUtil.writeTo(stream, value, schema, LinkedBuffer.allocate());
    }

    @Override
    public Message.Content decode(InputStream stream, Type type) throws IOException {
        try {
            Class<?> clazz = null;
            if (type instanceof Class<?>) {
                clazz = (Class<?>) type;
            } else if (type instanceof ParameterizedType) {
                clazz = Class.forName(((ParameterizedType) type).getRawType().getTypeName());
            }
            //noinspection rawtypes
            Schema schema = RuntimeSchema.getSchema(clazz);


            Object result = schema.newMessage();
            //noinspection unchecked
            ProtobufIOUtil.mergeFrom(stream, result, schema, LinkedBuffer.allocate());
            return (Message.Content) result;
        } catch (ClassNotFoundException e) {
            //TODO throw new MessageCodecException("Couldn't decode message", e);
        }
        return null;
    }

    private final Schema<Message.Header> HEADER_SCHEMA = RuntimeSchema.getSchema(Message.Header.class);

    @Override
    public void encode(OutputStream stream, Message.Header header) throws IOException {
        ProtostuffIOUtil.writeTo(stream, header, HEADER_SCHEMA, LinkedBuffer.allocate());
    }

    @Override
    public Message.Header decode(InputStream stream) throws IOException {
        Message.Header header = new Message.Header();
        ProtostuffIOUtil.mergeFrom(stream, header, HEADER_SCHEMA);
        return header;
    }
}
