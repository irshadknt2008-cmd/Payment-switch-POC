package com.paymentswitch.bootstrap;

import com.paymentswitch.acquirer.AcquirerServer;
import com.paymentswitch.acquirer.handler.AcquirerChannelInitializer;
import com.paymentswitch.acquirer.handler.AcquirerMessageHandler;
import com.paymentswitch.iso8583.Iso8583Codec;
import com.paymentswitch.iso8583.IsoMessageAssembler;
import com.paymentswitch.iso8583.IsoMessageParser;
import com.paymentswitch.issuer.IssuerClientPool;
import com.paymentswitch.issuer.IssuerEndpoint;
import com.paymentswitch.persistence.config.DataSourceConfig;
import com.paymentswitch.persistence.config.FlywayMigrator;
import com.paymentswitch.persistence.config.MyBatisConfig;
import com.paymentswitch.routing.RoutingService;
import com.paymentswitch.routing.impl.BinRoutingService;
import com.paymentswitch.routing.impl.InMemoryBinTableRepository;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SwitchContext {

    private static final Logger log = LoggerFactory.getLogger(SwitchContext.class);

    private DataSource dataSource;
    private SqlSessionFactory sqlSessionFactory;
    private IsoMessageParser isoParser;
    private IsoMessageAssembler isoAssembler;
    private RoutingService routingService;
    private IssuerClientPool issuerClientPool;
    private AcquirerServer acquirerServer;
    private String defaultIssuerId;

    public void initialize() throws InterruptedException {
        Properties config = loadConfig();

        log.info("[1/6] Initialising persistence...");
        dataSource = new DataSourceConfig(config).build();
        new FlywayMigrator(dataSource).migrate();
        sqlSessionFactory = new MyBatisConfig(dataSource).build();

        log.info("[2/6] Building ISO 8583 codecs...");
        Iso8583Codec codec = new Iso8583Codec();
        isoParser    = codec.getParser();
        isoAssembler = codec.getAssembler();

        log.info("[3/6] Building routing service...");
        InMemoryBinTableRepository binRepo = new InMemoryBinTableRepository();
        routingService = new BinRoutingService(binRepo);

        defaultIssuerId = config.getProperty("default.issuer", "9009");

        log.info("[4/6] Connecting issuer client pool...");
        issuerClientPool = new IssuerClientPool(isoParser, isoAssembler);
        registerIssuers(config, issuerClientPool);

        log.info("[5/6] Starting acquirer server...");
        AcquirerMessageHandler messageHandler =
                new AcquirerMessageHandler(routingService, issuerClientPool, defaultIssuerId);
        AcquirerChannelInitializer initializer =
                new AcquirerChannelInitializer(isoParser, isoAssembler, messageHandler);

        int acquirerPort = Integer.parseInt(config.getProperty("acquirer.port", "9998"));
        acquirerServer = new AcquirerServer(acquirerPort, initializer);
        acquirerServer.start();

        log.info("[6/6] Switch fully initialised and ready.");
        log.info("  Acquirer listening on port {}", acquirerPort);
        log.info("  Default issuer: {} (auto-reconnect every 15s if down)", defaultIssuerId);
    }

    public void shutdown() {
        if (acquirerServer   != null) acquirerServer.stop();
        if (issuerClientPool != null) issuerClientPool.shutdown();
        log.info("Switch context shut down cleanly.");
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/switch.properties")) {
            if (is != null) {
                props.load(is);
                log.info("Loaded configuration from switch.properties");
            } else {
                log.warn("switch.properties not found; using defaults");
            }
        } catch (IOException e) {
            log.warn("Failed to read switch.properties: {}", e.getMessage());
        }
        return props;
    }

    private void registerIssuers(Properties config, IssuerClientPool pool) {
        String issuerList = config.getProperty("issuers", "");
        if (issuerList.isEmpty()) {
            log.warn("No issuers configured (issuers=). Issuer forwarding disabled.");
            return;
        }

        String fallbackHost = config.getProperty("switch.issuer.host", "127.0.0.1");
        String fallbackPort = config.getProperty("switch.issuer.port", "9009");

        for (String id : issuerList.split(",")) {
            id = id.trim();
            String host = config.getProperty("issuer." + id + ".host", fallbackHost);
            int port = Integer.parseInt(
                    config.getProperty("issuer." + id + ".port", fallbackPort));
            int poolSz = Integer.parseInt(
                    config.getProperty("issuer." + id + ".pool", "1"));
            pool.addEndpoint(new IssuerEndpoint(id, host, port, poolSz));
            log.info("Registered issuer {} at {}:{}", id, host, port);
        }
    }
}
