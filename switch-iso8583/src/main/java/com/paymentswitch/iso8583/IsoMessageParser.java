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

/**
 * Parses ISO 8583 into {@link SwitchMessage}. Always preserves raw bytes for passthrough.
 * Never fails the pipeline – routing metadata is extracted best-effort.
 */
public class IsoMessageParser {

    private static final Logger log = LoggerFactory.getLogger(IsoMessageParser.class);
    private final MessageFactory<IsoMessage> messageFactory;

    public IsoMessageParser(MessageFactory<IsoMessage> messageFactory) {
        this.messageFactory = messageFactory;
    }

    /**
     * Parse without throwing. Raw bytes are always attached for transparent forwarding.
     */
    public SwitchMessage parseLenient(byte[] rawBytes) {
        SwitchMessage msg = new SwitchMessage();
        msg.setRawIsoBody(rawBytes);

        if (rawBytes.length >= 4) {
            String mti = new String(rawBytes, 0, 4, StandardCharsets.US_ASCII);
            msg.setMessageType(MessageType.fromMtiSafe(mti));
        }

        try {
            IsoMessage isoMsg = messageFactory.parseMessage(rawBytes, 0);
            enrichFromIso(msg, isoMsg);
        } catch (IOException | ParseException e) {
            log.debug("Metadata parse incomplete ({} bytes), using routing extractor: {}",
                    rawBytes.length, e.getMessage());
            IsoRoutingExtractor.extract(rawBytes, msg);
        }

        return msg;
    }

    public SwitchMessage parse(byte[] rawBytes) {
        try {
            IsoMessage isoMsg = messageFactory.parseMessage(rawBytes, 0);
            SwitchMessage msg = new SwitchMessage();
            msg.setRawIsoBody(rawBytes);
            enrichFromIso(msg, isoMsg);
            return msg;
        } catch (IOException | ParseException e) {
            throw new MessageParseException("Failed to parse ISO 8583 message", e);
        }
    }

    private void enrichFromIso(SwitchMessage msg, IsoMessage iso) {
        msg.setMessageType(MessageType.fromMtiSafe(String.format("%04d", iso.getType())));

        if (iso.hasField(2))  msg.setPan(iso.getObjectValue(2));

        if (iso.hasField(3)) {
            String code = iso.getObjectValue(3);
            try {
                msg.setProcessingCode(ProcessingCode.fromCode(code));
            } catch (IllegalArgumentException ignored) {
                msg.setField(3, code);
            }
        }

        if (iso.hasField(4)) {
            String amount = iso.getObjectValue(4);
            try {
                msg.setTransactionAmount(Long.parseLong(amount.trim()));
            } catch (NumberFormatException ignored) {
                msg.setField(4, amount);
            }
        }

        if (iso.hasField(11)) msg.setSystemTraceAuditNumber(iso.getObjectValue(11));
        if (iso.hasField(37)) msg.setRetrievalReferenceNumber(iso.getObjectValue(37));
        if (iso.hasField(49)) msg.setCurrencyCode(iso.getObjectValue(49));
        if (iso.hasField(39)) msg.setResponseCode(ResponseCode.fromCode(iso.getObjectValue(39)));

        for (int i = 1; i <= 128; i++) {
            if (iso.hasField(i)) {
                Object val = iso.getField(i).getValue();
                msg.setField(i, val != null ? val.toString() : iso.getObjectValue(i));
            }
        }
    }
}
