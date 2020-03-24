package io.lastwill.eventscan.messages;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public abstract class NotifyContract extends BaseNotify {
    private final int contractId;

    public NotifyContract(int contractId, PaymentStatus status, String txHash) {
        super(status, txHash);
        this.contractId = contractId;
    }
}
