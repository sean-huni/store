package com.example.store.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class GlobalSearchProps {
    @Value("${global.search.sort-field:id}")
    private String sortField;

    @Value("${global.search.limit:20}")
    private int limit;

    @Value("${global.search.sort-direction:asc}")
    private String direction;
}