package com.nikhil.productapi.service;

import com.nikhil.productapi.dto.ItemRequest;
import com.nikhil.productapi.dto.ItemResponse;
import com.nikhil.productapi.dto.PageResponse;
import com.nikhil.productapi.entity.Item;
import com.nikhil.productapi.entity.Product;
import com.nikhil.productapi.exception.ResourceNotFoundException;
import com.nikhil.productapi.repository.ItemRepository;
import com.nikhil.productapi.repository.ProductRepository;
import com.nikhil.productapi.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    private Product product;
    private Item item;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1).productName("Laptop").createdBy("admin").build();
        item = Item.builder().id(10).product(product).quantity(5).build();
    }

    @Test
    void getByProductId_returnsPage_whenProductExists() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Item> page = new PageImpl<>(List.of(item), pageable, 1);

        when(productRepository.existsById(1)).thenReturn(true);
        when(itemRepository.findByProductId(1, pageable)).thenReturn(page);

        PageResponse<ItemResponse> response = itemService.getByProductId(1, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getProductId()).isEqualTo(1);
        assertThat(response.getContent().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void getByProductId_throws_whenProductMissing() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> itemService.getByProductId(99, pageable))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(itemRepository);
    }

    @Test
    void addItem_savesItem_whenProductExists() {
        ItemRequest request = new ItemRequest();
        request.setQuantity(7);

        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> {
            Item i = inv.getArgument(0);
            i.setId(20);
            return i;
        });

        ItemResponse response = itemService.addItem(1, request);

        assertThat(response.getId()).isEqualTo(20);
        assertThat(response.getProductId()).isEqualTo(1);
        assertThat(response.getQuantity()).isEqualTo(7);
    }

    @Test
    void addItem_throws_whenProductMissing() {
        ItemRequest request = new ItemRequest();
        request.setQuantity(3);

        when(productRepository.findById(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> itemService.addItem(5, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(itemRepository, never()).save(any());
    }
}
