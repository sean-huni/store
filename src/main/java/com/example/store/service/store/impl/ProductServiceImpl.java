package com.example.store.service.store.impl;

import com.example.store.dto.ProductDTO;
import com.example.store.mapper.ProductMapper;
import com.example.store.persistence.repo.ProductOrderRepo;
import com.example.store.persistence.repo.ProductRepo;
import com.example.store.service.store.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepo productRepo;
    private final ProductOrderRepo productOrderRepo;
    private final ProductMapper productMapper;

    /**
     * Retrieves a product by its unique identifier and maps it to a ProductDTO.
     * If the product exists, also retrieves the IDs of orders containing this product.
     *
     * @param id the unique identifier of the product to retrieve
     * @return a ProductDTO containing the product details and associated order IDs, or null if no product with the given ID exists
     */
    @Override
    public ProductDTO findProductById(final Long id) {
        final var product = productRepo.findById(id).orElse(null);
        if (isNull(product)) {
            return null;
        }

        final var productOrders = findOrderIds(id);
        final var productDTO = productMapper.toProductDTO(product);

        productDTO.setOrderIds(productOrders);
        return productDTO;
    }

    /**
     * Fetches and returns a set of order IDs associated with a specific product ID.
     *
     * @param id the unique identifier of the product for which to retrieve associated order IDs
     * @return a set containing the unique identifiers of the orders associated with the specified product
     */
    private Set<Long> findOrderIds(final Long id) {
        final var productOrderEntity = productOrderRepo.findProductOrdersByProduct_Id(id);

        return productOrderEntity.stream()
                .map(p -> p.getOrder().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public ProductDTO createProduct(final ProductDTO productDTO) {
        final var product = productMapper.toProduct(productDTO);
        // Set created and updated times
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        product.setCreated(now);
        product.setUpdated(now);
        final var savedProduct = productRepo.save(product);
        return productMapper.toProductDTO(savedProduct);
    }

    /**
     * Retrieves a paginated list of all products and maps them to ProductDTOs.
     * Each ProductDTO will also include a set of associated order IDs.
     *
     * @param pageable the pagination information, including page number and size
     * @return a list of ProductDTO objects containing product details and their associated order IDs
     */
    @Override
    public List<ProductDTO> findAllProducts(final Pageable pageable) {
        final var productsPage = productRepo.findAll(pageable);

        final var productsDto = productMapper.toProductDTOList(productsPage.getContent());
        productsDto.forEach(p -> p.setOrderIds(productOrderRepo.findOrderIdsByProduct_Id(p.getId())));
        return productsDto;
    }
}
