package io.protobj.redisaccessor.entity;

import io.protobj.redisaccessor.FieldId;
import io.protobj.redisaccessor.FieldDesc;
import io.protobj.redisaccessor.util.PersistenceRedisHelper;

public class SingleFieldId implements FieldId {

    protected final FieldDesc fieldDesc;

    @Override
    public byte[] byteId() {
        return PersistenceRedisHelper.encodeFieldId(fieldDesc);
    }

    @Override
    public FieldDesc fieldDesc() {
        return fieldDesc;
    }

    public SingleFieldId(FieldDesc fieldDesc) {
        this.fieldDesc = fieldDesc;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof SingleFieldId singleFieldId) {
            return singleFieldId.fieldDesc == this.fieldDesc;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.fieldDesc.hashCode();
    }

    @Override
    public String toString() {
        return "SingleFieldId{" +
                "fieldDesc=" + fieldDesc +
                '}';
    }
}
