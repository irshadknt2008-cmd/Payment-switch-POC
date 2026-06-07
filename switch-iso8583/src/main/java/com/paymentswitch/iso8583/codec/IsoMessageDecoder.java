package com.paymentswitch.iso8583.codec;

import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.iso8583.IsoMessageParser;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Converts a raw {@link ByteBuf} (post-frame-decode) into a {@link SwitchMessage}.
 * Uses lenient parsing – raw bytes are always preserved for passthrough forwarding.
 */
public class IsoMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(IsoMessageDecoder.class);

    private final IsoMessageParser parser;

    public IsoMessageDecoder(IsoMessageParser parser) {
        this.parser = parser;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);

        SwitchMessage msg = parser.parseLenient(bytes);
        log.info("Decoded ISO message from {}: type={} stan={} passthrough={}",
                ctx.channel().remoteAddress(),
                msg.getMessageType(),
                msg.getSystemTraceAuditNumber(),
                msg.getRawIsoBody() != null);
        out.add(msg);
    }
}
