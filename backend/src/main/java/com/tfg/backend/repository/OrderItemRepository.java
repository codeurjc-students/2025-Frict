package com.tfg.backend.repository;

import com.tfg.backend.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    //Retrieves cart items for the specified user id
    Page<OrderItem> findByUserIdAndOrderIsNull(Long userId, Pageable pageable);
}
