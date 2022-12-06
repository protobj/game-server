package io.protobj.redisaccessor.entity;

import io.protobj.redisaccessor.FieldDesc;
import io.protobj.redisaccessor.util.PersistenceRedisHelper;

public class IntFieldId extends SingleFieldId {

    private final int fieldId;

    public IntFieldId(FieldDesc fieldDesc, int fieldId) {
        super(fieldDesc);
        this.fieldId = fieldId;
    }

    @Override
    public byte[] byteId() {
        return PersistenceRedisHelper.encodeFieldId(fieldDesc, fieldId);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof IntFieldId intFieldId) {
            return fieldDesc == intFieldId.fieldDesc && intFieldId.fieldId == this.fieldId;
        }
        return false;
    }

    @Override
    public String toString() {
        return "IntFieldId{" +
                "fieldId=" + fieldId +
                ", fieldDesc=" + fieldDesc +
                '}';
    }
}
