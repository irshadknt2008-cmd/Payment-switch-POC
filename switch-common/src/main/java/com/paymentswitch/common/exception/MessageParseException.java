package com.paymentswitch.common.exception;

public class MessageParseException extends SwitchException {
    private static final long serialVersionUID = 1L;
    public MessageParseException(String message) { super("MSG_PARSE_ERROR", message); }
    public MessageParseException(String message, Throwable cause) { super("MSG_PARSE_ERROR", message, cause); }
}
