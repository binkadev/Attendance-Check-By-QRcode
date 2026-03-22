package com.attendance.backend.fraud.repository;

import java.nio.ByteBuffer;
import java.util.UUID;

final class UuidBytes {

    private UuidBytes() {
    }

    static UUID fromOrderedBytes(byte[] dbBytes) {
        if (dbBytes == null) {
            return null;
        }
        if (dbBytes.length != 16) {
            throw new IllegalArgumentException("UUID binary length must be 16");
        }

        byte[] canonical = new byte[16];

        // DB bytes are stored like UUID_TO_BIN(uuid, 1)
        canonical[0] = dbBytes[4];
        canonical[1] = dbBytes[5];
        canonical[2] = dbBytes[6];
        canonical[3] = dbBytes[7];
        canonical[4] = dbBytes[2];
        canonical[5] = dbBytes[3];
        canonical[6] = dbBytes[0];
        canonical[7] = dbBytes[1];
        canonical[8] = dbBytes[8];
        canonical[9] = dbBytes[9];
        canonical[10] = dbBytes[10];
        canonical[11] = dbBytes[11];
        canonical[12] = dbBytes[12];
        canonical[13] = dbBytes[13];
        canonical[14] = dbBytes[14];
        canonical[15] = dbBytes[15];

        ByteBuffer bb = ByteBuffer.wrap(canonical);
        long high = bb.getLong();
        long low = bb.getLong();
        return new UUID(high, low);
    }
}