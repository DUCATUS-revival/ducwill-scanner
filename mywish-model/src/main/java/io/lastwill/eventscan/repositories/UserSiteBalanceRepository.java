package io.lastwill.eventscan.repositories;

import io.lastwill.eventscan.model.User;
import io.lastwill.eventscan.model.UserSiteBalance;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface UserSiteBalanceRepository extends CrudRepository<UserSiteBalance, Integer> {

    @Query("select c from UserSiteBalance c where c.ducAddress in :addresses")
    List<UserSiteBalance> findByDucAddressesList(@Param("addresses") Collection<String> addresses);

    @Query("select c from UserSiteBalance c where c.ducxAddress in :addresses")
    List<UserSiteBalance> findByDucxAddressesList(@Param("addresses") Collection<String> addresses);

    @Query("select c from UserSiteBalance c where c.ethAddress in :addresses")
    List<UserSiteBalance> findByEthAddressesList(@Param("addresses") Collection<String> addresses);

    UserSiteBalance findByEthAddress(String ethAddress);

    List<UserSiteBalance> findAllByUser(@Param("user") User user);
}
