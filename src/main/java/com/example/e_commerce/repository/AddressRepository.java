package com.example.e_commerce.repository;

import com.example.e_commerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByIdAndMemberId(Long addressId, Long memberId);
}
