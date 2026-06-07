package com.paymentswitch.common.exception;

public class IssuerConnectionException extends SwitchException {
    private static final long serialVersionUID = 1L;
    public IssuerConnectionException(String message) { super("ISSUER_CONN_ERROR", message); }
    public IssuerConnectionException(String message, Throwable cause) { super("ISSUER_CONN_ERROR", message, cause); }
}
