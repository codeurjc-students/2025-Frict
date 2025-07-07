package com.tfg.ProjectBackend.repository;

import com.tfg.ProjectBackend.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
