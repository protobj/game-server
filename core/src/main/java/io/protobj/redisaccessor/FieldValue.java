package io.protobj.redisaccessor;

public interface FieldValue {

    KeyId keyId();

    FieldId fieldId();

    FieldDesc fieldDesc();

    default FieldId createFieldId() {
        FieldId fieldId = fieldId();
        if (fieldId != null) {
            throw new UnsupportedOperationException("create field id repeat");
        }
        FieldId fieldId0 = createFieldId0();
        initFieldId(fieldId0);
        return fieldId0;
    }

    void initFieldId(FieldId fieldId);

    FieldId createFieldId0();

    void initKeyId(KeyId keyId);
}
