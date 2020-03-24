package io.lastwill.eventscan.services.monitors;

import io.lastwill.eventscan.events.model.ContractEventsEvent;
import io.lastwill.eventscan.events.model.ContractTransactionFailedEvent;
import io.lastwill.eventscan.model.Contract;
import io.lastwill.eventscan.model.NetworkType;
import io.lastwill.eventscan.repositories.ContractRepository;
import io.lastwill.eventscan.services.TransactionProvider;
import io.mywish.scanner.model.NewBlockEvent;
import io.mywish.scanner.services.EventPublisher;
import io.mywish.blockchain.ContractEvent;
import io.mywish.blockchain.WrapperBlock;
import io.mywish.blockchain.WrapperTransaction;
import io.mywish.blockchain.WrapperTransactionReceipt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class ContractsMonitor {
    private final HashMap<NetworkType, String> proxyByNetwork = new HashMap<>();
    @Autowired
    private ContractRepository contractRepository;
    @Autowired
    private EventPublisher eventPublisher;
    @Autowired
    private TransactionProvider transactionProvider;
    @Value("${io.lastwill.eventscan.contract.proxy-address.ethereum}")
    private String proxyAddressEthereum;
    @Value("${io.lastwill.eventscan.contract.proxy-address.ropsten}")
    private String proxyAddressRopsten;
    @Value("${io.lastwill.eventscan.contract.skip-addresses}")
    private String skipAddressesLine;
    private List<String> skipAddresses = Collections.emptyList();

    @PostConstruct
    protected void init() {
        proxyByNetwork.put(NetworkType.ETHEREUM_MAINNET, proxyAddressEthereum.toLowerCase());
        proxyByNetwork.put(NetworkType.ETHEREUM_ROPSTEN, proxyAddressRopsten.toLowerCase());
        if (skipAddressesLine == null || skipAddressesLine.isEmpty()) {
            return;
        }
        skipAddresses = Stream.of(skipAddressesLine.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    @EventListener
    private void onNewBlockEvent(final NewBlockEvent event) {
        // skip eos because address is not unique in our contract model
//        if (event.getNetworkType() == NetworkType.EOS_MAINNET || event.getNetworkType() == NetworkType.EOS_TESTNET) {
//            return;
//        }
        Set<String> addresses = new HashSet<>(event.getTransactionsByAddress().keySet());
        // remove addresses to ignore
        addresses.removeAll(skipAddresses);

        if (addresses.isEmpty()) {
            return;
        }

        if (proxyByNetwork.containsKey(event.getNetworkType())) {
            final String proxyAddress = proxyByNetwork.get(event.getNetworkType());

            if (addresses.contains(proxyAddress)) {
                final List<WrapperTransaction> transactions = event.getTransactionsByAddress().get(proxyAddress);
                grabProxyEvents(event.getNetworkType(), transactions, event.getBlock());
            }
        }

        List<Contract> contracts = contractRepository.findByAddressesList(addresses, event.getNetworkType());
        for (final Contract contract : contracts) {
            if (contract.getAddress() == null || !addresses.contains(contract.getAddress().toLowerCase())) {
                continue;
            }

            final List<WrapperTransaction> transactions = event
                    .getTransactionsByAddress()
                    .get(contract.getAddress().toLowerCase());
            for (final WrapperTransaction transaction : transactions) {
                // grab events
                if (transaction.getOutputs().size() == 0) {
                    continue;
                }

                grabContractEvents(event.getNetworkType(), contract, transaction, event.getBlock());
            }
        }
    }

    private void grabProxyEvents(
            final NetworkType networkType,
            final List<WrapperTransaction> transactions,
            final WrapperBlock block
    ) {
        for (WrapperTransaction transaction : transactions) {
            try {
                WrapperTransactionReceipt transactionReceipt = transactionProvider.getTransactionReceipt(networkType, transaction);
                MultiValueMap<String, ContractEvent> logsByAddress = CollectionUtils.toMultiValueMap(new HashMap<>());
                for (ContractEvent contractEvent : transactionReceipt.getLogs()) {
                    logsByAddress.add(contractEvent.getAddress(), contractEvent);
                }

                for (Contract contract : contractRepository.findByAddressesList(logsByAddress.keySet(), networkType)) {
                    handleReceiptAndContract(
                            networkType,
                            contract,
                            transaction,
                            transactionReceipt,
                            block
                    );
                }
            }
            catch (Exception e) {
                log.error("ContractEventsEvent handling cause exception.", e);
            }
        }
    }

    private void grabContractEvents(
            final NetworkType networkType,
            final Contract contract,
            final WrapperTransaction transaction,
            final WrapperBlock block
    ) {
        try {
            WrapperTransactionReceipt transactionReceipt = transactionProvider.getTransactionReceipt(networkType, transaction);
            handleReceiptAndContract(
                    networkType,
                    contract,
                    transaction,
                    transactionReceipt,
                    block
            );
        }
        catch (Exception e) {
            log.error("ContractEventsEvent handling cause exception.", e);
        }
    }

    private void handleReceiptAndContract(
            final NetworkType networkType,
            final Contract contract,
            final WrapperTransaction transaction,
            final WrapperTransactionReceipt transactionReceipt,
            final WrapperBlock block
    ) {
        if (!transactionReceipt.isSuccess()) {
            eventPublisher.publish(new ContractTransactionFailedEvent(
                    networkType,
                    contract,
                    transaction,
                    transactionReceipt,
                    block
            ));
        }

        List<ContractEvent> events = transactionReceipt.getLogs();
        if (events.isEmpty()) {
            return;
        }
        eventPublisher.publish(
                new ContractEventsEvent(
                        networkType,
                        contract,
                        events,
                        transaction,
                        transactionReceipt,
                        block
                )
        );
    }
}
