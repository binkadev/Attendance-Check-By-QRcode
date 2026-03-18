package com.attendance.backend.common.persistence;

import java.nio.ByteBuffer;
import java.util.UUID;

public final class UuidBytes {

    private UuidBytes() {
    }

    public static UUID fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("UUID bytes must have length 16");
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}