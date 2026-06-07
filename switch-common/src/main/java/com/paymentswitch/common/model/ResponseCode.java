package com.paymentswitch.common.model;

/** Standard ISO 8583 response codes (DE 39). */
public enum ResponseCode {
    APPROVED("00", "Approved"),
    DO_NOT_HONOUR("05", "Do Not Honour"),
    INVALID_TRANSACTION("12", "Invalid Transaction"),
    INVALID_AMOUNT("13", "Invalid Amount"),
    INVALID_CARD_NUMBER("14", "Invalid Card Number"),
    INSUFFICIENT_FUNDS("51", "Insufficient Funds"),
    EXPIRED_CARD("54", "Expired Card"),
    INCORRECT_PIN("55", "Incorrect PIN"),
    CARD_BLOCKED("62", "Restricted Card"),
    ISSUER_UNAVAILABLE("91", "Issuer Unavailable"),
    DECLINED("99", "Declined"),
    SYSTEM_ERROR("96", "System Malfunction"),
    TIMEOUT("TO", "Timeout");

    private final String code;
    private final String description;

    ResponseCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode()       { return code; }
    public String getDescription(){ return description; }
    public boolean isApproved()   { return "00".equals(code); }

    public static ResponseCode fromCode(String code) {
        for (ResponseCode rc : values()) {
            if (rc.code.equals(code)) return rc;
        }
        return SYSTEM_ERROR;
    }
}
