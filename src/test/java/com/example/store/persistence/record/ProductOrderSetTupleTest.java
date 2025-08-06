package com.example.store.persistence.record;

import com.example.store.persistence.entity.ProductOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
@DisplayName("ProductOrderSetTuple Record Tests")
class ProductOrderSetTupleTest {

    @Test
    @DisplayName("Should create ProductOrderSetTuple with constructor")
    void shouldCreateProductOrderSetTupleWithConstructor() {
        // Given
        ProductOrder productOrder = new ProductOrder();
        Set<Long> orderIds = new HashSet<>();
        orderIds.add(1L);
        orderIds.add(2L);
        
        // When
        ProductOrderSetTuple tuple = new ProductOrderSetTuple(productOrder, orderIds);
        
        // Then
        assertNotNull(tuple);
        assertEquals(productOrder, tuple.productOrder());
        assertEquals(orderIds, tuple.orderIds());
    }
    
    @Test
    @DisplayName("Should create ProductOrderSetTuple with null values")
    void shouldCreateProductOrderSetTupleWithNullValues() {
        // When
        ProductOrderSetTuple tuple = new ProductOrderSetTuple(null, null);
        
        // Then
        assertNotNull(tuple);
        assertNull(tuple.productOrder());
        assertNull(tuple.orderIds());
    }
    
    @Test
    @DisplayName("Should have correct equals and hashCode behavior")
    void shouldHaveCorrectEqualsAndHashCodeBehavior() {
        // Given
        ProductOrder productOrder1 = new ProductOrder();
        ProductOrder productOrder2 = new ProductOrder();
        
        Set<Long> orderIds1 = new HashSet<>();
        orderIds1.add(1L);
        
        Set<Long> orderIds2 = new HashSet<>();
        orderIds2.add(2L);
        
        // When
        ProductOrderSetTuple tuple1 = new ProductOrderSetTuple(productOrder1, orderIds1);
        ProductOrderSetTuple tuple2 = new ProductOrderSetTuple(productOrder1, orderIds1);
        ProductOrderSetTuple tuple3 = new ProductOrderSetTuple(productOrder2, orderIds1);
        ProductOrderSetTuple tuple4 = new ProductOrderSetTuple(productOrder1, orderIds2);
        
        // Then
        assertEquals(tuple1, tuple2);
        assertEquals(tuple1.hashCode(), tuple2.hashCode());
        
        assertNotEquals(tuple1, tuple3);
        assertNotEquals(tuple1, tuple4);
        
        assertNotEquals(tuple1, null);
        assertNotEquals(tuple1, new Object());
    }
    
    @Test
    @DisplayName("Should have correct toString representation")
    void shouldHaveCorrectToStringRepresentation() {
        // Given
        ProductOrder productOrder = new ProductOrder();
        Set<Long> orderIds = new HashSet<>();
        orderIds.add(1L);
        
        // When
        ProductOrderSetTuple tuple = new ProductOrderSetTuple(productOrder, orderIds);
        String toStringResult = tuple.toString();
        
        // Then
        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("productOrder"));
        assertTrue(toStringResult.contains("orderIds"));
    }
}