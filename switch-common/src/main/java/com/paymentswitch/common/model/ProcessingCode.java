package com.paymentswitch.common.model;

/** ISO 8583 DE 3 Processing Code. */
public enum ProcessingCode {
    PURCHASE("000000"),
    CASH_ADVANCE("010000"),
    REFUND("200000"),
    BALANCE_INQUIRY("310000"),
    MINI_STATEMENT("380000");

    private final String code;
    ProcessingCode(String code) { this.code = code; }
    public String getCode() { return code; }

    public static ProcessingCode fromCode(String code) {
        for (ProcessingCode pc : values()) {
            if (pc.code.equals(code)) return pc;
        }
        throw new IllegalArgumentException("Unknown processing code: " + code);
    }
}
