package io.mywish.web3.blockchain;

import io.lastwill.eventscan.model.NetworkType;
import io.lastwill.eventscan.repositories.LastBlockRepository;
import io.mywish.scanner.services.LastBlockDbPersister;
import io.mywish.scanner.services.LastBlockPersister;
import io.mywish.web3.blockchain.service.Web3Network;
import io.mywish.web3.blockchain.service.Web3Scanner;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.websocket.WebSocketClient;

import java.net.ConnectException;
import java.net.URI;

@Configuration
@ComponentScan
public class Web3BCModule {
    @ConditionalOnProperty(name = "io.lastwill.eventscan.ducatusx.mainnet")
    @Bean(name = NetworkType.DUCX_MAINNET_VALUE)
    public Web3Network ducxNetMain(
            @Value("${io.lastwill.eventscan.ducatusx.mainnet}") URI web3Url,
            @Value("${etherscanner.polling-interval-ms:5000}") Long pollingInterval,
            @Value("${etherscanner.pending-transactions-threshold}") int pendingThreshold) throws ConnectException {
        return new Web3Network(
                NetworkType.DUCATUSX_MAINNET,
                new WebSocketClient(web3Url),
                pollingInterval,
                pendingThreshold);
    }

    @ConditionalOnProperty(name = "io.lastwill.eventscan.ducatusx.testnet")
    @Bean(name = NetworkType.DUCX_TESTNET_VALUE)
    public Web3Network ducxNetRopsten(
            @Value("${io.lastwill.eventscan.ducatusx.testnet}") URI web3Url,
            @Value("${etherscanner.polling-interval-ms:5000}") Long pollingInterval,
            @Value("${etherscanner.pending-transactions-threshold}") int pendingThreshold) throws ConnectException {
        return new Web3Network(
                NetworkType.DUCATUSX_TESTNET,
                new WebSocketClient(web3Url),
                pollingInterval,
                pendingThreshold);
    }

    @Configuration
    public class DucxDbPersisterConfiguration {
        @Bean
        public LastBlockPersister ducxMainnetLastBlockPersister(
                LastBlockRepository lastBlockRepository
        ) {
            return new LastBlockDbPersister(NetworkType.DUCATUSX_MAINNET, lastBlockRepository, null);
        }

        @Bean
        public LastBlockPersister ducxRopstenLastBlockPersister(
                LastBlockRepository lastBlockRepository
        ) {
            return new LastBlockDbPersister(NetworkType.DUCATUSX_TESTNET, lastBlockRepository, null);
        }
    }

    @ConditionalOnBean(name = NetworkType.DUCX_MAINNET_VALUE)
    @Bean
    public Web3Scanner ducxScannerMain(
            final @Qualifier(NetworkType.DUCX_MAINNET_VALUE) Web3Network network,
            final @Qualifier("ducxMainnetLastBlockPersister") LastBlockPersister lastBlockPersister,
            final @Value("${etherscanner.polling-interval-ms:5000}") Long pollingInterval,
            final @Value("${etherscanner.commit-chain-length:5}") Integer commitmentChainLength
    ) {
        return new Web3Scanner(
                network,
                lastBlockPersister,
                pollingInterval,
                commitmentChainLength
        );
    }

    @ConditionalOnBean(name = NetworkType.DUCX_TESTNET_VALUE)
    @Bean
    public Web3Scanner ducxScannerTestnet(
            final @Qualifier(NetworkType.DUCX_TESTNET_VALUE) Web3Network network,
            final @Qualifier("ducxRopstenLastBlockPersister") LastBlockPersister lastBlockPersister,
            final @Value("${etherscanner.polling-interval-ms:5000}") Long pollingInterval,
            final @Value("${etherscanner.commit-chain-length:5}") Integer commitmentChainLength
    ) {
        return new Web3Scanner(
                network,
                lastBlockPersister,
                pollingInterval,
                commitmentChainLength
        );
    }
}
