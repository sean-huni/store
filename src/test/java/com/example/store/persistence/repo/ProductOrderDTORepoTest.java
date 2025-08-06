package com.example.store.persistence.repo;

import com.example.store.persistence.entity.Customer;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.ProductOrder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import test.config.TestConfig;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("repo")
@ActiveProfiles("db")
@DataJpaTest
@Import(TestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("DB-Integration-Test - Given ProductOrderRepo")
class ProductOrderDTORepoTest {
    @Autowired
    private ProductOrderRepo productOrderRepo;
    @Autowired
    private ProductRepo productRepo;
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private CustomerRepo customerRepo;
    
    private Product testProduct;
    private Order order1;
    private Order order2;
    
    @BeforeEach
    void setUp() {
        // Create a new test product
        testProduct = new Product();
        testProduct.setDescription("Test Product " + UUID.randomUUID());
        testProduct.setSku(UUID.randomUUID()); // Set SKU on Product instead of Order
        testProduct.setCreated(ZonedDateTime.now());
        testProduct.setUpdated(ZonedDateTime.now());
        testProduct = productRepo.save(testProduct);
        
        // Use an existing customer or create a new one with a random ID
        Customer customer = customerRepo.findById(1L).orElseGet(() -> {
            Customer newCustomer = new Customer();
            newCustomer.setName("Test Customer " + UUID.randomUUID());
            newCustomer.setCreated(ZonedDateTime.now());
            newCustomer.setUpdated(ZonedDateTime.now());
            return customerRepo.save(newCustomer);
        });
        
        // Use existing orders or create new ones
        order1 = orderRepo.findById(1L).orElseGet(() -> {
            Order newOrder = new Order();
            newOrder.setDescription("Test Order " + UUID.randomUUID());
            // SKU is now on Product, not Order
            newOrder.setCustomer(customer);
            newOrder.setCreated(ZonedDateTime.now());
            newOrder.setUpdated(ZonedDateTime.now());
            return orderRepo.save(newOrder);
        });
        
        order2 = orderRepo.findById(2L).orElseGet(() -> {
            Order newOrder = new Order();
            newOrder.setDescription("Test Order " + UUID.randomUUID());
            // SKU is now on Product, not Order
            newOrder.setCustomer(customer);
            newOrder.setCreated(ZonedDateTime.now());
            newOrder.setUpdated(ZonedDateTime.now());
            return orderRepo.save(newOrder);
        });
        
        // Create product orders
        ProductOrder productOrder1 = new ProductOrder();
        productOrder1.setProduct(testProduct);
        productOrder1.setOrder(order1);
        productOrder1.setQuantity(2);
        productOrder1.setPrice(BigDecimal.valueOf(19.99));
        productOrder1.setCreated(ZonedDateTime.now());
        productOrder1.setUpdated(ZonedDateTime.now());
        productOrderRepo.save(productOrder1);
        
        ProductOrder productOrder2 = new ProductOrder();
        productOrder2.setProduct(testProduct);
        productOrder2.setOrder(order2);
        productOrder2.setQuantity(1);
        productOrder2.setPrice(BigDecimal.valueOf(29.99));
        productOrder2.setCreated(ZonedDateTime.now());
        productOrder2.setUpdated(ZonedDateTime.now());
        productOrderRepo.save(productOrder2);
    }

    @Nested
    @DisplayName("When finding Product Orders")
    class WhenFindingProductOrdersDO {

        @Test
        @DisplayName("Then return valid Products with associated Orders")
        void returnProductOrders() {
            final var productOrders = productOrderRepo.findProductOrdersByProduct_Id(testProduct.getId());

            assertNotNull(productOrders);
            assertEquals(2, productOrders.size());
            assertNotNull(productOrders.get(0).getOrder());
        }

        @Test
        @DisplayName("Then return OrderIDs Set")
        void returnOrderIdSet() {
            final var orderIds = productOrderRepo.findOrderIdsByProduct_Id(testProduct.getId());

            assertNotNull(orderIds);
            assertEquals(2, orderIds.size());
            assertTrue(orderIds.contains(order1.getId()));
            assertTrue(orderIds.contains(order2.getId()));
        }
    }
}