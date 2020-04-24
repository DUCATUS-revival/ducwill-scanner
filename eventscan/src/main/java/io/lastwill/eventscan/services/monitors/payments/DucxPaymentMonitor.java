package io.lastwill.eventscan.services.monitors.payments;

import io.lastwill.eventscan.events.model.UserPaymentEvent;
import io.lastwill.eventscan.model.CryptoCurrency;
import io.lastwill.eventscan.model.NetworkType;
import io.lastwill.eventscan.model.UserSiteBalance;
import io.lastwill.eventscan.repositories.UserSiteBalanceRepository;
import io.lastwill.eventscan.services.TransactionProvider;
import io.mywish.blockchain.WrapperTransaction;
import io.mywish.scanner.model.NewBlockEvent;
import io.mywish.scanner.services.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class DucxPaymentMonitor {
    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private UserSiteBalanceRepository userSiteBalanceRepository;

    @Autowired
    private TransactionProvider transactionProvider;

    @EventListener
    private void onNewBlockEvent(NewBlockEvent event) {
        // payments only in mainnet works
        if (event.getNetworkType() != NetworkType.DUCATUSX_MAINNET) {
            return;
        }

        Set<String> addresses = event.getTransactionsByAddress().keySet();
        if (addresses.isEmpty()) {
            return;
        }

        List<UserSiteBalance> userSiteBalances = userSiteBalanceRepository.findByEthAddressesList(addresses);
        for (UserSiteBalance userSiteBalance : userSiteBalances) {
            final List<WrapperTransaction> transactions = event.getTransactionsByAddress().get(
                    userSiteBalance.getEthAddress().toLowerCase()
            );

            if (transactions == null) {
                log.error("User {} received from DB, but was not found in transaction list (block {}).", userSiteBalance, event.getBlock().getNumber());
                continue;
            }

            transactions.forEach(transaction -> {
                if (!userSiteBalance.getEthAddress().equalsIgnoreCase(transaction.getOutputs().get(0).getAddress())) {
                    log.debug("Found transaction out from internal address. Skip it.");
                    return;
                }
                transactionProvider.getTransactionReceiptAsync(event.getNetworkType(), transaction)
                        .thenAccept(receipt -> {
                            eventPublisher.publish(new UserPaymentEvent(
                                    userSiteBalance.getEthAddress(),
                                    event.getNetworkType(),
                                    transaction,
                                    getAmountFor(userSiteBalance.getEthAddress(), transaction),
                                    CryptoCurrency.DUCX,
                                    receipt.isSuccess(),
                                    userSiteBalance));
                        })
                        .exceptionally(throwable -> {
                            log.error("UserPaymentEvent handling failed.", throwable);
                            return null;
                        });
            });
        }
    }

    private BigInteger getAmountFor(final String address, final WrapperTransaction transaction) {
        BigInteger result = BigInteger.ZERO;
        if (address.equalsIgnoreCase(transaction.getInputs().get(0))) {
            result = result.subtract(transaction.getOutputs().get(0).getValue());
        }
        if (address.equalsIgnoreCase(transaction.getOutputs().get(0).getAddress())) {
            result = result.add(transaction.getOutputs().get(0).getValue());
        }
        return result;
    }
}
