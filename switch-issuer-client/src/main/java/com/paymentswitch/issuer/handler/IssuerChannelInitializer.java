package com.paymentswitch.issuer.handler;

import com.paymentswitch.common.constants.SwitchConstants;
import com.paymentswitch.iso8583.IsoMessageAssembler;
import com.paymentswitch.iso8583.IsoMessageParser;
import com.paymentswitch.iso8583.codec.IsoFrameDecoder;
import com.paymentswitch.iso8583.codec.IsoMessageDecoder;
import com.paymentswitch.iso8583.codec.IsoMessageEncoder;
import com.paymentswitch.issuer.IssuerConnection;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

public class IssuerChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final IsoMessageParser parser;
    private final IsoMessageAssembler assembler;
    private final IssuerConnection connection;

    public IssuerChannelInitializer(IsoMessageParser parser,
                                    IsoMessageAssembler assembler,
                                    IssuerConnection connection) {
        this.parser     = parser;
        this.assembler  = assembler;
        this.connection = connection;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline p = ch.pipeline();

        p.addLast("isoMessageEncoder", new IsoMessageEncoder(assembler));
        p.addLast("idleStateHandler",  new IdleStateHandler(
                SwitchConstants.ACQUIRER_READ_IDLE_SEC, 0, 0, TimeUnit.SECONDS));
        p.addLast("isoFrameDecoder",   new IsoFrameDecoder());
        p.addLast("isoMessageDecoder", new IsoMessageDecoder(parser));
        p.addLast("issuerResponseHandler", new IssuerResponseHandler(connection));
    }
}
