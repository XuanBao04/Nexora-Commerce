package com.nexoracommerce.common.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.UUID;

/**
 * Utility class for UUID operations, specifically UUID v7 generation.
 */
public final class UUIDUtils {

    private static final SecureRandom random = new SecureRandom();

    private UUIDUtils() {
        // Prevent instantiation
    }

    /**
     * Generates a time-ordered UUID v7.
     * UUID v7 layout:
     * - 48 bits: timestamp (milliseconds since epoch)
     * - 4 bits: version (0111 = 7)
     * - 12 bits: rand_a (random data)
     * - 2 bits: variant (10 = RFC 4122)
     * - 62 bits: rand_b (random data)
     *
     * @return a time-ordered UUID v7
     */
    public static UUID generateUUIDv7() {
        byte[] value = new byte[16];
        random.nextBytes(value);

        long timestamp = System.currentTimeMillis();

        // 48-bit timestamp in big-endian order
        value[0] = (byte) ((timestamp >> 40) & 0xFF);
        value[1] = (byte) ((timestamp >> 32) & 0xFF);
        value[2] = (byte) ((timestamp >> 24) & 0xFF);
        value[3] = (byte) ((timestamp >> 16) & 0xFF);
        value[4] = (byte) ((timestamp >> 8) & 0xFF);
        value[5] = (byte) (timestamp & 0xFF);

        // Version: set bits 4-7 of byte 6 to 0111 (version 7)
        value[6] = (byte) ((value[6] & 0x0F) | 0x70);

        // Variant: set bits 6-7 of byte 8 to 10 (RFC 4122)
        value[8] = (byte) ((value[8] & 0x3F) | 0x80);

        ByteBuffer buffer = ByteBuffer.wrap(value);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
