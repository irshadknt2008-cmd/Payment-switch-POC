package com.paymentswitch.common.exception;

/** Base unchecked exception for the payment switch. */
public class SwitchException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final String errorCode;

    public SwitchException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    public SwitchException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    public String getErrorCode() { return errorCode; }
}
