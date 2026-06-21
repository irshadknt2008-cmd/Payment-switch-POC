package com.paymentswitch.common.model;

import java.io.Serializable;
import java.util.Collections;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Canonical internal representation of a payment message flowing through the switch.
 * Decoupled from the ISO 8583 wire format – all handler layers operate on this object.
 */
public class SwitchMessage implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Map<Integer, String> SIM_FIELD_LABELS = buildSimFieldLabels();

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

    /**
     * Verbose ISO-style dump for simulator and response logging.
     */
    public String toSimString() {
        Set<Integer> fieldNumbers = collectDisplayFieldNumbers();
        StringBuilder sb = new StringBuilder();

        appendSimLine(sb, "MessageType: " + (messageType != null ? messageType.getMti() : "null"));
        appendSimLine(sb, "Bitmap: " + buildBitmapHex(fieldNumbers) + " " + formatFieldList(fieldNumbers));

        for (Integer de : fieldNumbers) {
            String value = resolveDisplayValue(de);
            if (value == null) {
                continue;
            }
            appendSimLine(sb, formatFieldLine(de, value));
        }

        return sb.toString().trim();
    }

    private Set<Integer> collectDisplayFieldNumbers() {
        Set<Integer> fieldNumbers = new TreeSet<>(rawFields.keySet());
        if (pan != null) fieldNumbers.add(2);
        if (processingCode != null) fieldNumbers.add(3);
        if (rawFields.containsKey(4) || transactionAmount > 0) fieldNumbers.add(4);
        if (rawFields.containsKey(11) || systemTraceAuditNumber != null) fieldNumbers.add(11);
        if (rawFields.containsKey(37) || retrievalReferenceNumber != null) fieldNumbers.add(37);
        if (rawFields.containsKey(39) || responseCode != null) fieldNumbers.add(39);
        if (rawFields.containsKey(49) || currencyCode != null) fieldNumbers.add(49);
        return fieldNumbers;
    }

    private String resolveDisplayValue(int de) {
        switch (de) {
            case 2:
                return pan;
            case 3:
                return processingCode != null ? processingCode.getCode() : getField(3);
            case 4:
                if (getField(4) != null) {
                    return getField(4);
                }
                return transactionAmount > 0 ? String.format("%012d", transactionAmount) : null;
            case 11:
                return systemTraceAuditNumber != null ? systemTraceAuditNumber : getField(11);
            case 37:
                return retrievalReferenceNumber != null ? retrievalReferenceNumber : getField(37);
            case 39:
                return responseCode != null ? responseCode.getCode() : getField(39);
            case 49:
                return currencyCode != null ? currencyCode : getField(49);
            default:
                return getField(de);
        }
    }

    private String formatFieldLine(int de, String value) {
        return String.format("F%02d_%s: %s", de, labelForField(de), value);
    }

    private static String labelForField(int de) {
        String label = SIM_FIELD_LABELS.get(de);
        return label != null ? label : "Field" + de;
    }

    private static String buildBitmapHex(Set<Integer> fieldNumbers) {
        int maxField = fieldNumbers.isEmpty() ? 0 : Collections.max(fieldNumbers);
        int bitmapLength = maxField > 64 ? 16 : 8;
        byte[] bitmap = new byte[bitmapLength];

        if (bitmapLength == 16) {
            bitmap[0] |= (byte) 0x80;
        }

        for (int field : fieldNumbers) {
            if (field < 1 || field > 128) {
                continue;
            }
            int index = field - 1;
            int byteIndex = index / 8;
            int bitIndex = 7 - (index % 8);
            bitmap[byteIndex] |= (byte) (1 << bitIndex);
        }

        StringBuilder hex = new StringBuilder(bitmap.length * 2);
        for (byte b : bitmap) {
            hex.append(String.format("%02X", b & 0xFF));
        }
        return hex.toString();
    }

    private static String formatFieldList(Set<Integer> fieldNumbers) {
        StringBuilder out = new StringBuilder("[");
        boolean first = true;
        for (Integer field : fieldNumbers) {
            if (!first) {
                out.append(',');
            }
            out.append(field);
            first = false;
        }
        out.append(']');
        return out.toString();
    }

    private static void appendSimLine(StringBuilder sb, String content) {
        sb.append("Sim:").append("                   ").append(content).append('\n');
    }

    private static Map<Integer, String> buildSimFieldLabels() {
        Map<Integer, String> labels = new HashMap<>();
        labels.put(2, "PAN");
        labels.put(3, "ProcessingCode");
        labels.put(4, "AmountTransaction");
        labels.put(7, "TransmissionDateTime");
        labels.put(11, "SystemTraceAuditNumber");
        labels.put(12, "DateTimeLocalTransaction");
        labels.put(13, "DateLocalTxn");
        labels.put(14, "DateExpiration");
        labels.put(18, "MerchantType");
        labels.put(22, "POSDataCode");
        labels.put(23, "CardSequenceNumber");
        labels.put(26, "CardAcceptorBusinessCode");
        labels.put(32, "AcquiringInstitutionIdenti");
        labels.put(35, "Track2Data");
        labels.put(37, "RetrievalReferenceNumber");
        labels.put(38, "ApprovalCode");
        labels.put(39, "ActionCode");
        labels.put(41, "CardAcceptorTerminalIdenti");
        labels.put(42, "CardAcceptorIdentification");
        labels.put(49, "CurrencyCodeTransaction");
        labels.put(51, "CurrencyCodeCardholderBill");
        return labels;
    }
}
