package com.nikhil.productapi.service;

import com.nikhil.productapi.dto.ItemRequest;
import com.nikhil.productapi.dto.ItemResponse;
import com.nikhil.productapi.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface ItemService {
    PageResponse<ItemResponse> getByProductId(Integer productId, Pageable pageable);
    ItemResponse addItem(Integer productId, ItemRequest request);
}
