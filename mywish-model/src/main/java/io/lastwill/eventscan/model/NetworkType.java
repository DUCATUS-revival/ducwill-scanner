package io.lastwill.eventscan.model;

import lombok.Getter;

@Getter
public enum NetworkType {
    DUCX_MAINNET(NetworkProviderType.DUCX),
    DUCX_TESTNET(NetworkProviderType.DUCX),
    DUC_MAINNET(NetworkProviderType.DUC),
    DUC_TESTNET(NetworkProviderType.DUC),
    ;
    public final static String DUC_MAINNET_VALUE = "DUC_MAINNET";
    public final static String DUC_TESTNET_VALUE = "DUC_TESTNET";
    public final static String DUCX_MAINNET_VALUE = "DUCX_MAINNET";
    public final static String DUCX_TESTNET_VALUE = "DUCX_TESTNET";

    private final NetworkProviderType networkProviderType;

    NetworkType(NetworkProviderType networkProviderType) {
        this.networkProviderType = networkProviderType;
    }

}
