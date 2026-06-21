package com.paymentswitch.iso8583;

import com.paymentswitch.common.exception.MessageParseException;
import com.paymentswitch.common.model.MessageType;
import com.paymentswitch.common.model.ProcessingCode;
import com.paymentswitch.common.model.ResponseCode;
import com.paymentswitch.common.model.SwitchMessage;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Parses ISO 8583 into {@link SwitchMessage}. Always preserves raw bytes for passthrough.
 * Never fails the pipeline – routing metadata is extracted best-effort.
 */
public class IsoMessageParser {

    private static final Logger log = LoggerFactory.getLogger(IsoMessageParser.class);
    private final MessageFactory<IsoMessage> asciiMessageFactory;
    private final MessageFactory<IsoMessage> binaryMessageFactory;

    public IsoMessageParser(MessageFactory<IsoMessage> asciiMessageFactory, MessageFactory<IsoMessage> binaryMessageFactory) {
        this.asciiMessageFactory = asciiMessageFactory;
        this.binaryMessageFactory = binaryMessageFactory;
    }

    /**
     * Parse without throwing. Raw bytes are always attached for transparent forwarding.
     */
    public SwitchMessage parseLenient(byte[] rawBytes) {
        SwitchMessage best = parseWithFallback(rawBytes);
        if (isBlank(best.getSystemTraceAuditNumber())) {
            IsoRoutingExtractor.extract(rawBytes, best);
        }
        return best;
    }

    public SwitchMessage parse(byte[] rawBytes) {
        try {
            IsoMessage isoMsg = asciiMessageFactory.parseMessage(rawBytes, 0);
            SwitchMessage msg = new SwitchMessage();
            msg.setRawIsoBody(rawBytes);
            enrichFromIso(msg, isoMsg);
            return msg;
        } catch (IOException | ParseException e) {
            throw new MessageParseException("Failed to parse ISO 8583 message", e);
        }
    }

    private SwitchMessage parseWithFallback(byte[] rawBytes) {
        SwitchMessage ascii = parseWithFactory(rawBytes, asciiMessageFactory, "ASCII");
        SwitchMessage binary = parseWithFactory(rawBytes, binaryMessageFactory, "binary");

        if (binary != null && score(binary) > score(ascii)) {
            if (ascii != null) {
                log.debug("Using binary bitmap parse over ASCII parse (stan={}, panPresent={})",
                        binary.getSystemTraceAuditNumber(),
                        !isBlank(binary.getPan()));
            }
            return binary;
        }

        if (ascii != null) {
            return ascii;
        }

        SwitchMessage routed = new SwitchMessage();
        routed.setRawIsoBody(rawBytes);
        if (rawBytes.length >= 4) {
            String mti = new String(rawBytes, 0, 4, StandardCharsets.US_ASCII);
            routed.setMessageType(MessageType.fromMtiSafe(mti));
        }
        IsoRoutingExtractor.extract(rawBytes, routed);
        return routed;
    }

    private SwitchMessage parseWithFactory(byte[] rawBytes, MessageFactory<IsoMessage> factory, String label) {
        try {
            IsoMessage isoMsg = factory.parseMessage(rawBytes, 0);
            SwitchMessage msg = new SwitchMessage();
            msg.setRawIsoBody(rawBytes);
            enrichFromIso(msg, isoMsg);
            return msg;
        } catch (IOException | ParseException e) {
            log.debug("Metadata parse incomplete ({} bytes) using {} factory: {}",
                    rawBytes.length, label, e.getMessage());
            return null;
        }
    }

    private int score(SwitchMessage msg) {
        if (msg == null) {
            return Integer.MIN_VALUE;
        }

        int score = 0;
        if (msg.getMessageType() != null) score += 10;
        if (!isBlank(msg.getSystemTraceAuditNumber())) score += 100;
        if (!isBlank(msg.getPan())) score += 50;
        if (!isBlank(msg.getRetrievalReferenceNumber())) score += 20;
        if (msg.getResponseCode() != null) score += 10;
        if (msg.getTransactionAmount() > 0) score += 5;
        return score;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void enrichFromIso(SwitchMessage msg, IsoMessage iso) {
        msg.setMessageType(MessageType.fromMtiSafe(String.format("%04X", iso.getType())));

        if (iso.hasField(2))  msg.setPan(fieldValueAsString(iso, 2));

        if (iso.hasField(3)) {
            String code = fieldValueAsString(iso, 3);
            try {
                msg.setProcessingCode(ProcessingCode.fromCode(code));
            } catch (IllegalArgumentException ignored) {
                msg.setField(3, code);
            }
        }

        if (iso.hasField(4)) {
            String amount = fieldValueAsString(iso, 4);
            try {
                msg.setTransactionAmount(Long.parseLong(amount.trim()));
            } catch (NumberFormatException ignored) {
                msg.setField(4, amount);
            }
        }

        if (iso.hasField(11)) msg.setSystemTraceAuditNumber(fieldValueAsString(iso, 11));
        if (iso.hasField(37)) msg.setRetrievalReferenceNumber(fieldValueAsString(iso, 37));
        if (iso.hasField(49)) msg.setCurrencyCode(fieldValueAsString(iso, 49));
        if (iso.hasField(39)) msg.setResponseCode(ResponseCode.fromCode(fieldValueAsString(iso, 39)));

        for (int i = 1; i <= 128; i++) {
            if (iso.hasField(i)) {
                msg.setField(i, fieldValueAsString(iso, i));
            }
        }
    }

    private String fieldValueAsString(IsoMessage iso, int field) {
        Object value = iso.getField(field).getValue();
        if (value instanceof Date) {
            return formatDateField(field, (Date) value);
        }
        return value != null ? value.toString() : String.valueOf(iso.getObjectValue(field));
    }

    private String formatDateField(int field, Date value) {
        String pattern;
        switch (field) {
            case 7:
                pattern = "MMddHHmmss";
                break;
            case 12:
                pattern = "HHmmss";
                break;
            case 13:
            case 15:
            case 17:
                pattern = "MMdd";
                break;
            case 14:
                pattern = "yyMM";
                break;
            default:
                pattern = "MMddHHmmss";
                break;
        }
        return new SimpleDateFormat(pattern).format(value);
    }
}
