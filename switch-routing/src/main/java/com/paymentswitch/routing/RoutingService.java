package com.paymentswitch.routing;

import com.paymentswitch.common.model.SwitchMessage;

/**
 * Core routing service interface.
 * Determines the destination issuer for an inbound switch message
 * and populates {@code issuerId} on the message.
 */
public interface RoutingService {

    /**
     * Resolve the issuer for the given message and set {@link SwitchMessage#setIssuerId(String)}.
     *
     * @param message the inbound acquirer message
     * @return the same message with issuerId populated
     * @throws com.paymentswitch.common.exception.RoutingException if no route found
     */
    SwitchMessage route(SwitchMessage message);
}
