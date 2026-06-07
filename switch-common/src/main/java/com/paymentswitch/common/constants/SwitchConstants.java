package com.paymentswitch.common.constants;

/** Switch-wide constants. */
public final class SwitchConstants {
    private SwitchConstants() {}

    // Netty channel attributes
    public static final String CHANNEL_ATTR_SESSION_ID   = "sessionId";
    public static final String CHANNEL_ATTR_ACQUIRER_ID  = "acquirerId";
    public static final String CHANNEL_ATTR_CONNECTED_AT = "connectedAt";

    // Timeouts (ms)
    public static final int DEFAULT_CONNECT_TIMEOUT_MS  = 5_000;
    public static final int DEFAULT_RESPONSE_TIMEOUT_MS = 30_000;
    public static final int DEFAULT_READ_TIMEOUT_SEC    = 60;
    public static final int DEFAULT_WRITE_TIMEOUT_SEC   = 30;
    /** Neapay acquirer keepalive connections can be idle for minutes between tests. */
    public static final int ACQUIRER_READ_IDLE_SEC      = 300;

    // ISO 8583
    public static final int ISO_LENGTH_HEADER_BYTES = 2;
    public static final int MAX_ISO_MESSAGE_BYTES   = 4096;

    // Network management function codes
    public static final String SIGN_ON_CODE    = "001";
    public static final String SIGN_OFF_CODE   = "002";
    public static final String ECHO_TEST_CODE  = "301";
}
