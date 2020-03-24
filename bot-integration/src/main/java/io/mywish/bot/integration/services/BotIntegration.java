package io.mywish.bot.integration.services;

import io.lastwill.eventscan.events.model.*;
import io.lastwill.eventscan.events.model.contract.CreateAccountEvent;
import io.lastwill.eventscan.events.model.utility.NetworkStuckEvent;
import io.lastwill.eventscan.events.model.utility.PendingStuckEvent;
import io.lastwill.eventscan.model.*;
import io.lastwill.eventscan.repositories.UserRepository;
import io.mywish.blockchain.ContractEvent;
import io.mywish.bot.service.MyWishBot;
import io.mywish.bot.service.MyWishBotLight;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class BotIntegration {
    @Autowired
    private MyWishBot bot;

    @Autowired
    private MyWishBotLight botLight;

    @Autowired
    private UserRepository userRepository;

    @Value("${io.lastwill.eventscan.contract.crowdsale-address}")
    private String crowdsaleAddress;

    @Autowired
    protected BlockchainExplorerProvider explorerProvider;

    private final ZoneId localZone = ZoneId.of("Europe/Moscow");
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<NetworkType, String> networkName = new HashMap<NetworkType, String>() {{
        put(NetworkType.DUCX_MAINNET, "DUCX");
        put(NetworkType.DUCX_TESTNET, "tDUCX");
        put(NetworkType.DUC_MAINNET, "DUC");

    }};

    private final String defaultNetwork = "unknown";

    private String getUser(int userId) {
        User user = userRepository.findOne(userId);
        String email = user.getEmail();
        return email != null && !email.isEmpty() ? email : "user " + userId;
    }

    @EventListener
    private void onContractCreated(final ContractCreatedEvent contractCreatedEvent) {
        final Contract contract = contractCreatedEvent.getContract();
        final Product product = contract.getProduct();
        final String type = ProductStatistics.PRODUCT_TYPES.get(product.getContractType());
        final String user = getUser(product.getUserId());
        final String network = networkName.getOrDefault(product.getNetwork().getType(), defaultNetwork);
        final String txLink = explorerProvider.getOrStub(product.getNetwork().getType())
                .buildToTransaction(contractCreatedEvent.getTransaction().getHash());
        final String addressLink = explorerProvider.getOrStub(product.getNetwork().getType())
                .buildToAddress(contractCreatedEvent.getAddress());

        if (contractCreatedEvent.isSuccess()) {

            bot.onContract(
                    network,
                    product.getId(),
                    type,
                    contract.getId(),
                    toCurrency(CryptoCurrency.USD, product.getCost()),
                    user,
                    contractCreatedEvent.getAddress(),
                    addressLink
            );
        } else {
            bot.onContractFailed(network, product.getId(), type, contract.getId(), txLink);

        }
    }


    @EventListener
    private void onOwnerBalanceChanged(final UserPaymentEvent event) {
        try {
            final UserSiteBalance userSiteBalance = event.getUserSiteBalance();
            final String network = networkName.getOrDefault(event.getNetworkType(), defaultNetwork);
            final String txLink = explorerProvider.getOrStub(event.getNetworkType())
                    .buildToTransaction(event.getTransaction().getHash());
            final String email = userSiteBalance.getUser().getEmail();
            final String id = Integer.toString(userSiteBalance.getUser().getId());
            bot.onBalance(
                    network,
                    email != null && !email.isEmpty() ? email : id,
                    toCurrency(event.getCurrency(), event.getAmount()),
                    txLink
            );
        } catch (Exception e) {
            log.error("Error on sending payment info to the bot.", e);
        }
    }


    @EventListener
    private void onBtcPaymentChanged(final ProductPaymentEvent event) {
        final String network = networkName.getOrDefault(event.getNetworkType(), defaultNetwork);
        final Product product = event.getProduct();
        final String type = ProductStatistics.PRODUCT_TYPES.get(product.getContractType());
        final String txLink = explorerProvider.getOrStub(event.getNetworkType())
                .buildToTransaction(event.getTransactionOutput().getParentTransaction());
        bot.onBtcPayment(
                network,
                type,
                product.getId(),
                toCurrency(event.getCurrency(), event.getAmount()),
                txLink
        );
    }


    @EventListener
    private void onNetworkStuck(final NetworkStuckEvent event) {
        final String network = networkName.getOrDefault(event.getNetworkType(), defaultNetwork);
        String lastBlock = formatToLocal(event.getReceivedTime());
        final String blockLink = explorerProvider
                .getOrStub(event.getNetworkType())
                .buildToBlock(event.getLastBlockNo());
        bot.sendToAllWithMarkdown("Network " + network + " *stuck!* Last block was at " + lastBlock + " [" + event.getLastBlockNo() + "](" + blockLink + ").");
    }

    @EventListener
    private void onPendingStuck(final PendingStuckEvent event) {
        final String network = networkName.getOrDefault(event.getNetworkType(), defaultNetwork);
        String lastBlock = formatToLocal(event.getReceivedTime());
        bot.sendToAllWithMarkdown("*No pending transactions* for the network " + network + "! Last pending was at " + lastBlock + ", count: " + event.getCount() + ".");
    }

    @EventListener
    private void onContractEvent(final ContractEventsEvent event) {
        final String network = networkName.getOrDefault(event.getNetworkType(), defaultNetwork);

        for (ContractEvent contractEvent : event.getEvents()) {
            if (contractEvent instanceof CreateAccountEvent) {
                final String accountRef = explorerProvider.getOrStub(event.getNetworkType())
                        .buildToAddress(((CreateAccountEvent) contractEvent).getCreated());

                bot.sendToAllWithMarkdown(network + ": account [" + ((CreateAccountEvent) contractEvent).getCreated() + "](" + accountRef + ") created.");
            }
        }
    }

    @SuppressWarnings("BigDecimalMethodWithoutRoundingCalled")
    private static String toCurrency(CryptoCurrency currency, BigInteger amount) {
        BigDecimal bdAmount = new BigDecimal(amount)
                .divide(BigDecimal.TEN.pow(currency.getDecimals()));

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(currency.getDecimals());
        df.setMinimumFractionDigits(0);

        return df.format(bdAmount) + " " + currency;
    }

    private String formatToLocal(LocalDateTime localDateTime) {
        return ZonedDateTime.ofInstant(
                localDateTime.toInstant(ZoneOffset.UTC),
                this.localZone
        )
                .format(dateFormatter);
    }

    private String getSymbol(Swaps2Order order, String token) {
        String[] symbols = order.getName().split("<>");

        if (symbols.length == 2) {
            if (token.equalsIgnoreCase(order.getBaseAddress())) {
                return symbols[0];
            }

            if (token.equalsIgnoreCase(order.getQuoteAddress())) {
                return symbols[1];
            }
        }

        return token;
    }
}
