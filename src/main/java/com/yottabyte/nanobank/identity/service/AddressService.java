package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.dto.StartOnboardingRequest;
import com.yottabyte.nanobank.identity.entity.Address;
import com.yottabyte.nanobank.identity.entity.Customer;
import com.yottabyte.nanobank.identity.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    @Transactional
    public Address createForCustomer(
            Customer customer,
            StartOnboardingRequest request
    ) {

        Address address = Address.builder()
                .customer(customer)
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .province(request.province())
                .postalCode(request.postalCode())
                .country("South Africa")
                .build();

        return addressRepository.save(address);
    }
}