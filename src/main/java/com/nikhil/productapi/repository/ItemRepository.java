package com.nikhil.productapi.repository;

import com.nikhil.productapi.entity.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, Integer> {
    Page<Item> findByProductId(Integer productId, Pageable pageable);
}
