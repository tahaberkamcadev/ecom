package com.tahaberkamcadev.e_com.user_service.repository;

import com.tahaberkamcadev.e_com.user_service.model.Address;
import com.tahaberkamcadev.e_com.user_service.model.AddressType;
import com.tahaberkamcadev.e_com.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByUserAndIsActiveTrue(User user);
    
    List<Address> findByUserAndAddressTypeAndIsActiveTrue(User user, AddressType addressType);
    
    Optional<Address> findByUserAndIsDefaultTrueAndAddressType(User user, AddressType addressType);
    
    Optional<Address> findByUserAndIsDefaultTrueAndIsActiveTrue(User user);
    
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user = :user AND a.isActive = true")
    long countActiveAddressesByUser(@Param("user") User user);
    
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user = :user AND a.addressType = :addressType")
    void clearDefaultAddressesByUserAndType(@Param("user") User user, @Param("addressType") AddressType addressType);
    
    @Modifying
    @Query("UPDATE Address a SET a.isActive = false WHERE a.user = :user")
    void softDeleteAllByUser(@Param("user") User user);
}