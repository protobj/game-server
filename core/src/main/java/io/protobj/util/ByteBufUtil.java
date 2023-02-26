package io.protobj.util;

import io.netty.buffer.ByteBuf;

public class ByteBufUtil {

    public static int writeVarInt(ByteBuf buf, int value) {
        value = (value << 1) ^ (value >> 31);
        int a = value >>> 7;
        if (a == 0) {
            buf.writeByte(value);
            return 1;
        }

        int writeIndex = buf.writerIndex();
        buf.ensureWritable(5);

        buf.setByte(writeIndex++, value | 0x80);
        int b = value >>> 14;
        if (b == 0) {
            buf.setByte(writeIndex++, a);
            buf.writerIndex(writeIndex);
            return 2;
        }

        buf.setByte(writeIndex++, a | 0x80);
        a = value >>> 21;
        if (a == 0) {
            buf.setByte(writeIndex++, b);
            buf.writerIndex(writeIndex);
            return 3;
        }

        buf.setByte(writeIndex++, b | 0x80);
        b = value >>> 28;
        if (b == 0) {
            buf.setByte(writeIndex++, a);
            buf.writerIndex(writeIndex);
            return 4;
        }
        buf.setByte(writeIndex++, a | 0x80);
        buf.setByte(writeIndex++, b);
        buf.writerIndex(writeIndex);
        return 5;
    }
    public static int readVarInt(ByteBuf byteBuf) {
        int readIndex = byteBuf.readerIndex();
        int b = byteBuf.getByte(readIndex++);
        int value = b;
        if (b < 0) {
            b = byteBuf.getByte(readIndex++);
            value = value & 0x0000007F | b << 7;
            if (b < 0) {
                b = byteBuf.getByte(readIndex++);
                value = value & 0x00003FFF | b << 14;
                if (b < 0) {
                    b = byteBuf.getByte(readIndex++);
                    value = value & 0x001FFFFF | b << 21;
                    if (b < 0) {
                        value = value & 0x0FFFFFFF | byteBuf.getByte(readIndex++) << 28;
                    }
                }
            }
        }
        byteBuf.readerIndex(readIndex);
        return ((value >>> 1) ^ -(value & 1));
    }

    public static int writeIntCount(int value) {
        value = (value << 1) ^ (value >> 31);

        if (value >>> 7 == 0) {
            return 1;
        }

        if (value >>> 14 == 0) {
            return 2;
        }

        if (value >>> 21 == 0) {
            return 3;
        }

        if (value >>> 28 == 0) {
            return 4;
        }

        return 5;
    }

}
