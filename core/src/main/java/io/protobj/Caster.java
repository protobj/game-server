package io.protobj;

public interface Caster {

    default <T> T cast() {
        return (T) this;
    }

    default <T> T cast(Class<T> clazz) {
        return (T) this;
    }


}
