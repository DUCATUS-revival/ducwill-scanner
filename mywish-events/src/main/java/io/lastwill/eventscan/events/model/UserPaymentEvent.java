package io.lastwill.eventscan.events.model;

import io.lastwill.eventscan.model.CryptoCurrency;
import io.lastwill.eventscan.model.UserSiteBalance;
import io.mywish.blockchain.WrapperTransaction;
import io.lastwill.eventscan.model.NetworkType;
import lombok.Getter;
import java.math.BigInteger;

@Getter
public class UserPaymentEvent extends PaymentEvent {
    private final UserSiteBalance userSiteBalance;

    public UserPaymentEvent(String address, NetworkType networkType, WrapperTransaction transaction, BigInteger amount, CryptoCurrency currency, boolean isSuccess, UserSiteBalance userSiteBalance) {
        super(networkType, transaction, address, amount, currency, isSuccess);
        this.userSiteBalance = userSiteBalance;
    }
}
