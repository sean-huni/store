package com.example.store.service.store.impl;

import com.example.store.dto.ProductDTO;
import com.example.store.mapper.ProductMapper;
import com.example.store.persistence.entity.Order;
import com.example.store.persistence.entity.Product;
import com.example.store.persistence.entity.ProductOrder;
import com.example.store.persistence.repo.ProductOrderRepo;
import com.example.store.persistence.repo.ProductRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@DisplayName("Unit Test - ProductServiceImpl")
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepo productRepo;

    @Mock
    private ProductOrderRepo productOrderRepo;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDTO testProductDTO;
    private List<Product> productList;
    private List<ProductDTO> productDTOList;
    private List<ProductOrder> productOrderList;
    private Set<Long> orderIds;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        // Create test product
        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setDescription("Test Product");
        testProduct.setSku(UUID.randomUUID());
        testProduct.setCreated(ZonedDateTime.now());
        testProduct.setUpdated(ZonedDateTime.now());
        testProduct.setOrders(new ArrayList<>());

        // Create test product DTO
        testProductDTO = new ProductDTO();
        testProductDTO.setId(1L);
        testProductDTO.setDescription("Test Product");
        testProductDTO.setSku(testProduct.getSku());
        testProductDTO.setCreated(testProduct.getCreated());
        testProductDTO.setUpdated(testProduct.getUpdated());
        
        // Create order IDs
        orderIds = Set.of(101L, 102L);
        testProductDTO.setOrderIds(orderIds);

        // Create list of products
        productList = new ArrayList<>();
        productList.add(testProduct);

        // Create list of product DTOs
        productDTOList = new ArrayList<>();
        productDTOList.add(testProductDTO);

        // Create product orders
        productOrderList = new ArrayList<>();
        Order order1 = new Order();
        order1.setId(101L);
        
        Order order2 = new Order();
        order2.setId(102L);
        
        ProductOrder po1 = new ProductOrder();
        po1.setProduct(testProduct);
        po1.setOrder(order1);
        
        ProductOrder po2 = new ProductOrder();
        po2.setProduct(testProduct);
        po2.setOrder(order2);
        
        productOrderList.add(po1);
        productOrderList.add(po2);

        // Create pageable mock
        pageable = Pageable.ofSize(10);
    }

    @Nested
    @DisplayName("When finding product by ID")
    class WhenFindingProductById {

        @Test
        @DisplayName("Then return product with order IDs when found")
        void thenReturnProductWithOrderIdsWhenFound() {
            // Given
            when(productRepo.findById(anyLong())).thenReturn(Optional.of(testProduct));
            when(productOrderRepo.findProductOrdersByProduct_Id(anyLong())).thenReturn(productOrderList);
            when(productMapper.toProductDTO(any(Product.class))).thenReturn(testProductDTO);

            // When
            ProductDTO result = productService.findProductById(1L);

            // Then
            assertNotNull(result);
            assertEquals(testProductDTO.getId(), result.getId());
            assertEquals(testProductDTO.getDescription(), result.getDescription());
            assertEquals(testProductDTO.getSku(), result.getSku());
            verify(productRepo, times(1)).findById(1L);
            verify(productOrderRepo, times(1)).findProductOrdersByProduct_Id(1L);
            verify(productMapper, times(1)).toProductDTO(testProduct);
        }

        @Test
        @DisplayName("Then return null when not found")
        void thenReturnNullWhenNotFound() {
            // Given
            when(productRepo.findById(anyLong())).thenReturn(Optional.empty());

            // When
            ProductDTO result = productService.findProductById(999L);

            // Then
            assertNull(result);
            verify(productRepo, times(1)).findById(999L);
        }
    }

    @Nested
    @DisplayName("When creating a product")
    class WhenCreatingProduct {

        @Test
        @DisplayName("Then save and return the product")
        void thenSaveAndReturnProduct() {
            // Given
            when(productMapper.toProduct(any(ProductDTO.class))).thenReturn(testProduct);
            when(productRepo.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toProductDTO(any(Product.class))).thenReturn(testProductDTO);

            // When
            ProductDTO result = productService.createProduct(testProductDTO);

            // Then
            assertNotNull(result);
            assertEquals(testProductDTO, result);
            verify(productMapper, times(1)).toProduct(testProductDTO);
            verify(productRepo, times(1)).save(testProduct);
            verify(productMapper, times(1)).toProductDTO(testProduct);
        }
    }

    @Nested
    @DisplayName("When finding all products")
    class WhenFindingAllProducts {

        @Test
        @DisplayName("Then return list of product DTOs with order IDs")
        void thenReturnListOfProductDTOsWithOrderIds() {
            // Given
            Page<Product> productPage = new PageImpl<>(productList);
            when(productRepo.findAll(any(Pageable.class))).thenReturn(productPage);
            when(productMapper.toProductDTOList(productList)).thenReturn(productDTOList);
            when(productOrderRepo.findOrderIdsByProduct_Id(anyLong())).thenReturn(orderIds);

            // When
            List<ProductDTO> result = productService.findAllProducts(pageable);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(testProductDTO.getId(), result.get(0).getId());
            assertEquals(testProductDTO.getDescription(), result.get(0).getDescription());
            assertEquals(orderIds, result.get(0).getOrderIds());
            verify(productRepo, times(1)).findAll(pageable);
            verify(productMapper, times(1)).toProductDTOList(productList);
            verify(productOrderRepo, times(1)).findOrderIdsByProduct_Id(1L);
        }
    }

    @Nested
    @DisplayName("When using private helper methods")
    class WhenUsingPrivateHelperMethods {

        @Test
        @DisplayName("Then findOrderIds returns correct order IDs")
        void thenFindOrderIdsReturnsCorrectOrderIds() {
            // This test indirectly tests the private findOrderIds method through findProductById
            
            // Given
            when(productRepo.findById(anyLong())).thenReturn(Optional.of(testProduct));
            when(productOrderRepo.findProductOrdersByProduct_Id(anyLong())).thenReturn(productOrderList);
            when(productMapper.toProductDTO(any(Product.class))).thenReturn(testProductDTO);

            // When
            ProductDTO result = productService.findProductById(1L);

            // Then
            assertNotNull(result);
            assertNotNull(result.getOrderIds());
            verify(productOrderRepo, times(1)).findProductOrdersByProduct_Id(1L);
        }
    }
}