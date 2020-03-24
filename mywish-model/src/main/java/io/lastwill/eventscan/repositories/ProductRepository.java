package io.lastwill.eventscan.repositories;

import io.lastwill.eventscan.model.*;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ProductRepository extends CrudRepository<Product, Integer> {
    @Transactional
    @Modifying
    @Query("update Product set balance = :balanceWei where id = :id")
    void updateBalance(@Param("id") int id, @Param("balanceWei") BigInteger balanceWei);

    @Query("select c from Product c where lower(c.ownerAddress) in :addresses")
    List<Product> findByAddressesList(@Param("addresses") Collection<String> addresses);

    @Query(name = "Product.productStatistics")
    List<ProductStatistics> getProductStatistics(int networkId);

    @Query("select c " +
            "from ProductCrowdsale c " +
            "where c.crowdsaleContract.address = :address " +
            "   and c.tokenContract.address = :tokenAddress" +
            "   and c.network.type = :network")
    List<ProductCrowdsale> findCrowdsaleByAddressAndTokenAddress(
            @Param("address") String contractAddress,
            @Param("tokenAddress") String tokenAddress,
            @Param("network") NetworkType networkType
    );


    @Query("select c from ProductInvestmentPool c " +
            "where c.network.type = :network " +
            "and c.tokenAddress in :addresses ")
    List<ProductInvestmentPool> findIPoolByTokenAddress(
            @Param("addresses") Collection<String> addresses,
            @Param("network") NetworkType networkType
    );


    List<Product> findAllByUserId(@Param("userId") Integer userId);

    @Query("select c from ProductToken c " +
            "where c.createdDate >= :from " +
            "and c.network.type = io.lastwill.eventscan.model.NetworkType.DUCATUSX_MAINNET")
    List<ProductToken> findEthTokensFromDate(@Param("from") LocalDateTime from);

    @Query("select c from ProductCrowdsale c " +
            "where c.createdDate >= :from " +
            "and c.network.type = io.lastwill.eventscan.model.NetworkType.DUCATUSX_MAINNET")
    List<ProductCrowdsale> findEthIcoFromDate(@Param("from") LocalDateTime from);


//    @Query("select c from ProductTokenProtector c where lower(c.owner) in :owners")
//    List<ProductTokenProtector> findProtectorsByOwner(@Param("owners") Set<String> owners);
}
