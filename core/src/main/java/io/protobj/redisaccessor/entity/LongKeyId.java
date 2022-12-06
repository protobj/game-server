package io.protobj.redisaccessor.entity;

import io.protobj.redisaccessor.KeyDesc;
import io.protobj.redisaccessor.KeyId;
import io.protobj.redisaccessor.util.PersistenceRedisHelper;

import java.util.Objects;

public class LongKeyId implements KeyId {

    private final KeyDesc keyDesc;
    private final long keyId;

    private final int hashcode;

    private final byte[] key;

    @Override
    public byte[] byteKey() {
        return key;
    }

    public LongKeyId(String namespace, KeyDesc keyDesc, long keyId) {
        this.keyId = keyId;
        this.keyDesc = keyDesc;
        this.hashcode = Objects.hash(keyDesc.keyType(), keyId);
        this.key = PersistenceRedisHelper.getSaveKeyId(namespace, keyDesc, keyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof LongKeyId longKeyId) {
            return longKeyId.keyDesc == this.keyDesc && longKeyId.keyId == this.keyId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return hashcode;
    }

    @Override
    public String toString() {
        return "LongKeyId{" +
                "keyDesc=" + keyDesc +
                ", keyId=" + keyId +
                '}';
    }
}
