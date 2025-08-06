package com.example.store.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Data
public class CustomerSearchProps {
    @Value("${customer.search.sort-field:name}")
    private String sortField;

    @Value("${customer.search.limit:10}")
    private int limit;

    @Value("${customer.search.sort-direction:asc}")
    private String direction;
}
