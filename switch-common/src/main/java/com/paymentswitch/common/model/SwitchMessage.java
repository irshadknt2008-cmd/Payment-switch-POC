package com.paymentswitch.common.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Canonical internal representation of a payment message flowing through the switch.
 * Decoupled from the ISO 8583 wire format – all handler layers operate on this object.
 */
public class SwitchMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String messageId;
    private final Instant createdAt;
    private MessageType messageType;
    private MessageDirection direction;
    private ProcessingCode processingCode;
    private String pan;                     // Primary Account Number (mask in logs)
    private long transactionAmount;         // Minor currency units (e.g. cents)
    private String currencyCode;            // ISO 4217
    private String retrievalReferenceNumber;
    private String systemTraceAuditNumber;
    private String acquirerId;
    private String issuerId;
    private ResponseCode responseCode;
    private Map<Integer, String> rawFields = new HashMap<>(); // DE number -> value
    private byte[] rawIsoBody;            // original ISO bytes for transparent pass-through

    public SwitchMessage() {
        this.messageId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getMessageId()             { return messageId; }
    public Instant getCreatedAt()            { return createdAt; }

    public MessageType getMessageType()      { return messageType; }
    public void setMessageType(MessageType t){ this.messageType = t; }

    public MessageDirection getDirection()          { return direction; }
    public void setDirection(MessageDirection d)    { this.direction = d; }

    public ProcessingCode getProcessingCode()          { return processingCode; }
    public void setProcessingCode(ProcessingCode pc)   { this.processingCode = pc; }

    public String getPan()           { return pan; }
    public void setPan(String pan)   { this.pan = pan; }

    public long getTransactionAmount()              { return transactionAmount; }
    public void setTransactionAmount(long amount)   { this.transactionAmount = amount; }

    public String getCurrencyCode()                 { return currencyCode; }
    public void setCurrencyCode(String currencyCode){ this.currencyCode = currencyCode; }

    public String getRetrievalReferenceNumber()        { return retrievalReferenceNumber; }
    public void setRetrievalReferenceNumber(String rrn){ this.retrievalReferenceNumber = rrn; }

    public String getSystemTraceAuditNumber()           { return systemTraceAuditNumber; }
    public void setSystemTraceAuditNumber(String stan)  { this.systemTraceAuditNumber = stan; }

    public String getAcquirerId()                   { return acquirerId; }
    public void setAcquirerId(String acquirerId)    { this.acquirerId = acquirerId; }

    public String getIssuerId()                 { return issuerId; }
    public void setIssuerId(String issuerId)    { this.issuerId = issuerId; }

    public ResponseCode getResponseCode()               { return responseCode; }
    public void setResponseCode(ResponseCode rc)        { this.responseCode = rc; }

    public Map<Integer, String> getRawFields()              { return rawFields; }
    public void setRawFields(Map<Integer, String> fields)   { this.rawFields = fields; }
    public void setField(int de, String value)              { this.rawFields.put(de, value); }
    public String getField(int de)                          { return this.rawFields.get(de); }

    public byte[] getRawIsoBody()                { return rawIsoBody; }
    public void setRawIsoBody(byte[] rawIsoBody) { this.rawIsoBody = rawIsoBody; }

    /** Returns masked PAN for safe logging (first 6 + last 4). */
    public String getMaskedPan() {
        if (pan == null || pan.length() < 10) return "****";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }

    @Override
    public String toString() {
        return "SwitchMessage{id=" + messageId
                + ", type=" + messageType
                + ", direction=" + direction
                + ", pan=" + getMaskedPan()
                + ", amount=" + transactionAmount
                + ", currency=" + currencyCode
                + ", rrn=" + retrievalReferenceNumber + "}";
    }
}
