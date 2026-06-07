package com.paymentswitch.routing;

import java.util.List;
import java.util.Optional;

/**
 * Data-access interface for the BIN routing table.
 * Implementations may source data from a database, file, or in-memory cache.
 */
public interface BinTableRepository {

    /**
     * Find the routing entry for a given 6-digit BIN.
     *
     * @param bin 6-digit Bank Identification Number
     * @return matching entry, or empty if not found
     */
    Optional<BinTableEntry> findByBin(String bin);

    /**
     * Return all active BIN table entries (used for cache warm-up).
     */
    List<BinTableEntry> findAllActive();

    /**
     * Reload the table from the underlying data source.
     * Implementations must be thread-safe.
     */
    void refresh();
}
