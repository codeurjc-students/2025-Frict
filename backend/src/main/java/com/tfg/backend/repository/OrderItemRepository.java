package com.tfg.backend.repository;

import com.tfg.backend.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    //Retrieve cart items for the specified user id
    List<OrderItem> findByUserIdAndOrderIsNull(Long userId); //For cart summary
    Page<OrderItem> findByUserIdAndOrderIsNull(Long userId, Pageable pageable); //For cart page visualization

    //Retrieves all the items of the same product that are in all users carts (stock check)
    List<OrderItem> findByProductIdAndOrderIsNull(Long productId);
}
