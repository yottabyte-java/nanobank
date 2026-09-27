package com.yottabyte.nanobank.identity.service;

import com.yottabyte.nanobank.identity.entity.Customer;
import com.yottabyte.nanobank.identity.entity.OnboardingApplication;
import com.yottabyte.nanobank.identity.exception.CustomerAlreadyExistsException;
import com.yottabyte.nanobank.identity.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void shouldCreateCustomerWhenIdentityDoesNotAlreadyExist() {

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .firstName("Wayne")
                        .lastName("Mokoena")
                        .email("wayne@example.com")
                        .mobileNumber("0821234567")
                        .nationalId("9805105000088")
                        .build();

        when(customerRepository.existsByEmailIgnoreCase(application.getEmail()))
                .thenReturn(false);

        when(customerRepository.existsByMobileNumber(application.getMobileNumber()))
                .thenReturn(false);

        when(customerRepository.existsByNationalId(application.getNationalId()))
                .thenReturn(false);

        Customer savedCustomer =
                Customer.builder()
                        .firstName("Wayne")
                        .lastName("Mokoena")
                        .email("wayne@example.com")
                        .mobileNumber("0821234567")
                        .nationalId("9805105000088")
                        .build();

        when(customerRepository.save(any(Customer.class)))
                .thenReturn(savedCustomer);

        Customer result =
                customerService.createFromOnboarding(application);

        assertNotNull(result);
        assertEquals("Wayne", result.getFirstName());
        assertEquals("Mokoena", result.getLastName());
        assertEquals("wayne@example.com", result.getEmail());

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectWhenEmailAlreadyExists() {

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .firstName("Wayne")
                        .lastName("Mokoena")
                        .email("wayne@example.com")
                        .mobileNumber("0821234567")
                        .nationalId("9805105000088")
                        .build();

        when(customerRepository.existsByEmailIgnoreCase(application.getEmail()))
                .thenReturn(true);

        assertThrows(
                CustomerAlreadyExistsException.class,
                () -> customerService.createFromOnboarding(application)
        );

        verify(customerRepository, never())
                .save(any(Customer.class));
    }

    @Test
    void shouldRejectWhenMobileNumberAlreadyExists() {

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .firstName("Wayne")
                        .lastName("Mokoena")
                        .email("wayne@example.com")
                        .mobileNumber("0821234567")
                        .nationalId("9805105000088")
                        .build();

        when(customerRepository.existsByEmailIgnoreCase(application.getEmail()))
                .thenReturn(false);

        when(customerRepository.existsByMobileNumber(application.getMobileNumber()))
                .thenReturn(true);

        assertThrows(
                CustomerAlreadyExistsException.class,
                () -> customerService.createFromOnboarding(application)
        );

        verify(customerRepository, never())
                .save(any(Customer.class));
    }

    @Test
    void shouldRejectWhenNationalIdAlreadyExists() {

        OnboardingApplication application =
                OnboardingApplication.builder()
                        .firstName("Wayne")
                        .lastName("Mokoena")
                        .email("wayne@example.com")
                        .mobileNumber("0821234567")
                        .nationalId("9805105000088")
                        .build();

        when(customerRepository.existsByEmailIgnoreCase(application.getEmail()))
                .thenReturn(false);

        when(customerRepository.existsByMobileNumber(application.getMobileNumber()))
                .thenReturn(false);

        when(customerRepository.existsByNationalId(application.getNationalId()))
                .thenReturn(true);

        assertThrows(
                CustomerAlreadyExistsException.class,
                () -> customerService.createFromOnboarding(application)
        );

        verify(customerRepository, never())
                .save(any(Customer.class));
    }
}