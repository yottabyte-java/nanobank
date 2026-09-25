package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.Customer;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.enums.CustomerStatus;
import com.yottabyte.nanobank.identity.exception.CustomerAlreadyExistsException;
import com.yottabyte.nanobank.identity.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional
    public Customer createFromOnboarding(
            OnboardingApplication application
    ) {

        if (customerRepository.existsByEmailIgnoreCase(application.getEmail())
                || customerRepository.existsByMobileNumber(application.getMobileNumber())
                || customerRepository.existsByNationalId(application.getNationalId())) {

            throw new CustomerAlreadyExistsException(
                    "Customer already exists for supplied identity information."
            );
        }

        Customer customer = Customer.builder()
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .dateOfBirth(application.getDateOfBirth())
                .email(application.getEmail())
                .mobileNumber(application.getMobileNumber())
                .nationalId(application.getNationalId())
                .status(CustomerStatus.ACTIVE)
                .build();

        Customer savedCustomer = customerRepository.save(customer);

        application.setCustomerId(savedCustomer.getId());

        return savedCustomer;
    }
}
