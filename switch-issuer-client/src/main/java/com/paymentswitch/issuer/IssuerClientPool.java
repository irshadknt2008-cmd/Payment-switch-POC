package com.paymentswitch.issuer;

import com.paymentswitch.common.exception.IssuerConnectionException;
import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.iso8583.IsoMessageAssembler;
import com.paymentswitch.iso8583.IsoMessageParser;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages issuer TCP connections with automatic background reconnect.
 */
public class IssuerClientPool {

    private static final Logger log = LoggerFactory.getLogger(IssuerClientPool.class);

    private final Map<String, IssuerEndpoint> endpoints = new ConcurrentHashMap<>();
    private final Map<String, List<IssuerConnection>> pools = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobinCounters = new ConcurrentHashMap<>();

    private final EventLoopGroup eventLoopGroup;
    private final IsoMessageParser parser;
    private final IsoMessageAssembler assembler;
    private final ScheduledExecutorService reconnectExecutor;

    public IssuerClientPool(IsoMessageParser parser, IsoMessageAssembler assembler) {
        this.eventLoopGroup = new NioEventLoopGroup();
        this.parser         = parser;
        this.assembler      = assembler;
        this.reconnectExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "issuer-reconnect");
            t.setDaemon(true);
            return t;
        });
        this.reconnectExecutor.scheduleWithFixedDelay(
                this::reconnectAll, 10, 15, TimeUnit.SECONDS);
    }

    public void addEndpoint(IssuerEndpoint endpoint) {
        endpoints.put(endpoint.getIssuerId(), endpoint);
        List<IssuerConnection> connections = new ArrayList<>();
        pools.put(endpoint.getIssuerId(), connections);
        roundRobinCounters.put(endpoint.getIssuerId(), new AtomicInteger(0));

        for (int i = 0; i < endpoint.getConnectionPoolSize(); i++) {
            IssuerConnection conn = new IssuerConnection(endpoint, eventLoopGroup, parser, assembler);
            if (tryConnect(conn, i + 1, endpoint.getConnectionPoolSize())) {
                connections.add(conn);
            }
        }
        log.info("Pool for issuer {} ready with {} active connection(s)",
                endpoint.getIssuerId(), connections.size());
    }

    public CompletableFuture<SwitchMessage> send(SwitchMessage message) {
        String issuerId = message.getIssuerId();
        ensureConnections(issuerId);

        List<IssuerConnection> connections = pools.get(issuerId);
        if (connections == null || connections.isEmpty()) {
            CompletableFuture<SwitchMessage> failed = new CompletableFuture<>();
            failed.completeExceptionally(
                    new IssuerConnectionException("Issuer " + issuerId + " is not reachable"));
            return failed;
        }

        int size = connections.size();
        for (int attempt = 0; attempt < size; attempt++) {
            int idx = roundRobinCounters.get(issuerId).getAndIncrement() % size;
            IssuerConnection conn = connections.get(idx);
            if (conn.isActive()) {
                log.info("Dispatching STAN {} to issuer {} via {}",
                        message.getSystemTraceAuditNumber(), issuerId, conn.getEndpoint());
                return conn.send(message);
            }
            tryConnect(conn, idx + 1, size);
            if (conn.isActive()) {
                log.info("Dispatching STAN {} to issuer {} via {}",
                        message.getSystemTraceAuditNumber(), issuerId, conn.getEndpoint());
                return conn.send(message);
            }
        }

        CompletableFuture<SwitchMessage> failed = new CompletableFuture<>();
        failed.completeExceptionally(
                new IssuerConnectionException("All connections to issuer " + issuerId + " are down"));
        return failed;
    }

    private void ensureConnections(String issuerId) {
        List<IssuerConnection> connections = pools.get(issuerId);
        IssuerEndpoint endpoint = endpoints.get(issuerId);
        if (endpoint == null) return;

        if (connections == null) return;

        if (connections.isEmpty()) {
            IssuerConnection conn = new IssuerConnection(endpoint, eventLoopGroup, parser, assembler);
            if (tryConnect(conn, 1, 1)) {
                connections.add(conn);
            }
            return;
        }

        for (IssuerConnection conn : connections) {
            if (!conn.isActive()) {
                tryConnect(conn, 0, 0);
            }
        }
    }

    private void reconnectAll() {
        for (Map.Entry<String, List<IssuerConnection>> entry : pools.entrySet()) {
            String issuerId = entry.getKey();
            List<IssuerConnection> connections = entry.getValue();
            IssuerEndpoint endpoint = endpoints.get(issuerId);

            if (connections.isEmpty() && endpoint != null) {
                IssuerConnection conn = new IssuerConnection(endpoint, eventLoopGroup, parser, assembler);
                if (tryConnect(conn, 1, endpoint.getConnectionPoolSize())) {
                    connections.add(conn);
                    log.info("Background reconnect: issuer {} now has 1 connection", issuerId);
                }
                continue;
            }

            for (IssuerConnection conn : connections) {
                if (!conn.isActive()) {
                    if (tryConnect(conn, 0, 0)) {
                        log.info("Background reconnect: restored {}", endpoint);
                    }
                }
            }
        }
    }

    private boolean tryConnect(IssuerConnection conn, int index, int total) {
        try {
            conn.reconnect();
            if (index > 0) {
                log.info("Connected to issuer {} ({}/{})", conn.getEndpoint(), index, total);
            }
            return true;
        } catch (IssuerConnectionException e) {
            log.debug("Issuer {} not reachable: {}", conn.getEndpoint(), e.getMessage());
            return false;
        }
    }

    public void shutdown() {
        reconnectExecutor.shutdownNow();
        pools.values().forEach(list -> list.forEach(IssuerConnection::disconnect));
        eventLoopGroup.shutdownGracefully();
        log.info("IssuerClientPool shut down.");
    }
}
