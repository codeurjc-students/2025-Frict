package com.tfg.backend.unit;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceUTest {

    @Mock
    private OrderRepository repository;

    @InjectMocks
    private OrderService orderService;

    // findById() method tests
    @Test
    void findById_ShouldReturnOrder_WhenIdExists() {
        Long id = 1L;
        Order order = new Order();
        when(repository.findById(id)).thenReturn(Optional.of(order));

        Optional<Order> result = orderService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(order, result.get());
        verify(repository, times(1)).findById(id);
    }

    @Test
    void findById_ShouldReturnEmpty_WhenIdDoesNotExist() {
        Long id = 1L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<Order> result = orderService.findById(id);

        assertTrue(result.isEmpty());
        verify(repository, times(1)).findById(id);
    }


    // findAll() method tests
    @Test
    void findAll_ShouldReturnList_WhenOrdersExist() {
        List<Order> orders = Arrays.asList(new Order(), new Order());
        when(repository.findAll()).thenReturn(orders);

        List<Order> result = orderService.findAll();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository, times(1)).findAll();
    }

    @Test
    void findAll_ShouldReturnEmptyList_WhenNoOrdersExist() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        List<Order> result = orderService.findAll();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findAll();
    }


    // findAllByUser() method tests
    @Test
    void findAllByUser_ShouldReturnPage_WhenOrdersExist() {
        User user = new User();
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(Collections.singletonList(new Order()));

        when(repository.findAllByUser(user, pageable)).thenReturn(page);

        Page<Order> result = orderService.findAllByUser(user, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(repository, times(1)).findAllByUser(user, pageable);
    }


    // save() method tests
    @Test
    void save_ShouldReturnSavedOrder() {
        Order order = new Order();
        when(repository.save(order)).thenReturn(order);

        Order result = orderService.save(order);

        assertEquals(order, result);
        verify(repository, times(1)).save(order);
    }


    // existsByIdAndUser() method tests
    @Test
    void existsByIdAndUser_ShouldReturnTrue_WhenExists() {
        Long orderId = 1L;
        User user = new User();
        when(repository.existsByIdAndUser(orderId, user)).thenReturn(true);

        boolean result = orderService.existsByIdAndUser(orderId, user);

        assertTrue(result);
        verify(repository, times(1)).existsByIdAndUser(orderId, user);
    }

    @Test
    void existsByIdAndUser_ShouldReturnFalse_WhenDoesNotExist() {
        Long orderId = 1L;
        User user = new User();
        when(repository.existsByIdAndUser(orderId, user)).thenReturn(false);

        boolean result = orderService.existsByIdAndUser(orderId, user);

        assertFalse(result);
        verify(repository, times(1)).existsByIdAndUser(orderId, user);
    }
}
