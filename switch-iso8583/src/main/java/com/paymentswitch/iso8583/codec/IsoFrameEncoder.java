package com.paymentswitch.iso8583.codec;

import com.paymentswitch.common.constants.SwitchConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty encoder that prepends a 2-byte big-endian length header to each
 * outgoing ISO 8583 message byte array.
 *
 * <p>Pipeline position: first outbound handler.</p>
 */
public class IsoFrameEncoder extends MessageToByteEncoder<byte[]> {

    private static final Logger log = LoggerFactory.getLogger(IsoFrameEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) {
        out.writeShort(msg.length);
        out.writeBytes(msg);
        log.trace("Encoded {} bytes to {}", msg.length, ctx.channel().remoteAddress());
    }
}
