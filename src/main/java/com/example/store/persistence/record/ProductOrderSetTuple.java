package com.example.store.persistence.record;

import com.example.store.persistence.entity.ProductOrder;

import java.util.Set;

public record ProductOrderSetTuple(ProductOrder productOrder, Set<Long> orderIds) {
}
