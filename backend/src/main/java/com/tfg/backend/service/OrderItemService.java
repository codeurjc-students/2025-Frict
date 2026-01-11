package com.tfg.backend.service;

import com.tfg.backend.model.OrderItem;
import com.tfg.backend.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    public List<OrderItem> findUserCartItemsList(Long id) { return orderItemRepository.findByUserIdAndOrderIsNull(id); }

    public Page<OrderItem> findUserCartItemsPage(Long id, Pageable pageable) { return orderItemRepository.findByUserIdAndOrderIsNull(id, pageable); }

    public List<OrderItem> findProductUnitsInCart(Long id) { return orderItemRepository.findByProductIdAndOrderIsNull(id); }

    public void save(OrderItem item) { this.orderItemRepository.save(item); }

    public void unlinkItemsFromUser(Long id) { orderItemRepository.unlinkItemsFromUser(id);}
}