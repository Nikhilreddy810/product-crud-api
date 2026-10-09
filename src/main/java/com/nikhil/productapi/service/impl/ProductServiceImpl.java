package com.nikhil.productapi.service.impl;

import com.nikhil.productapi.dto.PageResponse;
import com.nikhil.productapi.dto.ProductRequest;
import com.nikhil.productapi.dto.ProductResponse;
import com.nikhil.productapi.entity.Product;
import com.nikhil.productapi.exception.ResourceNotFoundException;
import com.nikhil.productapi.repository.ProductRepository;
import com.nikhil.productapi.service.AuditLogService;
import com.nikhil.productapi.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final AuditLogService auditLogService;

    @Override
    public PageResponse<ProductResponse> getAll(Pageable pageable) {
        Page<ProductResponse> page = productRepository.findAll(pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public ProductResponse getById(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request, String username) {
        Product product = Product.builder()
                .productName(request.getProductName())
                .createdBy(username)
                .build();
        Product saved = productRepository.save(product);
        auditLogService.logProductChange(saved.getId(), "CREATE", username);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Integer id, ProductRequest request, String username) {
        Product product = findOrThrow(id);
        product.setProductName(request.getProductName());
        product.setModifiedBy(username);
        Product saved = productRepository.save(product);
        auditLogService.logProductChange(saved.getId(), "UPDATE", username);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Integer id, String username) {
        Product product = findOrThrow(id);
        productRepository.delete(product);
        auditLogService.logProductChange(id, "DELETE", username);
    }

    private Product findOrThrow(Integer id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .productName(p.getProductName())
                .createdBy(p.getCreatedBy())
                .createdOn(p.getCreatedOn())
                .modifiedBy(p.getModifiedBy())
                .modifiedOn(p.getModifiedOn())
                .build();
    }
}
