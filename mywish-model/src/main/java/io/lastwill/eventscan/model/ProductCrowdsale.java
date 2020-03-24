package io.lastwill.eventscan.model;

import lombok.Getter;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.Instant;
import java.time.ZonedDateTime;

@Entity
@Table(name = "contracts_contractdetailsico")
@PrimaryKeyJoinColumn(name = "contract_id")
@DiscriminatorValue("4")
@Getter
public class ProductCrowdsale extends ProductTokenCommon implements ProductSingleCheck {
    @Column(name = "token_short_name")
    private String symbol;

    @Column(name = "stop_date")
    private int finishTimestamp;
    @ManyToOne
    @JoinColumn(name = "ducx_contract_crowdsale_id")
    private Contract crowdsaleContract;
    @ManyToOne
    @JoinColumn(name = "ducx_contract_token_id")
    private Contract tokenContract;

    @Override
    public int getContractType() {
        return 4;
    }

    @Override
    public BigInteger getCheckGasLimit() {
        // really it is finish gas price
        return BigInteger.valueOf(200000);
    }

    @Override
    public Instant getCheckDate() {
        return Instant.ofEpochSecond(finishTimestamp);
    }
}
