package io.lastwill.eventscan.services.handlers;

import io.lastwill.eventscan.events.model.ProductPaymentEvent;
import io.lastwill.eventscan.messages.ContractPaymentNotify;
import io.lastwill.eventscan.messages.PaymentStatus;
import io.lastwill.eventscan.services.ExternalNotifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductBtcPaymentEventHandler {
    @Autowired
    private ExternalNotifier externalNotifier;

    @EventListener
    private void handleBtcBlock(ProductPaymentEvent event) {
        if (event.getTransactionOutput().getParentTransaction() == null) {
            log.warn("Skip it. Output {} has not parent transaction.", event.getTransactionOutput());
            return;
        }
        externalNotifier.send(event.getNetworkType(), new ContractPaymentNotify(
                event.getTransactionOutput().getValue(),
                PaymentStatus.COMMITTED,
                event.getTransactionOutput().getParentTransaction(),
                event.getTransactionOutput().getIndex(),
                event.getProduct()
        ));
    }
}
