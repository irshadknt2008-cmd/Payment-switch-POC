package com.paymentswitch.routing;

/**
 * Represents a single row in the BIN (Bank Identification Number) routing table.
 * Maps a BIN range to an issuer endpoint.
 */
public class BinTableEntry {

    private final String binRangeStart;   // 6-digit BIN range start (inclusive)
    private final String binRangeEnd;     // 6-digit BIN range end (inclusive)
    private final String issuerId;
    private final String issuerName;
    private final boolean active;

    public BinTableEntry(String binRangeStart, String binRangeEnd,
                         String issuerId, String issuerName, boolean active) {
        this.binRangeStart = binRangeStart;
        this.binRangeEnd   = binRangeEnd;
        this.issuerId      = issuerId;
        this.issuerName    = issuerName;
        this.active        = active;
    }

    public boolean matches(String bin) {
        if (bin == null || bin.length() < 6) return false;
        String sixDigit = bin.substring(0, 6);
        return sixDigit.compareTo(binRangeStart) >= 0
            && sixDigit.compareTo(binRangeEnd) <= 0;
    }

    public String getBinRangeStart() { return binRangeStart; }
    public String getBinRangeEnd()   { return binRangeEnd; }
    public String getIssuerId()      { return issuerId; }
    public String getIssuerName()    { return issuerName; }
    public boolean isActive()        { return active; }

    @Override
    public String toString() {
        return "BinTableEntry{" + binRangeStart + "-" + binRangeEnd
                + " -> " + issuerId + " (" + issuerName + ")}";
    }
}
