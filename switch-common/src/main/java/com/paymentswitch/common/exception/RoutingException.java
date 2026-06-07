package com.paymentswitch.common.exception;

public class RoutingException extends SwitchException {
    private static final long serialVersionUID = 1L;
    public RoutingException(String message) { super("ROUTING_ERROR", message); }
    public RoutingException(String message, Throwable cause) { super("ROUTING_ERROR", message, cause); }
}
