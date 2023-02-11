package io.protobj.util;

public interface IdGenerator {

    long generateId();

    default String generateStringId() {
        return String.valueOf(generateId());
    }
}
