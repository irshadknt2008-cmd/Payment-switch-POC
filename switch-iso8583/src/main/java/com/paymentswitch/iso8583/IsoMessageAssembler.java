package com.paymentswitch.iso8583;

import com.paymentswitch.common.model.SwitchMessage;
import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.TreeMap;

/**
 * Assembles ISO 8583 byte arrays from {@link SwitchMessage} domain objects.
 * Prefers the original raw ISO body for transparent pass-through.
 */
public class IsoMessageAssembler {

    private static final Logger log = LoggerFactory.getLogger(IsoMessageAssembler.class);

    /** Neapay ISO8583-93 field types keyed by DE number (from iso8583-config.xml). */
    private static final Map<Integer, FieldSpec> FIELD_SPECS = buildFieldSpecs();

    private final MessageFactory<IsoMessage> messageFactory;

    public IsoMessageAssembler(MessageFactory<IsoMessage> messageFactory) {
        this.messageFactory = messageFactory;
    }

    public byte[] assemble(SwitchMessage msg) {
        if (msg.getRawIsoBody() != null) {
            return msg.getRawIsoBody();
        }
        return assembleFromFields(msg);
    }

    private byte[] assembleFromFields(SwitchMessage msg) {
        int mti = Integer.parseInt(msg.getMessageType().getMti(), 16);
        IsoMessage iso = messageFactory.newMessage(mti);

        Map<Integer, String> fields = new TreeMap<>(msg.getRawFields());

        // Ensure canonical fields are present when raw map is sparse
        if (msg.getPan() != null)                    fields.putIfAbsent(2,  msg.getPan());
        if (msg.getProcessingCode() != null)         fields.putIfAbsent(3,  msg.getProcessingCode().getCode());
        if (msg.getTransactionAmount() > 0)          fields.putIfAbsent(4,  String.format("%012d", msg.getTransactionAmount()));
        if (msg.getSystemTraceAuditNumber() != null) fields.putIfAbsent(11, msg.getSystemTraceAuditNumber());
        if (msg.getRetrievalReferenceNumber() != null) fields.putIfAbsent(37, msg.getRetrievalReferenceNumber());
        if (msg.getCurrencyCode() != null)           fields.putIfAbsent(49, msg.getCurrencyCode());
        if (msg.getResponseCode() != null)           fields.putIfAbsent(39, msg.getResponseCode().getCode());

        for (Map.Entry<Integer, String> entry : fields.entrySet()) {
            int de = entry.getKey();
            String value = entry.getValue();
            if (value == null) continue;

            FieldSpec spec = FIELD_SPECS.get(de);
            if (spec != null) {
                iso.setValue(de, value, spec.type, spec.length);
            } else {
                log.trace("No field spec for DE {}; skipping in assembly", de);
            }
        }

        byte[] bytes = iso.writeData();
        log.debug("Assembled {} bytes for {}", bytes.length, msg);
        return bytes;
    }

    private static Map<Integer, FieldSpec> buildFieldSpecs() {
        Map<Integer, FieldSpec> specs = new TreeMap<>();
        specs.put(2,  new FieldSpec(IsoType.LLVAR,   0));
        specs.put(3,  new FieldSpec(IsoType.NUMERIC, 6));
        specs.put(4,  new FieldSpec(IsoType.NUMERIC, 12));
        specs.put(7,  new FieldSpec(IsoType.DATE10,  0));
        specs.put(11, new FieldSpec(IsoType.NUMERIC, 6));
        specs.put(12, new FieldSpec(IsoType.TIME,    0));
        specs.put(13, new FieldSpec(IsoType.DATE4,   0));
        specs.put(14, new FieldSpec(IsoType.DATE_EXP,0));
        specs.put(18, new FieldSpec(IsoType.NUMERIC, 4));
        specs.put(22, new FieldSpec(IsoType.NUMERIC, 3));
        specs.put(23, new FieldSpec(IsoType.NUMERIC, 3));
        specs.put(25, new FieldSpec(IsoType.NUMERIC, 2));
        specs.put(26, new FieldSpec(IsoType.NUMERIC, 2));
        specs.put(28, new FieldSpec(IsoType.ALPHA,   9));
        specs.put(32, new FieldSpec(IsoType.LLVAR,   0));
        specs.put(33, new FieldSpec(IsoType.LLVAR,   0));
        specs.put(35, new FieldSpec(IsoType.LLVAR,   0));
        specs.put(37, new FieldSpec(IsoType.ALPHA,   12));
        specs.put(38, new FieldSpec(IsoType.ALPHA,   6));
        specs.put(39, new FieldSpec(IsoType.ALPHA,   2));
        specs.put(41, new FieldSpec(IsoType.ALPHA,   8));
        specs.put(42, new FieldSpec(IsoType.ALPHA,   15));
        specs.put(43, new FieldSpec(IsoType.ALPHA,   40));
        specs.put(49, new FieldSpec(IsoType.NUMERIC, 3));
        specs.put(51, new FieldSpec(IsoType.NUMERIC, 3));
        specs.put(52, new FieldSpec(IsoType.ALPHA,   16));
        specs.put(70, new FieldSpec(IsoType.NUMERIC, 3));
        return specs;
    }

    private static final class FieldSpec {
        final IsoType type;
        final int length;
        FieldSpec(IsoType type, int length) { this.type = type; this.length = length; }
    }
}
