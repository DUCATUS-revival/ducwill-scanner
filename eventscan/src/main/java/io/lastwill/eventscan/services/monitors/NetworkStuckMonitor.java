package io.lastwill.eventscan.services.monitors;

import io.lastwill.eventscan.events.model.utility.NetworkStuckEvent;
import io.lastwill.eventscan.events.model.utility.PendingStuckEvent;
import io.lastwill.eventscan.model.NetworkType;
import io.mywish.scanner.model.NewBlockEvent;
import io.mywish.scanner.model.NewPendingTransactionsEvent;
import io.mywish.scanner.services.EventPublisher;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class NetworkStuckMonitor {
    private final ConcurrentHashMap<NetworkType, LastEvent> lastBlockEvents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NetworkType, LastEvent> lastPendingTxEvents = new ConcurrentHashMap<>();

    private final Map<NetworkType, Long> checkFrequencies = new HashMap<>();
    private final ConcurrentHashMap<NetworkType, Long> notifyFrequencies = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${io.lastwill.eventscan.network-stuck.interval.duc}")
    private long ducInterval;
    @Value("${io.lastwill.eventscan.network-stuck.interval.ducx}")
    private long ethInterval;

    @Value("${io.lastwill.eventscan.network-stuck.interval.pending}")
    private long pendingInterval;
    @Value("${io.lastwill.eventscan.network-stuck.interval.max-notification}")
    private long maxFrequency;

    @Autowired
    private EventPublisher eventPublisher;

    @PostConstruct
    protected void init() {
        checkFrequencies.put(NetworkType.DUCX_MAINNET, ethInterval);
        checkFrequencies.put(NetworkType.DUCX_TESTNET, ethInterval);
        checkFrequencies.put(NetworkType.DUC_MAINNET, ducInterval);

        notifyFrequencies.putAll(checkFrequencies);

        long checkFrequency = checkFrequencies.values()
                .stream()
                .min(Long::compareTo)
                .orElseThrow(NoSuchElementException::new);

        scheduler.scheduleWithFixedDelay(this::checkNetworks, checkFrequency, checkFrequency, TimeUnit.MILLISECONDS);
    }

    @EventListener
    private void newBlockEvent(NewBlockEvent event) {
        lastBlockEvents.put(
                event.getNetworkType(),
                new LastEvent(
                        LocalDateTime.now(ZoneOffset.UTC),
                        event.getBlock().getTimestamp(),
                        event.getBlock().getNumber()
                )
        );
    }

    @EventListener
    private void newPendingTx(NewPendingTransactionsEvent event) {
        lastPendingTxEvents.put(
                event.getNetworkType(),
                new LastEvent(
                        LocalDateTime.now(ZoneOffset.UTC),
                        Instant.now(),
                        event.getPendingTransactions().size()
                )
        );
    }

    public Map<NetworkType, LastEvent> getLastBlockEvents() {
        return Collections.unmodifiableMap(this.lastBlockEvents);
    }

    public Map<NetworkType, LastEvent> getLastPendingTxEvents() {
        return Collections.unmodifiableMap(this.lastPendingTxEvents);
    }

    protected void checkNetworks() {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        lastBlockEvents.keySet()
                .forEach(networkType -> {
                    LastEvent lastEvent = lastBlockEvents.get(networkType);
                    long checkFrequency = checkFrequencies.get(networkType);
                    // last block + interval is in future
                    if (lastEvent.receivedTime.plusSeconds(checkFrequency / 1000).isAfter(now)) {
                        notifyFrequencies.put(networkType, checkFrequency);
                        return;
                    }

                    long notifyFrequency = notifyFrequencies.get(networkType);
                    if (lastEvent.receivedTime.plusSeconds(notifyFrequency / 1000).isAfter(now)) {
                        return;
                    }

                    eventPublisher.publish(
                            new NetworkStuckEvent(
                                    networkType,
                                    lastEvent.receivedTime,
                                    lastEvent.timestamp,
                                    lastEvent.blockNo
                            )
                    );

                    notifyFrequency = notifyFrequency * 2 < maxFrequency
                            ? notifyFrequency * 2
                            : maxFrequency;
                    notifyFrequencies.put(networkType, notifyFrequency);
                });
    }

    //    @Scheduled(fixedDelayString = "${io.lastwill.eventscan.network-stuck.interval.pending}", initialDelayString = "${io.lastwill.eventscan.network-stuck.interval.pending}")
    protected void checkPending() {
        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        lastPendingTxEvents.keySet()
                .stream()
//                .filter(networkType -> networkType == NetworkType.BTC_MAINNET || networkType == NetworkType.BTC_TESTNET_3)
                .forEach(networkType -> {
                    LastEvent lastEvent = lastPendingTxEvents.get(networkType);
                    // last block + interval is in future
                    if (lastEvent.receivedTime.plusSeconds(pendingInterval / 1000).isAfter(now)) {
                        return;
                    }

                    eventPublisher.publish(
                            new PendingStuckEvent(
                                    networkType,
                                    lastEvent.receivedTime,
                                    (int) lastEvent.blockNo
                            )
                    );
                });
    }

    @Getter
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LastEvent {
        private final LocalDateTime receivedTime;
        private final Instant timestamp;
        private final long blockNo;
    }
}
