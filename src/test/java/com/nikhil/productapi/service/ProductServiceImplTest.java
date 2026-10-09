package com.nikhil.productapi.service;

import com.nikhil.productapi.dto.ProductRequest;
import com.nikhil.productapi.dto.ProductResponse;
import com.nikhil.productapi.entity.Product;
import com.nikhil.productapi.exception.ResourceNotFoundException;
import com.nikhil.productapi.repository.ProductRepository;
import com.nikhil.productapi.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1)
                .productName("Laptop")
                .createdBy("admin")
                .build();
    }

    @Test
    void getById_returnsProduct_whenExists() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getById(1);

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getProductName()).isEqualTo("Laptop");
    }

    @Test
    void getById_throws_whenNotFound() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void create_savesProduct_andTriggersAuditLog() {
        ProductRequest request = new ProductRequest();
        request.setProductName("Monitor");

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(2);
            return p;
        });

        ProductResponse response = productService.create(request, "admin");

        assertThat(response.getId()).isEqualTo(2);
        assertThat(response.getProductName()).isEqualTo("Monitor");
        assertThat(response.getCreatedBy()).isEqualTo("admin");
        verify(productRepository, times(1)).save(any(Product.class));
        verify(auditLogService, times(1)).logProductChange(2, "CREATE", "admin");
    }

    @Test
    void update_throws_whenProductMissing() {
        when(productRepository.findById(5)).thenReturn(Optional.empty());
        ProductRequest request = new ProductRequest();
        request.setProductName("X");

        assertThatThrownBy(() -> productService.update(5, request, "admin"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).save(any());
        verifyNoInteractions(auditLogService);
    }

    @Test
    void delete_removesProduct_andTriggersAuditLog() {
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        productService.delete(1, "admin");

        verify(productRepository, times(1)).delete(product);
        verify(auditLogService, times(1)).logProductChange(1, "DELETE", "admin");
    }
}
