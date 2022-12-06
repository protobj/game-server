package io.protobj.redisaccessor.entity;

import io.protobj.redisaccessor.KeyDesc;
import io.protobj.redisaccessor.KeyId;
import io.protobj.redisaccessor.util.PersistenceRedisHelper;

import java.util.Objects;

public class StringKeyId implements KeyId {

    private final KeyDesc keyDesc;
    private final String keyId;

    private final int hashcode;

    private final byte[] key;

    public StringKeyId(String namespace, KeyDesc keyDesc, String
            keyId) {
        this.keyId = keyId;
        this.keyDesc = keyDesc;
        this.hashcode = Objects.hash(keyDesc.keyType(), keyId);
        this.key = PersistenceRedisHelper.getSaveKeyId(namespace, keyDesc, keyId);
    }

    @Override
    public byte[] byteKey() {
        return key;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StringKeyId longKeyId) {
            return longKeyId.keyDesc == this.keyDesc && longKeyId.keyId.equals(this.keyId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        return "StringKeyId{" +
                "keyDesc=" + keyDesc +
                ", keyId=" + keyId +
                '}';
    }
}
