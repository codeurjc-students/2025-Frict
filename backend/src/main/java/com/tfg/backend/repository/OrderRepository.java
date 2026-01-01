package com.tfg.backend.repository;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    boolean existsByIdAndUser(Long orderId, User user);

    Page<Order> findAllByUser(User user, Pageable pageable);
}
