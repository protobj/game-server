package io.protobj.redisaccessor.entity;

import io.protobj.redisaccessor.KeyDesc;
import io.protobj.redisaccessor.KeyId;
import io.protobj.redisaccessor.util.PersistenceRedisHelper;

public class SingleKeyId implements KeyId {

    protected final KeyDesc keyDesc;

    private final byte[] key;

    @Override
    public byte[] byteKey() {
        return key;
    }

    public SingleKeyId(String namespace, KeyDesc keyDesc) {
        this.keyDesc = keyDesc;
        this.key = PersistenceRedisHelper.getSaveKeyId(namespace, keyDesc);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof SingleKeyId singleKeyId) {
            return singleKeyId.keyDesc == this.keyDesc;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.keyDesc.keyType();
    }

    @Override
    public String toString() {
        return "SingleKeyId{" +
                "keyDesc=" + keyDesc +
                '}';
    }
}
