package io.lastwill.eventscan.model;

import lombok.Getter;

import java.util.EnumSet;

@Getter
public enum NetworkType {
    ETHEREUM_MAINNET(NetworkProviderType.WEB3),
    ETHEREUM_ROPSTEN(NetworkProviderType.WEB3),
    BTC_MAINNET(NetworkProviderType.BTC),
    BTC_TESTNET_3(NetworkProviderType.BTC),
    DUC_MAINNET(NetworkProviderType.DUC),
    DUC_TESTNET(NetworkProviderType.DUC),
    DUCX_MAINNET(NetworkProviderType.DUCX),
    DUCX_TESTNET(NetworkProviderType.DUCX),
    ;
    public final static String DUC_MAINNET_VALUE = "DUC_MAINNET";
    public final static String DUC_TESTNET_VALUE = "DUC_TESTNET";
    public final static String DUCX_MAINNET_VALUE = "DUCX_MAINNET";
    public final static String DUCX_TESTNET_VALUE = "DUCX_TESTNET";
    public final static String ETHEREUM_MAINNET_VALUE = "ETHEREUM_MAINNET";
    public final static String ETHEREUM_ROPSTEN_VALUE = "ETHEREUM_ROPSTEN";

    private final NetworkProviderType networkProviderType;

    NetworkType(NetworkProviderType networkProviderType) {
        this.networkProviderType = networkProviderType;
    }

}
