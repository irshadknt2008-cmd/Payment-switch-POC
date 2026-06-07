package com.paymentswitch.bootstrap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Payment Switch application entry point.
 *
 * <p>Wires all modules together and starts the switch:</p>
 * <ol>
 *   <li>Load configuration from environment / properties file</li>
 *   <li>Run Flyway database migrations</li>
 *   <li>Build DataSource and MyBatis SqlSessionFactory</li>
 *   <li>Build routing service and BIN table</li>
 *   <li>Build ISO 8583 codec (parser + assembler)</li>
 *   <li>Connect issuer client pool</li>
 *   <li>Start acquirer TCP server</li>
 *   <li>Register JVM shutdown hook</li>
 * </ol>
 */
public class SwitchApplication {

    private static final Logger log = LoggerFactory.getLogger(SwitchApplication.class);

    public static void main(String[] args) throws Exception {
        log.info("=================================================");
        log.info("  Payment Switch – Starting Up");
        log.info("=================================================");

        SwitchContext context = new SwitchContext();
        context.initialize();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook triggered – stopping switch...");
            context.shutdown();
        }, "shutdown-hook"));

        log.info("Payment Switch is running. Press Ctrl+C to stop.");
        // Block main thread
        Thread.currentThread().join();
    }
}
