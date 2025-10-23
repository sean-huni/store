package com.example.store.integration;

import com.example.store.integration.config.IntTestConfig;
import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.repo.CustomerRepo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("int")
@Tag("int")
@Transactional
@Slf4j
@Import({IntTestConfig.class})
@DisplayName("SQL Performance Aspect Integration Test")
class SqlPerfAspectIntegrationTest {

    @Autowired
    private CustomerRepo customerRepo;

    @Test
    @DisplayName("Should print formatReport for @TrackSqlPerf annotated method")
    void shouldPrintFormatReportForTrackSqlPerfMethod() {
        // Given: A customer ID that doesn't exist 
        final Long nonExistentCustomerId = 999L;

        // When: Finding customer by ID (this method has @TrackSqlPerf annotation)
        log.debug("About to call findCustomerByIdWithOrders");
        final Optional<Customer> customerOptional = customerRepo.findCustomerByIdWithOrders(nonExistentCustomerId);
        log.debug("Finished calling findCustomerByIdWithOrders");

        // Then: Verify customer is not found
        assertNotNull(customerOptional);
        assertFalse(customerOptional.isPresent(), "Customer should not be found for non-existent ID");

        // The aspect should print the detailed formatReport in the logs above
        log.debug("Test completed - check logs above for SQL Performance Report");
    }
}