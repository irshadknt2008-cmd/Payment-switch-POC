package com.paymentswitch.common.util;

/** PAN (Primary Account Number) utilities. */
public final class PanUtil {
    private PanUtil() {}

    /** Mask a PAN for logging: show first 6 and last 4 digits. */
    public static String mask(String pan) {
        if (pan == null || pan.length() < 10) return "****";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    /** Validate PAN using the Luhn algorithm. */
    public static boolean isValidLuhn(String pan) {
        if (pan == null || pan.isEmpty()) return false;
        int sum = 0;
        boolean alternate = false;
        for (int i = pan.length() - 1; i >= 0; i--) {
            int n = Character.digit(pan.charAt(i), 10);
            if (n < 0) return false;
            if (alternate) { n *= 2; if (n > 9) n -= 9; }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }

    /** Extract the Bank Identification Number (first 6 digits). */
    public static String extractBin(String pan) {
        if (pan == null || pan.length() < 6) return "";
        return pan.substring(0, 6);
    }
}
