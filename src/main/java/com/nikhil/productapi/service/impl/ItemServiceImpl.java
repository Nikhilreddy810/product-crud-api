package com.nikhil.productapi.service.impl;

import com.nikhil.productapi.dto.ItemRequest;
import com.nikhil.productapi.dto.ItemResponse;
import com.nikhil.productapi.dto.PageResponse;
import com.nikhil.productapi.entity.Item;
import com.nikhil.productapi.entity.Product;
import com.nikhil.productapi.exception.ResourceNotFoundException;
import com.nikhil.productapi.repository.ItemRepository;
import com.nikhil.productapi.repository.ProductRepository;
import com.nikhil.productapi.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;

    @Override
    public PageResponse<ItemResponse> getByProductId(Integer productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        Page<ItemResponse> page = itemRepository.findByProductId(productId, pageable).map(this::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public ItemResponse addItem(Integer productId, ItemRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        Item item = Item.builder()
                .product(product)
                .quantity(request.getQuantity())
                .build();

        return toResponse(itemRepository.save(item));
    }

    private ItemResponse toResponse(Item item) {
        return ItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .quantity(item.getQuantity())
                .build();
    }
}
