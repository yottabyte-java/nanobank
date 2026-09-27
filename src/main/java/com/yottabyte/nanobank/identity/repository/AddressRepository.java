package com.yottabyte.nanobank.identity.repository;

import com.yottabyte.nanobank.identity.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AddressRepository extends JpaRepository<Address, UUID> {
}