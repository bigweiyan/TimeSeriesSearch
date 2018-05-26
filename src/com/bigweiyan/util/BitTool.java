package com.bigweiyan.util;

public class BitTool {
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset + 3] & 0xFF) | ((src[offset + 2] & 0xFF) << 8) | ((src[offset + 2] & 0xFF) << 16) | ((src[offset] & 0xFF) << 24));
        return value;
    }
    public static double bytesToDouble(byte[] src, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (src[7 - i + offset] & 0xff)) << (8 * i);
        }
        return Double.longBitsToDouble(value);
    }
}
