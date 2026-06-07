package com.paymentswitch.common.model;

/** ISO 8583 Message Type Identifier (MTI) abstraction. */
public enum MessageType {
    AUTHORIZATION_REQUEST("0100"),
    AUTHORIZATION_RESPONSE("0110"),
    FINANCIAL_REQUEST("0200"),
    FINANCIAL_RESPONSE("0210"),
    REVERSAL_REQUEST("0400"),
    REVERSAL_RESPONSE("0410"),
    REVERSAL_ADVICE_REQUEST("0420"),
    REVERSAL_ADVICE_RESPONSE("0430"),
    NETWORK_MANAGEMENT_REQUEST("0800"),
    NETWORK_MANAGEMENT_RESPONSE("0810");

    private final String mti;
    MessageType(String mti) { this.mti = mti; }
    public String getMti() { return mti; }

    public static MessageType fromMti(String mti) {
        for (MessageType t : values()) {
            if (t.mti.equals(mti)) return t;
        }
        throw new IllegalArgumentException("Unknown MTI: " + mti);
    }

    /** Returns matching type or best-guess from MTI digits (never throws). */
    public static MessageType fromMtiSafe(String mti) {
        for (MessageType t : values()) {
            if (t.mti.equals(mti)) return t;
        }
        if (mti != null && mti.length() == 4 && mti.charAt(2) == '1') {
            if (mti.startsWith("01")) return AUTHORIZATION_RESPONSE;
            if (mti.startsWith("02")) return FINANCIAL_RESPONSE;
            if (mti.startsWith("04")) return REVERSAL_ADVICE_RESPONSE;
            if (mti.startsWith("08")) return NETWORK_MANAGEMENT_RESPONSE;
        }
        if (mti != null && mti.length() == 4 && mti.charAt(2) == '0') {
            if (mti.startsWith("01")) return AUTHORIZATION_REQUEST;
            if (mti.startsWith("02")) return FINANCIAL_REQUEST;
            if (mti.startsWith("04")) return REVERSAL_ADVICE_REQUEST;
            if (mti.startsWith("08")) return NETWORK_MANAGEMENT_REQUEST;
        }
        return AUTHORIZATION_REQUEST;
    }

    public boolean isRequest()  { return mti.charAt(2) == '0'; }
    public boolean isResponse() { return mti.charAt(2) == '1'; }

    public MessageType toResponse() {
        if (!isRequest()) throw new IllegalStateException("Already a response: " + this);
        return fromMti(mti.substring(0, 2) + "1" + mti.substring(3));
    }

    public MessageType toResponseSafe() {
        if (!isRequest()) return this;
        return fromMtiSafe(mti.substring(0, 2) + "1" + mti.substring(3));
    }
}
