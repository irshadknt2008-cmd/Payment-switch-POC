package com.paymentswitch.iso8583;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Shared ISO 8583 codec. Configured for Neapay: ASCII hex bitmap, 2-byte TCP length.
 */
public final class Iso8583Codec {

    private static final Logger log = LoggerFactory.getLogger(Iso8583Codec.class);

    private final MessageFactory<IsoMessage> messageFactory;
    private final IsoMessageParser parser;
    private final IsoMessageAssembler assembler;

    public Iso8583Codec() {
        this.messageFactory = buildFactory();
        this.parser    = new IsoMessageParser(messageFactory);
        this.assembler = new IsoMessageAssembler(messageFactory);
    }

    public IsoMessageParser getParser()       { return parser; }
    public IsoMessageAssembler getAssembler() { return assembler; }

    private static MessageFactory<IsoMessage> buildFactory() {
        MessageFactory<IsoMessage> factory = new MessageFactory<>();
        factory.setCharacterEncoding("UTF-8");
        factory.setUseBinaryBitmap(false);
        factory.setIgnoreLastMissingField(true);
        factory.setForceStringEncoding(false);

        try {
            factory.setConfigPath("iso8583-config.xml");
        } catch (IOException e) {
            log.warn("Could not load iso8583-config.xml: {}", e.getMessage());
        }

        // Universal DE 2–128 guide – permanent fix for any Neapay bitmap combination
        Iso8583ParseGuideBuilder.applyTo(factory);
        log.info("ISO 8583 codec ready (ASCII hex bitmap, universal parse guide DE 2–128)");
        return factory;
    }
}
