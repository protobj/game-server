package io.protobj.serializer;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import io.protobj.Json;
import io.protobj.Protobj;
import io.protobj.core.Schema;
import io.protobj.microserver.Server;
import io.protobj.network.Serializer;
import io.protobj.util.ByteBufUtil;
import io.protobj.util.Jackson;

import java.io.IOException;
import java.io.InputStream;

public class ProtobjSerializer implements Serializer {

    private BiMap<Integer, Class<?>> messageIds = HashBiMap.create();

    private Protobj protobj = new Protobj();

    public ProtobjSerializer() {
        InputStream stream = getClass().getClassLoader().getResourceAsStream("message.json");
        try {
            JsonNode jsonNode = Jackson.INSTANCE.readTree(stream);
            for (int i = 0; i < jsonNode.size(); i++) {
                JsonNode node = jsonNode.get(i);
                int id = node.get("id").asInt();
                String className = node.get("clazz").asText();
                Class<?> clazz = Class.forName(className);
                Class<?> schemaClazz = Class.forName(className + "Schema");
                messageIds.put(id, clazz);
                protobj.register(clazz, (Schema) schemaClazz.newInstance());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteBuf toByteArray(Object msg) {
        CompositeByteBuf byteBufs = Unpooled.compositeBuffer(2);
        ByteBuf buffer = Unpooled.buffer(4);
        Integer id = messageIds.inverse().get(msg.getClass());
        ByteBufUtil.writeVarInt(buffer, id);
        byteBufs.addComponent(buffer);
        try {
            byteBufs.addComponent(Unpooled.wrappedBuffer(protobj.writeToBytes(msg)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return byteBufs;
    }

    @Override
    public Object toObject(ByteBuf buf) {
        int id = ByteBufUtil.readVarInt(buf);
        Class<?> clazz = messageIds.get(id);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        try {
            return protobj.readFromBytes(clazz, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
