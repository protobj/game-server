package io.protobj.services.transport.api;

import io.netty.buffer.*;
import io.protobj.services.api.Message;
import io.protobj.services.exceptions.MessageCodecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.function.BiFunction;

public class MessageCodec {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageCodec.class);


    private final HeadersCodec headersCodec;
    private final ContentCodec contentCodec;

    public MessageCodec(HeadersCodec headersCodec, ContentCodec contentCodec) {
        this.headersCodec = headersCodec;
        this.contentCodec = contentCodec;
    }

    public <T> T encodeAndTransform(Message message, BiFunction<ByteBuf, ByteBuf, T> transformer) throws MessageCodecException {
        ByteBuf contentBuffer = Unpooled.EMPTY_BUFFER;
        ByteBuf headersBuffer = Unpooled.EMPTY_BUFFER;

        if (message.getContent() != null) {
            contentBuffer = ByteBufAllocator.DEFAULT.buffer();
            try {
                this.contentCodec.encode(new ByteBufOutputStream(contentBuffer), message.getContent());
            } catch (Throwable ex) {
                ReferenceCountUtil.safestRelease(contentBuffer);
                LOGGER.error("Failed to encode service message data on: {}, cause: {}", message, ex.toString());
                throw new MessageCodecException("Failed to encode service message data", ex);
            }
        }
        if (message.getHeader() != null) {
            headersBuffer = ByteBufAllocator.DEFAULT.buffer();
            try {
                headersCodec.encode(new ByteBufOutputStream(headersBuffer), message.getHeader());
            } catch (Throwable ex) {
                ReferenceCountUtil.safestRelease(headersBuffer);
                ReferenceCountUtil.safestRelease(contentBuffer); // release data buf as well
                LOGGER.error("Failed to encode service message headers on: {}, cause: {}", message, ex.toString());
                throw new MessageCodecException("Failed to encode service message headers", ex);
            }
        }

        return transformer.apply(contentBuffer, headersBuffer);
    }

    public Message.Header decodeHeader(ByteBuf headersBuffer) throws MessageCodecException {

        if (headersBuffer.isReadable()) {
            try (ByteBufInputStream stream = new ByteBufInputStream(headersBuffer, true)) {
                return headersCodec.decode(stream);
            } catch (Throwable ex) {
                ReferenceCountUtil.safestRelease(headersBuffer); // release data buf as well
                throw new MessageCodecException("Failed to decode service message headers", ex);
            }
        }
        return null;
    }

    public Message decodeContent(Message.Header header, ByteBuf dataBuffer, Type responseType) throws MessageCodecException {
        Message message = new Message();
        message.setHeader(header);
        if (dataBuffer.isReadable()) {
            try (ByteBufInputStream stream = new ByteBufInputStream(dataBuffer, true)) {
                if (header.isError()) {
                    responseType = Message.ErrorData.class;
                }
                message.setContent(contentCodec.decode(stream, responseType));
            } catch (Throwable ex) {
                ReferenceCountUtil.safestRelease(dataBuffer); // release data buf as well
                throw new MessageCodecException("Failed to decode service message headers", ex);
            }
        }
        return message;
    }

}
