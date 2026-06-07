package com.paymentswitch.routing.impl;

import com.paymentswitch.common.exception.RoutingException;
import com.paymentswitch.common.model.SwitchMessage;
import com.paymentswitch.common.util.PanUtil;
import com.paymentswitch.routing.BinTableEntry;
import com.paymentswitch.routing.BinTableRepository;
import com.paymentswitch.routing.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * BIN-based routing service.
 * Extracts the BIN from DE 2 (PAN) and looks it up in the BIN table
 * to determine the destination issuer.
 */
public class BinRoutingService implements RoutingService {

    private static final Logger log = LoggerFactory.getLogger(BinRoutingService.class);

    private final BinTableRepository binTableRepository;

    public BinRoutingService(BinTableRepository binTableRepository) {
        this.binTableRepository = binTableRepository;
    }

    @Override
    public SwitchMessage route(SwitchMessage message) {
        String pan = message.getPan();
        if (pan == null || pan.length() < 6) {
            throw new RoutingException("Cannot route message without a valid PAN: " + message.getMessageId());
        }

        String bin = PanUtil.extractBin(pan);
        log.debug("Routing message {} using BIN {}", message.getMessageId(), bin);

        Optional<BinTableEntry> entry = binTableRepository.findByBin(bin);
        if (!entry.isPresent()) {
            throw new RoutingException("No BIN table entry found for BIN " + bin
                    + " (message: " + message.getMessageId() + ")");
        }

        BinTableEntry route = entry.get();
        if (!route.isActive()) {
            throw new RoutingException("BIN route is inactive for BIN " + bin
                    + " issuer: " + route.getIssuerId());
        }

        message.setIssuerId(route.getIssuerId());
        log.info("Routed message {} BIN={} -> issuer={}", message.getMessageId(), bin, route.getIssuerId());
        return message;
    }
}
