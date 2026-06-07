package com.paymentswitch.iso8583.codec;

import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.iso8583.IsoMessageAssembler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a {@link SwitchMessage} into raw bytes, then passes to {@link IsoFrameEncoder}.
 */
public class IsoMessageEncoder extends MessageToByteEncoder<SwitchMessage> {

    private static final Logger log = LoggerFactory.getLogger(IsoMessageEncoder.class);

    private final IsoMessageAssembler assembler;

    public IsoMessageEncoder(IsoMessageAssembler assembler) {
        this.assembler = assembler;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, SwitchMessage msg, ByteBuf out) {
        byte[] bytes = assembler.assemble(msg);
        // Prepend 2-byte length header
        out.writeShort(bytes.length);
        out.writeBytes(bytes);
        log.debug("Encoded message: {} ({} bytes)", msg, bytes.length);
    }
}
