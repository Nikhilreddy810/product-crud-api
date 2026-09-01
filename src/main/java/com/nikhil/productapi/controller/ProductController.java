package com.nikhil.productapi.controller;

import com.nikhil.productapi.dto.*;
import com.nikhil.productapi.service.ItemService;
import com.nikhil.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService productService;
    private final ItemService itemService;

    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productService.getAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request,
                                                   Authentication authentication) {
        ProductResponse response = productService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(@PathVariable Integer id,
                                                   @Valid @RequestBody ProductRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(productService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        productService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/items")
    public ResponseEntity<PageResponse<ItemResponse>> getItems(
            @PathVariable Integer id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(itemService.getByProductId(id, pageable));
    }

    @PostMapping("/{id}/items")
    public ResponseEntity<ItemResponse> addItem(@PathVariable Integer id,
                                                 @Valid @RequestBody ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(itemService.addItem(id, request));
    }
}
