package com.paymentswitch.issuer;

/**
 * Configuration for a single issuer TCP endpoint.
 */
public class IssuerEndpoint {

    private final String issuerId;
    private final String host;
    private final int port;
    private final int connectionPoolSize;

    public IssuerEndpoint(String issuerId, String host, int port, int connectionPoolSize) {
        this.issuerId           = issuerId;
        this.host               = host;
        this.port               = port;
        this.connectionPoolSize = connectionPoolSize;
    }

    public String getIssuerId()         { return issuerId; }
    public String getHost()             { return host; }
    public int getPort()                { return port; }
    public int getConnectionPoolSize()  { return connectionPoolSize; }

    @Override
    public String toString() {
        return "IssuerEndpoint{" + issuerId + " @ " + host + ":" + port + "}";
    }
}
