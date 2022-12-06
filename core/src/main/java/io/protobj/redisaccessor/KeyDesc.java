package io.protobj.redisaccessor;

public interface KeyDesc {

    int keyType();

    boolean many();

    KeyId createId(String namespace, byte[] bytes);
}
