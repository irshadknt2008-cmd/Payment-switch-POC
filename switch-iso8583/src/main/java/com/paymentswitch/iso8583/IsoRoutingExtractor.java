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
        BitmapParseResult bitmap = parseBitmap(raw, pos);
        if (bitmap == null) return;
        boolean[] fields = bitmap.fields;
        pos = bitmap.nextPos;

        if (fields[1]) {
            BitmapParseResult secondary = parseBitmap(raw, pos);
            if (secondary != null) {
                for (int de = 65; de <= 128; de++) {
                    fields[de] = secondary.fields[de - 64];
                }
                pos = secondary.nextPos;
            }
        }

        // Routing only needs PAN and STAN. Stop after DE 11 so malformed
        // later fields cannot block issuer forwarding.
        for (int de = 2; de <= 11 && pos < raw.length; de++) {
            if (!fields[de]) {
                continue;
            }

            int[] next = skipCoreField(raw, pos, de);
            if (next == null) {
                break;
            }

            if (de == 2 && next[1] > 0) {
                String pan = new String(raw, pos, Math.min(next[1], raw.length - pos), StandardCharsets.US_ASCII);
                msg.setPan(pan);
                msg.setField(2, pan);
            } else if (de == 11 && next[1] >= 6) {
                String stan = new String(raw, pos, 6, StandardCharsets.US_ASCII);
                msg.setSystemTraceAuditNumber(stan);
                msg.setField(11, stan);
            }

            pos = next[0];
        }
    }

    private static BitmapParseResult parseBitmap(byte[] raw, int offset) {
        BitmapParseResult ascii = parseAsciiBitmap(raw, offset);
        if (ascii != null) {
            return ascii;
        }
        return parseBinaryBitmap(raw, offset);
    }

    private static BitmapParseResult parseAsciiBitmap(byte[] raw, int offset) {
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
        return new BitmapParseResult(present, offset + 16);
    }

    private static BitmapParseResult parseBinaryBitmap(byte[] raw, int offset) {
        if (raw.length < offset + 8) return null;

        boolean[] present = new boolean[129];
        boolean secondary = (raw[offset] & 0x80) != 0;

        for (int i = 0; i < 8; i++) {
            int value = raw[offset + i] & 0xFF;
            for (int bit = 0; bit < 8; bit++) {
                if ((value & (1 << (7 - bit))) != 0) {
                    present[i * 8 + bit + 1] = true;
                }
            }
        }

        int nextPos = offset + 8;
        if (secondary) {
            if (raw.length < nextPos + 8) return null;
            for (int i = 0; i < 8; i++) {
                int value = raw[nextPos + i] & 0xFF;
                for (int bit = 0; bit < 8; bit++) {
                    if ((value & (1 << (7 - bit))) != 0) {
                        present[64 + i * 8 + bit + 1] = true;
                    }
                }
            }
            nextPos += 8;
        }

        return new BitmapParseResult(present, nextPos);
    }

    private static final class BitmapParseResult {
        final boolean[] fields;
        final int nextPos;

        BitmapParseResult(boolean[] fields, int nextPos) {
            this.fields = fields;
            this.nextPos = nextPos;
        }
    }

    /** Returns [nextPosition, fieldLength] or null on error. */
    private static int[] skipCoreField(byte[] raw, int pos, int de) {
        if (pos >= raw.length) return null;

        switch (de) {
            case 2:
                return readLlvar(raw, pos);
            case 3:
                return fixed(raw, pos, 6);
            case 4:
            case 5:
            case 6:
                return fixed(raw, pos, 12);
            case 7:
                return fixed(raw, pos, 10);
            case 8:
            case 9:
            case 10:
                return fixed(raw, pos, 8);
            case 11:
                return fixed(raw, pos, 6);
            default:
                return null;
        }
    }

    private static int[] fixed(byte[] raw, int pos, int len) {
        if (pos + len > raw.length) return null;
        return new int[]{pos + len, len};
    }

    private static int[] readLlvar(byte[] raw, int pos) {
        if (pos + 2 <= raw.length) {
            int len = parseInt2(raw, pos);
            if (len >= 0 && pos + 2 + len <= raw.length) {
                return new int[]{pos + 2 + len, len};
            }
        }

        if (pos + 1 <= raw.length) {
            int bcdLen = parseBcd2(raw[pos]);
            if (bcdLen >= 0 && pos + 1 + bcdLen <= raw.length) {
                return new int[]{pos + 1 + bcdLen, bcdLen};
            }

            int binaryLen = raw[pos] & 0xFF;
            if (pos + 1 + binaryLen <= raw.length) {
                return new int[]{pos + 1 + binaryLen, binaryLen};
            }
        }

        return null;
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

    private static int parseBcd2(byte b) {
        int hi = (b >> 4) & 0x0F;
        int lo = b & 0x0F;
        if (hi > 9 || lo > 9) {
            return -1;
        }
        return hi * 10 + lo;
    }
}
