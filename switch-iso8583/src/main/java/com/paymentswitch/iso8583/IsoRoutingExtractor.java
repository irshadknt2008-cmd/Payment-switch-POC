package com.paymentswitch.iso8583;

import com.paymentswitch.common.model.SwitchMessage;

import java.nio.charset.StandardCharsets;

/**
 * Lightweight ISO 8583 field walker for routing metadata (PAN, STAN) when full parse fails.
 * Works with Neapay ASCII hex bitmap format.
 */
final class IsoRoutingExtractor {

    private IsoRoutingExtractor() {}

    static void extract(byte[] raw, SwitchMessage msg) {
        if (raw.length < 20) return;

        int pos = 4;
        boolean[] fields = parseAsciiBitmap(raw, pos);
        if (fields == null) return;
        pos += 16;

        if (fields[1] && raw.length >= pos + 16) {
            boolean[] secondary = parseAsciiBitmap(raw, pos);
            if (secondary != null) {
                for (int de = 65; de <= 128; de++) {
                    fields[de] = secondary[de - 64];
                }
                pos += 16;
            }
        }

        for (int de = 2; de <= 128 && pos < raw.length; de++) {
            if (!fields[de]) continue;
            int[] next = skipField(raw, pos, de);
            if (next == null) break;
            if (de == 2 && next[1] > 0) {
                String pan = new String(raw, pos, Math.min(next[1], raw.length - pos), StandardCharsets.US_ASCII);
                msg.setPan(pan);
                msg.setField(2, pan);
            }
            if (de == 11 && next[1] >= 6) {
                String stan = new String(raw, pos, 6, StandardCharsets.US_ASCII);
                msg.setSystemTraceAuditNumber(stan);
                msg.setField(11, stan);
            }
            pos = next[0];
        }
    }

    private static boolean[] parseAsciiBitmap(byte[] raw, int offset) {
        if (raw.length < offset + 16) return null;
        String hex = new String(raw, offset, 16, StandardCharsets.US_ASCII);
        boolean[] present = new boolean[129];
        StringBuilder bits = new StringBuilder();
        for (int i = 0; i < hex.length(); i++) {
            int v = Character.digit(hex.charAt(i), 16);
            if (v < 0) return null;
            bits.append(String.format("%4s", Integer.toBinaryString(v)).replace(' ', '0'));
        }
        for (int i = 0; i < 64 && i < bits.length(); i++) {
            if (bits.charAt(i) == '1') {
                present[i + 1] = true;
            }
        }
        return present;
    }

    /** Returns [nextPosition, fieldLength] or null on error. */
    private static int[] skipField(byte[] raw, int pos, int de) {
        if (pos >= raw.length) return null;

        switch (de) {
            case 2: case 32: case 33: case 34: case 35:
            case 40: case 44: case 53: case 54: case 56:
                return readLlvar(raw, pos);
            case 41:
                return fixed(raw, pos, 8);
            case 48: case 55: case 57: case 59: case 60: case 61: case 62: case 63:
                return readLllvar(raw, pos);
            case 3: case 4: case 5: case 6: case 8: case 9: case 10:
            case 11: case 16: case 17: case 18: case 19: case 20:
            case 22: case 23: case 24: case 25: case 26:
                return fixed(raw, pos, fieldLen(de));
            case 7:
                return fixed(raw, pos, 10);
            case 12: case 13:
                return fixed(raw, pos, 6);
            case 14:
                return fixed(raw, pos, 4);
            case 27:
                return fixed(raw, pos, 1);
            case 28:
                return fixed(raw, pos, 9);
            case 30:
                return fixed(raw, pos, 8);
            case 37:
                return fixed(raw, pos, 12);
            case 38:
                return fixed(raw, pos, 6);
            case 39:
                return fixed(raw, pos, 2);
            case 42:
                return fixed(raw, pos, 15);
            case 43:
                return fixed(raw, pos, 40);
            case 49: case 51:
                return fixed(raw, pos, 3);
            case 50:
                return fixed(raw, pos, 3);
            case 52: case 64:
                return fixed(raw, pos, 8);
            case 70:
                return fixed(raw, pos, 3);
            default:
                return readLlvar(raw, pos);
        }
    }

    private static int fieldLen(int de) {
        if (de == 11) return 6;
        if (de == 3) return 6;
        if (de == 4 || de == 5 || de == 6) return 12;
        return 6;
    }

    private static int[] fixed(byte[] raw, int pos, int len) {
        if (pos + len > raw.length) return null;
        return new int[]{pos + len, len};
    }

    private static int[] readLlvar(byte[] raw, int pos) {
        if (pos + 2 > raw.length) return null;
        int len = parseInt2(raw, pos);
        if (len < 0 || pos + 2 + len > raw.length) return null;
        return new int[]{pos + 2 + len, len};
    }

    private static int[] readLllvar(byte[] raw, int pos) {
        if (pos + 3 > raw.length) return null;
        int len = parseInt3(raw, pos);
        if (len < 0 || pos + 3 + len > raw.length) return null;
        return new int[]{pos + 3 + len, len};
    }

    private static int parseInt2(byte[] raw, int pos) {
        try {
            return Integer.parseInt(new String(raw, pos, 2, StandardCharsets.US_ASCII).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static int parseInt3(byte[] raw, int pos) {
        try {
            return Integer.parseInt(new String(raw, pos, 3, StandardCharsets.US_ASCII).trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
