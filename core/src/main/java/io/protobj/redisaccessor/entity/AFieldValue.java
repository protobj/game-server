package io.protobj.redisaccessor.entity;

import io.protobj.redisaccessor.FieldId;
import io.protobj.redisaccessor.FieldValue;
import io.protobj.redisaccessor.KeyId;

public abstract class AFieldValue implements FieldValue {

    private transient KeyId keyId;

    private transient FieldId fieldId;

    @Override
    public KeyId keyId() {
        return keyId;
    }

    @Override
    public FieldId fieldId() {
        return fieldId;
    }

    @Override
    public void initFieldId(FieldId fieldId) {
        this.fieldId = fieldId;
    }

    @Override
    public void initKeyId(KeyId keyId) {
        this.keyId = keyId;
    }
}
