package io.lastwill.eventscan.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.math.BigInteger;

@Entity
@Table(name = "profile_usersitebalance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSiteBalance {
    @Id
    private int id;
    private BigInteger balance;
    @Column(name = "duc_address")
    private String ducAddress;
    @Column(name = "ducx_address")
    private String ducxAddress;
    @Column(name = "eth_address")
    private String ethAddress;
    private String memo;
    @ManyToOne
    @JoinColumn(name = "subsite_id", referencedColumnName = "id")
    private Site site;
    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
}
