package com.bigweiyan.util;

public class BitTool {
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (int) ((src[offset + 3] & 0xFF) | ((src[offset + 2] & 0xFF) << 8) | ((src[offset + 1] & 0xFF) << 16) | ((src[offset] & 0xFF) << 24));
        return value;
    }

    public static void intToBytes(int value, byte[] dst, int offset) {
        dst[offset] = (byte)((value >> 24) & 0xFF);
        dst[offset + 1] = (byte)((value >> 16) & 0xFF);
        dst[offset + 2] = (byte)((value >> 8) & 0xFF);
        dst[offset + 3] = (byte)(value & 0xFF);
    }

    public static double bytesToDouble(byte[] src, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value |= ((long) (src[7 - i + offset] & 0xff)) << (8 * i);
        }
        return Double.longBitsToDouble(value);
    }

    public static void doubleToBytes(double value, byte[] dst, int offset) {
        long tmp = Double.doubleToLongBits(value);
        for (int i = 0; i < 8; i++) {
            dst[offset + 7 - i] = (byte)((tmp >> (8 * i)) & 0xff);
        }
    }
}
