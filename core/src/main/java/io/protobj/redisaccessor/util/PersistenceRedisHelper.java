package io.protobj.redisaccessor.util;

import io.protobj.redisaccessor.FieldDesc;
import io.protobj.redisaccessor.KeyDesc;

import java.nio.charset.StandardCharsets;

public class PersistenceRedisHelper {
    private static final String SPLITTER1 = ":";
    private static final String SPLITTER2 = "_";

    public static byte[] getSaveKeyId(String namespace, KeyDesc keyDesc, long mainEntityId) {
        return (namespace + SPLITTER1 + keyDesc.keyType() + SPLITTER2 + mainEntityId).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getSaveKeyId(String namespace, KeyDesc keyDesc, String mainEntityId) {
        return (namespace + SPLITTER1 + keyDesc.keyType() + SPLITTER2 + mainEntityId).getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] getSaveKeyId(String namespace, KeyDesc keyDesc) {
        return (namespace + SPLITTER1 + keyDesc.keyType()).getBytes(StandardCharsets.UTF_8);
    }

    public static String getSingleKeyId(String namespace, KeyDesc keyDesc) {
        return namespace + SPLITTER1 + keyDesc.keyType();
    }

    public static String getManyKeyIdPattern(String namespace, KeyDesc keyDesc) {
        return namespace + SPLITTER1 + keyDesc.keyType() + SPLITTER2 + "*";
    }

    public static int readIntValue(byte[] keyBytes, int position) {
        int b = keyBytes[position++];
        int value = b & 0x7F;
        if ((b & 0x80) != 0) {
            b = keyBytes[position++];
            value |= (b & 0x7F) << 7;
            if ((b & 0x80) != 0) {
                b = keyBytes[position++];
                value |= (b & 0x7F) << 14;
                if ((b & 0x80) != 0) {
                    b = keyBytes[position++];
                    value |= (b & 0x7F) << 21;
                    if ((b & 0x80) != 0) {
                        b = keyBytes[position++];
                        value |= (b & 0x7F) << 28;
                    }
                }
            }
        }
        return value;
    }

    public static int getVarIntLength(int value) {
        if (value >>> 7 == 0) return 1;
        if (value >>> 14 == 0) return 2;
        if (value >>> 21 == 0) return 3;
        if (value >>> 28 == 0) return 4;
        return 5;
    }

    public static byte[] encodeFieldId(FieldDesc fieldDesc, int primaryKey) {
        int fieldTypeLength = getVarIntLength(fieldDesc.fieldType());
        int primaryKeyLength = getVarIntLength(primaryKey);
        byte[] bytes = new byte[fieldTypeLength + primaryKeyLength];
        write(bytes, fieldTypeLength, 0, fieldDesc.fieldType());
        write(bytes, primaryKeyLength, fieldTypeLength, primaryKey);
        return bytes;
    }

    public static byte[] encodeFieldId(FieldDesc fieldDesc) {
        int fieldTypeLength = getVarIntLength(fieldDesc.fieldType());
        byte[] bytes = new byte[fieldTypeLength];
        write(bytes, fieldTypeLength, 0, fieldDesc.fieldType());
        return bytes;
    }

    public static void write(byte[] bytes, int length, int index, int value) {
        switch (length) {
            case 1 -> bytes[index] = (byte) value;
            case 2 -> {
                bytes[index] = (byte) ((value & 0x7F) | 0x80);
                bytes[index + 1] = (byte) (value >>> 7);
            }
            case 3 -> {
                bytes[index] = (byte) ((value & 0x7F) | 0x80);
                bytes[index + 1] = (byte) (value >>> 7 | 0x80);
                bytes[index + 2] = (byte) (value >>> 14);
            }
            case 4 -> {
                bytes[index] = (byte) ((value & 0x7F) | 0x80);
                bytes[index + 1] = (byte) (value >>> 7 | 0x80);
                bytes[index + 2] = (byte) (value >>> 14 | 0x80);
                bytes[index + 3] = (byte) (value >>> 21);
            }
            case 5 -> {
                bytes[index] = (byte) ((value & 0x7F) | 0x80);
                bytes[index + 1] = (byte) (value >>> 7 | 0x80);
                bytes[index + 2] = (byte) (value >>> 14 | 0x80);
                bytes[index + 3] = (byte) (value >>> 21 | 0x80);
                bytes[index + 4] = (byte) (value >>> 28);
            }
        }
    }


    public static String getStrKeyId(byte[] bytes) {
        String strKey = new String(bytes, StandardCharsets.UTF_8);
        return strKey.substring(strKey.lastIndexOf(SPLITTER2) + 1);
    }

}
