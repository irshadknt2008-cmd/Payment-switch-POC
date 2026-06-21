package com.paymentswitch.issuer.handler;

import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.issuer.IssuerConnection;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Correlates inbound issuer responses to pending acquirer requests via STAN (DE 11).
 */
public class IssuerResponseHandler extends SimpleChannelInboundHandler<SwitchMessage> {

    private static final Logger log = LoggerFactory.getLogger(IssuerResponseHandler.class);

    private final IssuerConnection connection;

    public IssuerResponseHandler(IssuerConnection connection) {
        this.connection = connection;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SwitchMessage response) {
        log.info("Received response from issuer {}:\n{}", ctx.channel().remoteAddress(), response.toSimString());

        String stan = response.getSystemTraceAuditNumber();
        if (stan == null) {
            if (connection.completeSinglePending(response)) {
                log.warn("Issuer response missing STAN (DE 11), correlated via single-pending fallback");
                return;
            }
            log.warn("Issuer response missing STAN (DE 11), cannot correlate");
            return;
        }

        if (connection.completePending(stan, response)) {
            log.debug("Correlated issuer response for STAN {}", stan);
        } else {
            log.warn("No pending request for STAN {}", stan);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("Issuer connection dropped: {}", ctx.channel().remoteAddress());
        connection.failAllPending(new IllegalStateException("Issuer connection lost"));
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idle = (IdleStateEvent) evt;
            if (idle.state() == IdleState.READER_IDLE) {
                log.debug("Issuer reader idle on {} (keepalive, not closing)", ctx.channel().remoteAddress());
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Error on issuer channel {}: {}", ctx.channel().remoteAddress(), cause.getMessage(), cause);
        connection.failAllPending(cause);
        ctx.close();
    }
}
