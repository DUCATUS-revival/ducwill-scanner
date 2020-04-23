package io.lastwill.eventscan.model;

import lombok.Getter;

@Getter
public enum NetworkType {
    DUCATUSX_MAINNET(NetworkProviderType.DUCX),
    ETHEREUM_MAINNET(NetworkProviderType.ETH),
    ETHEREUM_TESTNET(NetworkProviderType.ETH),
    DUCATUSX_TESTNET(NetworkProviderType.DUCX),
    DUCATUS_MAINNET(NetworkProviderType.DUC),
    DUCATUS_TESTNET(NetworkProviderType.DUC),
    ;
    public final static String DUC_MAINNET_VALUE = "DUCATUS_MAINNET";
    public final static String DUC_TESTNET_VALUE = "DUCATUS_TESTNET";
    public final static String DUCX_MAINNET_VALUE = "DUCATUSX_MAINNET";
    public final static String DUCX_TESTNET_VALUE = "DUCATUSX_TESTNET";
    public final static String ETH_MAINNET_VALUE = "ETHEREUM_MAINNET";
    public final static String ETH_TESTNET_VALUE = "ETHEREUM_TESTNET";

    private final NetworkProviderType networkProviderType;

    NetworkType(NetworkProviderType networkProviderType) {
        this.networkProviderType = networkProviderType;
    }

}
