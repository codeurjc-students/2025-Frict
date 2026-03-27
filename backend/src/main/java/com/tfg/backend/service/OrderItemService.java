package com.tfg.backend.service;

import com.tfg.backend.model.OrderItem;
import com.tfg.backend.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {

    @Autowired
    private OrderItemRepository orderItemRepository;

    public Optional<OrderItem> findById(Long id) { return this.orderItemRepository.findById(id); }

    public List<OrderItem> findByProductIdAndOrderIsNotNull(Long id){ return orderItemRepository.findByProductIdAndOrderIsNotNull(id); }

    public List<OrderItem> findUserCartItemsList(Long id) { return orderItemRepository.findByUserIdAndOrderIsNull(id); }

    public Page<OrderItem> findUserCartItemsPage(Long id, Pageable pageable) { return orderItemRepository.findByUserIdAndOrderIsNull(id, pageable); }

    public List<OrderItem> findProductUnitsInCart(Long id) { return orderItemRepository.findByProductIdAndOrderIsNull(id); }

    public Optional<OrderItem> findUserCartItemByProductId(Long productId, Long userId){ return this.orderItemRepository.findByProductIdAndUserIdAndOrderIsNull(productId, userId); }

    public void save(OrderItem item) { this.orderItemRepository.save(item); }

    public List<OrderItem> saveAll(List<OrderItem> l) { return this.orderItemRepository.saveAll(l); }

    public void delete(OrderItem i) { this.orderItemRepository.delete(i); }

    public OrderItem findOrderItemHelper(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item with ID " + id + " does not exist."));
    }
}