package com.example.store.service.store;

import com.example.store.dto.ProductDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductDTO findProductById(Long id);

    ProductDTO createProduct(ProductDTO productDTO);

    List<ProductDTO> findAllProducts(final Pageable pageable);
}
