package com.example.store.controller;

import com.example.store.component.GlobalSearchProps;
import com.example.store.dto.ProductDTO;
import com.example.store.dto.SortEnumDTO;
import com.example.store.service.store.ProductService;
import com.example.store.util.PageableBuilder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
public class ProductController {
    private final PageableBuilder pageableBuilder;
    private final ProductService productService;
    private final GlobalSearchProps globalSearchProps;

    @GetMapping("{id}")
    public ProductDTO findProductById(@PathVariable("id") @Positive(message = "global.400.003") final Long id) {
        return productService.findProductById(id);
    }

    @GetMapping
    public List<ProductDTO> findProducts(@RequestParam(required = false) @Min(value = 0, message = "global.400.006") final Integer page,
                                         @RequestParam(required = false) @Min(value = 5, message = "global.400.005") final Integer limit,
                                         @RequestParam(required = false) final String sortBy,
                                         @RequestParam(required = false) final SortEnumDTO sortDir) {

        final Pageable pageable = pageableBuilder.buildPageable(page, limit, sortBy, sortDir, globalSearchProps.getLimit(),
                globalSearchProps.getSortField(),
                globalSearchProps.getDirection()
        );

        return productService.findAllProducts(pageable);
    }

    @PostMapping
    public ProductDTO createProduct(@Valid @RequestBody final ProductDTO productDTO) {
        return productService.createProduct(productDTO);
    }
}
