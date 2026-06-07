package com.paymentswitch.iso8583;

import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.FieldParseInfo;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Universal ISO 8583 parse guide (DE 2–128) applied to every supported MTI.
 */
final class Iso8583ParseGuideBuilder {

    private static final int[] ALL_MTIS = {
            100, 110, 200, 210, 400, 410, 420, 430, 800, 810
    };

    private Iso8583ParseGuideBuilder() {}

    static void applyTo(MessageFactory<?> factory) {
        Map<Integer, FieldParseInfo> guide = buildUniversalGuide();
        for (int mti : ALL_MTIS) {
            factory.setParseMap(mti, guide);
        }
    }

    private static Map<Integer, FieldParseInfo> buildUniversalGuide() {
        Map<Integer, FieldParseInfo> g = new LinkedHashMap<>();
        String e = "UTF-8";

        g.put(2,  f(IsoType.LLVAR,   0, e));
        g.put(3,  f(IsoType.NUMERIC, 6, e));
        g.put(4,  f(IsoType.NUMERIC, 12, e));
        g.put(5,  f(IsoType.NUMERIC, 12, e));
        g.put(6,  f(IsoType.NUMERIC, 12, e));
        g.put(7,  f(IsoType.DATE10,  0, e));
        g.put(8,  f(IsoType.NUMERIC, 8, e));
        g.put(9,  f(IsoType.NUMERIC, 8, e));
        g.put(10, f(IsoType.NUMERIC, 8, e));
        g.put(11, f(IsoType.NUMERIC, 6, e));
        g.put(12, f(IsoType.TIME,    0, e));
        g.put(13, f(IsoType.DATE4,   0, e));
        g.put(14, f(IsoType.DATE_EXP,0, e));
        g.put(15, f(IsoType.DATE4,   0, e));
        g.put(16, f(IsoType.NUMERIC, 4, e));
        g.put(17, f(IsoType.NUMERIC, 4, e));
        g.put(18, f(IsoType.NUMERIC, 4, e));
        g.put(19, f(IsoType.NUMERIC, 3, e));
        g.put(20, f(IsoType.NUMERIC, 3, e));
        g.put(22, f(IsoType.NUMERIC, 3, e));
        g.put(23, f(IsoType.NUMERIC, 3, e));
        g.put(24, f(IsoType.NUMERIC, 3, e));
        g.put(25, f(IsoType.NUMERIC, 2, e));
        g.put(26, f(IsoType.NUMERIC, 2, e));
        g.put(27, f(IsoType.NUMERIC, 1, e));
        g.put(28, f(IsoType.ALPHA,   9, e));
        g.put(29, f(IsoType.NUMERIC, 9, e));
        g.put(30, f(IsoType.NUMERIC, 8, e));
        g.put(31, f(IsoType.LLVAR,   0, e));
        g.put(32, f(IsoType.LLVAR,   0, e));
        g.put(33, f(IsoType.LLVAR,   0, e));
        g.put(34, f(IsoType.LLVAR,   0, e));
        g.put(35, f(IsoType.LLVAR,   0, e));
        g.put(36, f(IsoType.LLLVAR,  0, e));
        g.put(37, f(IsoType.ALPHA,   12, e));
        g.put(38, f(IsoType.ALPHA,   6, e));
        g.put(39, f(IsoType.ALPHA,   2, e));
        g.put(40, f(IsoType.LLVAR,   0, e));
        g.put(41, f(IsoType.ALPHA,   8, e));
        g.put(42, f(IsoType.ALPHA,   15, e));
        g.put(43, f(IsoType.ALPHA,   40, e));
        g.put(44, f(IsoType.LLVAR,   0, e));
        g.put(45, f(IsoType.LLVAR,   0, e));
        g.put(46, f(IsoType.LLLVAR,  0, e));
        g.put(47, f(IsoType.LLLVAR,  0, e));
        g.put(48, f(IsoType.LLLVAR,  0, e));
        g.put(49, f(IsoType.NUMERIC, 3, e));
        g.put(50, f(IsoType.ALPHA,   3, e));
        g.put(51, f(IsoType.NUMERIC, 3, e));
        g.put(52, f(IsoType.BINARY,  8, e));
        g.put(53, f(IsoType.LLVAR,   0, e));
        g.put(54, f(IsoType.LLVAR,   0, e));
        g.put(55, f(IsoType.LLLVAR,  0, e));
        g.put(56, f(IsoType.LLVAR,   0, e));
        g.put(57, f(IsoType.LLLVAR,  0, e));
        g.put(58, f(IsoType.LLVAR,   0, e));
        g.put(59, f(IsoType.LLLVAR,  0, e));
        g.put(60, f(IsoType.LLLVAR,  0, e));
        g.put(61, f(IsoType.LLLVAR,  0, e));
        g.put(62, f(IsoType.LLLVAR,  0, e));
        g.put(63, f(IsoType.LLLVAR,  0, e));
        g.put(64, f(IsoType.BINARY,  8, e));

        // Secondary bitmap – variable private fields default to LLLVAR
        for (int de = 65; de <= 128; de++) {
            g.put(de, f(IsoType.LLLVAR, 0, e));
        }
        g.put(65, f(IsoType.BINARY,  8, e));
        g.put(66, f(IsoType.LLVAR,   0, e));
        g.put(67, f(IsoType.NUMERIC, 2, e));
        g.put(69, f(IsoType.NUMERIC, 3, e));
        g.put(70, f(IsoType.NUMERIC, 3, e));
        g.put(74, f(IsoType.NUMERIC, 10, e));
        g.put(90, f(IsoType.LLVAR,   0, e));
        g.put(95, f(IsoType.LLVAR,   0, e));
        g.put(123, f(IsoType.LLLVAR, 0, e));
        g.put(128, f(IsoType.BINARY,  8, e));

        return g;
    }

    private static FieldParseInfo f(IsoType type, int length, String encoding) {
        return FieldParseInfo.getInstance(type, length, encoding);
    }
}
