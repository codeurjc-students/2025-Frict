package com.tfg.backend.repository;

import com.tfg.backend.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    //Retrieve cart items for the specified user id
    List<OrderItem> findByUserIdAndOrderIsNull(Long userId); //For cart summary
    Page<OrderItem> findByUserIdAndOrderIsNull(Long userId, Pageable pageable); //For cart page visualization

    //Retrieves all the items of the same product that are in all users carts (stock check)
    List<OrderItem> findByProductIdAndOrderIsNull(Long productId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE OrderItem o SET o.user = NULL WHERE o.user.id = :userId")
    void unlinkItemsFromUser(@Param("userId") Long userId);
}
