package com.paymentswitch.issuer;

import com.paymentswitch.common.constants.SwitchConstants;
import com.paymentswitch.common.exception.IssuerConnectionException;
import com.paymentswitch.common.model.ResponseCode;
import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.iso8583.IsoMessageAssembler;
import com.paymentswitch.iso8583.IsoMessageParser;
import com.paymentswitch.issuer.handler.IssuerChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class IssuerConnection {

    private static final Logger log = LoggerFactory.getLogger(IssuerConnection.class);

    private final IssuerEndpoint endpoint;
    private final EventLoopGroup eventLoopGroup;
    private final IssuerChannelInitializer channelInitializer;

    final ConcurrentHashMap<String, CompletableFuture<SwitchMessage>> pendingRequests
            = new ConcurrentHashMap<>();

    private volatile Channel channel;

    public IssuerConnection(IssuerEndpoint endpoint,
                            EventLoopGroup eventLoopGroup,
                            IsoMessageParser parser,
                            IsoMessageAssembler assembler) {
        this.endpoint       = endpoint;
        this.eventLoopGroup = eventLoopGroup;
        this.channelInitializer = new IssuerChannelInitializer(parser, assembler, this);
    }

    public void connect() {
        doConnect();
    }

    /** Reconnect only when the current channel is down. */
    public synchronized void reconnect() {
        if (channel != null && channel.isActive()) {
            return;
        }
        if (channel != null) {
            channel.close();
            channel = null;
        }
        doConnect();
    }

    private void doConnect() {
        Bootstrap bootstrap = new Bootstrap()
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, SwitchConstants.DEFAULT_CONNECT_TIMEOUT_MS)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(channelInitializer);

        try {
            ChannelFuture future = bootstrap.connect(endpoint.getHost(), endpoint.getPort()).sync();
            this.channel = future.channel();
            log.info("Connected to issuer {}", endpoint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IssuerConnectionException("Interrupted while connecting to " + endpoint, e);
        } catch (Exception e) {
            throw new IssuerConnectionException("Failed to connect to " + endpoint, e);
        }
    }

    public CompletableFuture<SwitchMessage> send(SwitchMessage request) {
        if (channel == null || !channel.isActive()) {
            CompletableFuture<SwitchMessage> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new IssuerConnectionException("Channel to " + endpoint + " is not active"));
            return failed;
        }

        String stan = request.getSystemTraceAuditNumber();
        if (stan == null) {
            CompletableFuture<SwitchMessage> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new IssuerConnectionException("Request missing STAN (DE 11)"));
            return failed;
        }

        CompletableFuture<SwitchMessage> responseFuture = new CompletableFuture<>();
        pendingRequests.put(stan, responseFuture);

        log.info("Sending to issuer {}:\n{}", endpoint, request.toSimString());
        channel.writeAndFlush(request).addListener(writeFuture -> {
            if (!writeFuture.isSuccess()) {
                pendingRequests.remove(stan);
                log.error("Failed to send STAN {} to issuer {}: {}",
                        stan, endpoint, writeFuture.cause() != null ? writeFuture.cause().getMessage() : "unknown error");
                responseFuture.completeExceptionally(writeFuture.cause());
            } else {
                log.debug("Queued STAN {} for issuer {}", stan, endpoint);
            }
        });

        channel.eventLoop().schedule(() -> {
            if (!responseFuture.isDone()) {
                pendingRequests.remove(stan);
                SwitchMessage timeout = buildTimeoutResponse(request);
                responseFuture.complete(timeout);
                log.warn("Issuer response timeout for STAN {} on {}", stan, endpoint);
            }
        }, SwitchConstants.DEFAULT_RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        return responseFuture;
    }

    public boolean completePending(String stan, SwitchMessage response) {
        CompletableFuture<SwitchMessage> future = pendingRequests.remove(stan);
        if (future != null) {
            future.complete(response);
            return true;
        }
        return false;
    }

    public boolean completeSinglePending(SwitchMessage response) {
        if (pendingRequests.size() != 1) {
            return false;
        }

        Map.Entry<String, CompletableFuture<SwitchMessage>> entry = pendingRequests.entrySet().iterator().next();
        if (entry == null) {
            return false;
        }

        if (pendingRequests.remove(entry.getKey(), entry.getValue())) {
            entry.getValue().complete(response);
            return true;
        }
        return false;
    }

    public void failAllPending(Throwable cause) {
        pendingRequests.forEach((stan, future) -> future.completeExceptionally(cause));
        pendingRequests.clear();
    }

    private SwitchMessage buildTimeoutResponse(SwitchMessage request) {
        SwitchMessage resp = new SwitchMessage();
        resp.setMessageType(request.getMessageType().toResponseSafe());
        resp.setSystemTraceAuditNumber(request.getSystemTraceAuditNumber());
        resp.setResponseCode(ResponseCode.TIMEOUT);
        if (request.getField(7) != null)  resp.setField(7,  request.getField(7));
        if (request.getField(11) != null) resp.setField(11, request.getField(11));
        if (request.getField(12) != null) resp.setField(12, request.getField(12));
        return resp;
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public void disconnect() {
        if (channel != null) channel.close();
        failAllPending(new IssuerConnectionException("Disconnected from " + endpoint));
        log.info("Disconnected from issuer {}", endpoint);
    }

    public IssuerEndpoint getEndpoint() { return endpoint; }
}
