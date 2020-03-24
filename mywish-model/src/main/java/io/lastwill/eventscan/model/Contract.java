package io.lastwill.eventscan.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contracts_ducxcontract")
@Getter
public class Contract {
    // TODO remove setter
    @Setter
    @Id
    private Integer id;
    @Setter
    private String address;
    private String txHash;
    // TODO: add convertors
//    @Column(name = "activeTo", nullable = false)
//    private OffsetDateTime activeUntil;
//    @Column(nullable = false)
//    private int checkInterval;
    @ManyToOne(optional = false)
    @JoinColumn(name = "contract_id")
    private Product product;
}
