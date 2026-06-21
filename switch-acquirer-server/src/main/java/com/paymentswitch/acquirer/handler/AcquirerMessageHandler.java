package com.paymentswitch.acquirer.handler;

import com.paymentswitch.common.exception.IssuerConnectionException;
import com.paymentswitch.common.exception.RoutingException;
import com.paymentswitch.common.model.MessageDirection;
import com.paymentswitch.common.model.MessageType;
import com.paymentswitch.common.model.ResponseCode;
import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.issuer.IssuerClientPool;
import com.paymentswitch.routing.RoutingService;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transparent ISO 8583 switch handler: acquirer → route → issuer → acquirer.
 */
@ChannelHandler.Sharable
public class AcquirerMessageHandler extends SimpleChannelInboundHandler<SwitchMessage> {

    private static final Logger log = LoggerFactory.getLogger(AcquirerMessageHandler.class);

    private final RoutingService routingService;
    private final IssuerClientPool issuerClientPool;
    private final String defaultIssuerId;

    public AcquirerMessageHandler(RoutingService routingService,
                                  IssuerClientPool issuerClientPool,
                                  String defaultIssuerId) {
        this.routingService    = routingService;
        this.issuerClientPool  = issuerClientPool;
        this.defaultIssuerId   = defaultIssuerId;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SwitchMessage msg) {
        msg.setDirection(MessageDirection.INBOUND);
        log.info("Received from acquirer {}: type={} stan={} pan={}",
                ctx.channel().remoteAddress(),
                msg.getMessageType(),
                msg.getSystemTraceAuditNumber(),
                msg.getMaskedPan());

        try {
            if (msg.getMessageType() == MessageType.NETWORK_MANAGEMENT_REQUEST) {
                writeResponse(ctx, buildNetworkManagementResponse(msg));
                return;
            }

            routeMessage(msg);

            issuerClientPool.send(msg)
                    .thenAccept(response -> writeResponse(ctx, response))
                    .exceptionally(ex -> {
                        Throwable cause = unwrap(ex);
                        log.error("Issuer forwarding failed for stan={}: {}",
                                msg.getSystemTraceAuditNumber(), cause.getMessage());
                        writeResponse(ctx, buildErrorResponse(msg, cause));
                        return null;
                    });

        } catch (Exception e) {
            log.error("Error processing stan={}: {}", msg.getSystemTraceAuditNumber(), e.getMessage(), e);
            writeResponse(ctx, buildErrorResponse(msg, e));
        }
    }

    private void routeMessage(SwitchMessage msg) {
        String pan = msg.getPan();
        if (pan != null && pan.length() >= 6) {
            routingService.route(msg);
        } else {
            msg.setIssuerId(defaultIssuerId);
            log.debug("No PAN – routing stan={} to default issuer {}",
                    msg.getSystemTraceAuditNumber(), defaultIssuerId);
        }
    }

    private void writeResponse(ChannelHandlerContext ctx, SwitchMessage response) {
        response.setDirection(MessageDirection.OUTBOUND);
        ctx.writeAndFlush(response);
        log.info("Sent to acquirer {}:\n{}", ctx.channel().remoteAddress(), response.toSimString());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("Acquirer connected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("Acquirer disconnected: {}", ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idle = (IdleStateEvent) evt;
            if (idle.state() == IdleState.READER_IDLE) {
                log.warn("Reader idle on {}, closing", ctx.channel().remoteAddress());
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Pipeline error on {}: {}", ctx.channel().remoteAddress(), cause.getMessage(), cause);
    }

    private SwitchMessage buildErrorResponse(SwitchMessage req, Throwable cause) {
        SwitchMessage resp = new SwitchMessage();
        resp.setMessageType(req.getMessageType() != null
                ? req.getMessageType().toResponseSafe() : null);
        resp.setSystemTraceAuditNumber(req.getSystemTraceAuditNumber());
        resp.setDirection(MessageDirection.OUTBOUND);

        ResponseCode rc = ResponseCode.SYSTEM_ERROR;
        if (cause instanceof IssuerConnectionException || cause instanceof RoutingException) {
            rc = ResponseCode.ISSUER_UNAVAILABLE;
        }
        resp.setResponseCode(rc);

        copyEchoFields(req, resp);
        return resp;
    }

    private SwitchMessage buildNetworkManagementResponse(SwitchMessage req) {
        SwitchMessage resp = new SwitchMessage();
        resp.setMessageType(MessageType.NETWORK_MANAGEMENT_RESPONSE);
        resp.setDirection(MessageDirection.OUTBOUND);
        resp.setResponseCode(ResponseCode.APPROVED);
        resp.setSystemTraceAuditNumber(
                req.getSystemTraceAuditNumber() != null ? req.getSystemTraceAuditNumber() : req.getField(11));
        copyEchoFields(req, resp);
        echoCanonicalField(req, resp, 7);
        echoCanonicalField(req, resp, 11);
        echoCanonicalField(req, resp, 12);
        echoCanonicalField(req, resp, 13);
        echoCanonicalField(req, resp, 41);
        return resp;
    }

    private void copyEchoFields(SwitchMessage req, SwitchMessage resp) {
        for (int de : new int[]{2, 3, 4, 7, 11, 12, 13, 37, 41, 49}) {
            echoCanonicalField(req, resp, de);
        }
    }

    private void echoCanonicalField(SwitchMessage req, SwitchMessage resp, int de) {
        String value = req.getField(de);
        if (value == null) {
            switch (de) {
                case 2:
                    value = req.getPan();
                    break;
                case 3:
                    value = req.getProcessingCode() != null ? req.getProcessingCode().getCode() : null;
                    break;
                case 4:
                    value = req.getTransactionAmount() > 0
                            ? String.format("%012d", req.getTransactionAmount())
                            : null;
                    break;
                case 11:
                    value = req.getSystemTraceAuditNumber();
                    break;
                case 37:
                    value = req.getRetrievalReferenceNumber();
                    break;
                case 39:
                    value = req.getResponseCode() != null ? req.getResponseCode().getCode() : null;
                    break;
                case 49:
                    value = req.getCurrencyCode();
                    break;
                default:
                    break;
            }
        }

        if (value != null) {
            resp.setField(de, value);
        }
    }

    private static Throwable unwrap(Throwable ex) {
        if (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }
}
