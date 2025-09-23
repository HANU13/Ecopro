package com.litemax.ECoPro.repository.auth;

import com.litemax.ECoPro.entity.auth.Address;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserId(Long userId);
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long UserId);
    Optional<Address> findByIdAndUserId(long addressId,long userId);
    long countByUserId(long userId);
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId")
    void updateDefaultToFalseForUser(@Param("userId") long userId);
    List<Address> findByUserIdOrderByCreatedAtAsc(long userId);
}