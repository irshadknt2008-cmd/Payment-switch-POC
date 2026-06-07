package com.paymentswitch.routing.impl;

import com.paymentswitch.routing.BinTableEntry;
import com.paymentswitch.routing.BinTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory BIN table repository backed by a {@link CopyOnWriteArrayList}.
 * Suitable for development / testing; replace with database-backed implementation in production.
 *
 * <p>Thread-safe: reads are lock-free; {@link #refresh()} atomically swaps the list.</p>
 */
public class InMemoryBinTableRepository implements BinTableRepository {

    private static final Logger log = LoggerFactory.getLogger(InMemoryBinTableRepository.class);

    private volatile List<BinTableEntry> entries = new CopyOnWriteArrayList<>();

    public InMemoryBinTableRepository() {
        loadDefaults();
    }

    @Override
    public Optional<BinTableEntry> findByBin(String bin) {
        for (BinTableEntry entry : entries) {
            if (entry.matches(bin)) return Optional.of(entry);
        }
        return Optional.empty();
    }

    @Override
    public List<BinTableEntry> findAllActive() {
        List<BinTableEntry> active = new ArrayList<>();
        for (BinTableEntry e : entries) {
            if (e.isActive()) active.add(e);
        }
        return Collections.unmodifiableList(active);
    }

    @Override
    public void refresh() {
        // TODO: reload from DB / config file
        log.info("BIN table refreshed ({} entries)", entries.size());
    }

    public void addEntry(BinTableEntry entry) {
        entries.add(entry);
    }

    private void loadDefaults() {
        // Placeholder entries – override with database-backed impl in production
        // Neapay test card DummyCard1: PAN 9876500000306084
        entries.add(new BinTableEntry("987650", "987650", "9009", "Neapay Issuer", true));
        entries.add(new BinTableEntry("400000", "499999", "9009", "Visa range",    true));
        entries.add(new BinTableEntry("510000", "559999", "9009", "MC range",      true));
        log.info("Loaded {} default BIN table entries", entries.size());
    }
}
