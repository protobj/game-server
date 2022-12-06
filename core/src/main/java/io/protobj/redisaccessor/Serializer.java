package io.protobj.redisaccessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ConcurrentModificationException;

public interface Serializer {

    Logger logger = LoggerFactory.getLogger(Serializer.class);

    default byte[] encode(FieldValue fieldValue) {
        try {
            return encode0(fieldValue);
        } catch (ConcurrentModificationException e) {
            return null;
        } catch (Throwable throwable) {
            logger.error("decode error", throwable);
            return null;
        }
    }

    byte[] encode0(FieldValue fieldValue);

    FieldValue decode(int fieldType, byte[] bytes);
}
