package com.example.store.controller;

import com.example.store.component.GlobalSearchProps;
import com.example.store.dto.ProductDTO;
import com.example.store.dto.SortEnumDTO;
import com.example.store.service.store.ProductService;
import com.example.store.util.PageableBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("ProductController - {Unit}")
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private PageableBuilder pageableBuilder;

    @Mock
    private ProductService productService;

    @Mock
    private GlobalSearchProps globalSearchProps;

    @InjectMocks
    private ProductController productController;

    private ProductDTO productDTO;
    private List<ProductDTO> productDTOList;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Create a sample ProductDTO
        productDTO = new ProductDTO();
        productDTO.setId(1L);
        productDTO.setDescription("Test Description");
        productDTO.setSku(UUID.randomUUID());
        Set<Long> orderIds = new HashSet<>();
        orderIds.add(101L);
        productDTO.setOrderIds(orderIds);

        // Create a list of ProductDTOs
        ProductDTO productDTO2 = new ProductDTO();
        productDTO2.setId(2L);
        productDTO2.setDescription("Test Description 2");
        productDTO2.setSku(UUID.randomUUID());
        Set<Long> orderIds2 = new HashSet<>();
        orderIds2.add(102L);
        productDTO2.setOrderIds(orderIds2);

        productDTOList = Arrays.asList(productDTO, productDTO2);

        // Create a sample Pageable
        pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "id"));

        // Set up default GlobalSearchProps values with lenient to avoid "unnecessary stubbing" errors
        lenient().when(globalSearchProps.getLimit()).thenReturn(10);
        lenient().when(globalSearchProps.getSortField()).thenReturn("id");
        lenient().when(globalSearchProps.getDirection()).thenReturn("asc");
    }

    @Nested
    @DisplayName("When finding product by ID")
    class WhenFindingProductById {

        @Test
        @DisplayName("Then return the product when it exists")
        void thenReturnTheProductWhenItExists() {
            // Given
            Long productId = 1L;
            when(productService.findProductById(productId)).thenReturn(productDTO);

            // When
            ProductDTO result = productController.findProductById(productId);

            // Then
            assertNotNull(result);
            assertEquals(productId, result.getId());
            assertEquals("Test Description", result.getDescription());
            assertNotNull(result.getSku());
            assertNotNull(result.getOrderIds());
            assertEquals(1, result.getOrderIds().size());
            verify(productService).findProductById(productId);
        }
    }

    @Nested
    @DisplayName("When finding products")
    class WhenFindingProducts {

        @Test
        @DisplayName("Then return all products with default pagination")
        void thenReturnAllProductsWithDefaultPagination() {
            // Given
            when(pageableBuilder.buildPageable(eq(null), eq(null), eq(null), eq(null), anyInt(), anyString(), anyString()))
                    .thenReturn(pageable);
            when(productService.findAllProducts(pageable)).thenReturn(productDTOList);

            // When
            List<ProductDTO> result = productController.findProducts(null, null, null, null);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals(2L, result.get(1).getId());
            verify(pageableBuilder).buildPageable(eq(null), eq(null), eq(null), eq(null), anyInt(), anyString(), anyString());
            verify(productService).findAllProducts(pageable);
        }

        @Test
        @DisplayName("Then return products with custom pagination")
        void thenReturnProductsWithCustomPagination() {
            // Given
            Integer page = 1;
            Integer limit = 5;
            String sortBy = "description";
            SortEnumDTO sortDir = SortEnumDTO.desc;
            Pageable customPageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "description"));

            when(pageableBuilder.buildPageable(eq(page), eq(limit), eq(sortBy), eq(sortDir), anyInt(), anyString(), anyString()))
                    .thenReturn(customPageable);
            when(productService.findAllProducts(customPageable)).thenReturn(productDTOList);

            // When
            List<ProductDTO> result = productController.findProducts(page, limit, sortBy, sortDir);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(pageableBuilder).buildPageable(eq(page), eq(limit), eq(sortBy), eq(sortDir), anyInt(), anyString(), anyString());
            verify(productService).findAllProducts(customPageable);
        }
    }

    @Nested
    @DisplayName("When creating a product")
    class WhenCreatingProduct {

        @Test
        @DisplayName("Then create and return the product")
        void thenCreateAndReturnTheProduct() {
            // Given
            ProductDTO inputDTO = new ProductDTO();
            inputDTO.setDescription("New Description");
            UUID sku = UUID.randomUUID();
            inputDTO.setSku(sku);

            ProductDTO savedDTO = new ProductDTO();
            savedDTO.setId(3L);
            savedDTO.setDescription("New Description");
            savedDTO.setSku(sku);

            when(productService.createProduct(inputDTO)).thenReturn(savedDTO);

            // When
            ProductDTO result = productController.createProduct(inputDTO);

            // Then
            assertNotNull(result);
            assertEquals(3L, result.getId());
            assertEquals("New Description", result.getDescription());
            assertEquals(sku, result.getSku());
            verify(productService).createProduct(inputDTO);
        }
    }
}