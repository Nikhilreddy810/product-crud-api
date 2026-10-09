package com.nikhil.productapi.service;

import com.nikhil.productapi.dto.PageResponse;
import com.nikhil.productapi.dto.ProductRequest;
import com.nikhil.productapi.dto.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    PageResponse<ProductResponse> getAll(Pageable pageable);
    ProductResponse getById(Integer id);
    ProductResponse create(ProductRequest request, String username);
    ProductResponse update(Integer id, ProductRequest request, String username);
    void delete(Integer id, String username);
}
