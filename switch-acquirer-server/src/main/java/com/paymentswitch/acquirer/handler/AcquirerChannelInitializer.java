package com.paymentswitch.acquirer.handler;

import com.paymentswitch.common.constants.SwitchConstants;
import com.paymentswitch.iso8583.IsoMessageAssembler;
import com.paymentswitch.iso8583.IsoMessageParser;
import com.paymentswitch.iso8583.codec.IsoFrameDecoder;
import com.paymentswitch.iso8583.codec.IsoMessageDecoder;
import com.paymentswitch.iso8583.codec.IsoMessageEncoder;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * Initialises the Netty pipeline for each incoming acquirer connection.
 *
 * <p>Pipeline (inbound left-to-right, outbound right-to-left):
 * <pre>
 *   [wire] ──► IsoFrameDecoder ──► IsoMessageDecoder ──► IdleStateHandler
 *                                                          ──► AcquirerMessageHandler
 *           ◄── IsoMessageEncoder ◄── (prepends 2-byte length)
 * </pre>
 */
public class AcquirerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final IsoMessageParser parser;
    private final IsoMessageAssembler assembler;
    private final AcquirerMessageHandler messageHandler;

    public AcquirerChannelInitializer(IsoMessageParser parser,
                                      IsoMessageAssembler assembler,
                                      AcquirerMessageHandler messageHandler) {
        this.parser         = parser;
        this.assembler      = assembler;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        p.addLast("isoMessageEncoder", new IsoMessageEncoder(assembler));

        // Inbound – idle handler first so any received bytes reset the timer.
        // Neapay opens a persistent keepalive TCP connection; writer-idle must be
        // disabled (0) because the switch has nothing to send until a request arrives.
        p.addLast("idleStateHandler",  new IdleStateHandler(
                SwitchConstants.ACQUIRER_READ_IDLE_SEC, 0, 0, TimeUnit.SECONDS));
        p.addLast("isoFrameDecoder",   new IsoFrameDecoder());
        p.addLast("isoMessageDecoder", new IsoMessageDecoder(parser));
        p.addLast("acquirerHandler",   messageHandler);
    }
}
