package com.paymentswitch.persistence.entity;

import java.time.Instant;

/**
 * Persistence entity for a processed payment transaction.
 * Maps to the {@code transactions} table.
 */
public class TransactionRecord {

    private Long id;
    private String messageId;
    private String mti;
    private String processingCode;
    private String pan;              // Stored masked
    private Long amount;
    private String currencyCode;
    private String stan;
    private String rrn;
    private String acquirerId;
    private String issuerId;
    private String responseCode;
    private String status;           // PENDING | APPROVED | DECLINED | REVERSED | ERROR
    private Instant createdAt;
    private Instant updatedAt;

    // ── Getters & Setters ────────────────────────────────────────────────
    public Long getId()                  { return id; }
    public void setId(Long id)           { this.id = id; }

    public String getMessageId()                 { return messageId; }
    public void setMessageId(String messageId)   { this.messageId = messageId; }

    public String getMti()         { return mti; }
    public void setMti(String mti) { this.mti = mti; }

    public String getProcessingCode()                    { return processingCode; }
    public void setProcessingCode(String processingCode) { this.processingCode = processingCode; }

    public String getPan()         { return pan; }
    public void setPan(String pan) { this.pan = pan; }

    public Long getAmount()            { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }

    public String getCurrencyCode()                  { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }

    public String getStan()            { return stan; }
    public void setStan(String stan)   { this.stan = stan; }

    public String getRrn()         { return rrn; }
    public void setRrn(String rrn) { this.rrn = rrn; }

    public String getAcquirerId()                { return acquirerId; }
    public void setAcquirerId(String acquirerId) { this.acquirerId = acquirerId; }

    public String getIssuerId()              { return issuerId; }
    public void setIssuerId(String issuerId) { this.issuerId = issuerId; }

    public String getResponseCode()                  { return responseCode; }
    public void setResponseCode(String responseCode) { this.responseCode = responseCode; }

    public String getStatus()              { return status; }
    public void setStatus(String status)   { this.status = status; }

    public Instant getCreatedAt()                { return createdAt; }
    public void setCreatedAt(Instant createdAt)  { this.createdAt = createdAt; }

    public Instant getUpdatedAt()                { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt)  { this.updatedAt = updatedAt; }
}
