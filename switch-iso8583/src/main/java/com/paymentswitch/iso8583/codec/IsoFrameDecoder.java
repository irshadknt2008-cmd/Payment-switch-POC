package com.paymentswitch.iso8583.codec;

import com.paymentswitch.common.constants.SwitchConstants;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteOrder;

/**
 * Netty frame decoder for ISO 8583 messages that are prefixed with a 2-byte
 * big-endian binary length header (common in banking networks).
 *
 * <p>Pipeline position: first inbound handler.</p>
 *
 * <pre>
 * Wire format:
 *   [2-byte length][ISO 8583 message bytes...]
 * </pre>
 */
public class IsoFrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger log = LoggerFactory.getLogger(IsoFrameDecoder.class);

    public IsoFrameDecoder() {
        super(
            SwitchConstants.MAX_ISO_MESSAGE_BYTES,        // maxFrameLength
            0,                                             // lengthFieldOffset
            SwitchConstants.ISO_LENGTH_HEADER_BYTES,      // lengthFieldLength
            0,                                             // lengthAdjustment
            SwitchConstants.ISO_LENGTH_HEADER_BYTES        // initialBytesToStrip
        );
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object frame = super.decode(ctx, in);
        if (frame != null) {
            log.info("ISO frame received: {} bytes from {}",
                    ((ByteBuf) frame).readableBytes(), ctx.channel().remoteAddress());
        }
        return frame;
    }
}
