package com.tfg.backend.unit;

import com.tfg.backend.model.OrderItem;
import com.tfg.backend.repository.OrderItemRepository;
import com.tfg.backend.service.OrderItemService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceUTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    // findUserCartItemsList() method tests
    @Test
    void findUserCartItemsList_ShouldReturnList_WhenItemsExist() {
        Long userId = 1L;
        List<OrderItem> expectedList = Arrays.asList(new OrderItem(), new OrderItem());

        when(orderItemRepository.findByUserIdAndOrderIsNull(userId)).thenReturn(expectedList);

        List<OrderItem> result = orderItemService.findUserCartItemsList(userId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedList, result);
        verify(orderItemRepository, times(1)).findByUserIdAndOrderIsNull(userId);
    }

    @Test
    void findUserCartItemsList_ShouldReturnEmptyList_WhenNoItemsFound() {
        Long userId = 1L;
        when(orderItemRepository.findByUserIdAndOrderIsNull(userId)).thenReturn(Collections.emptyList());

        List<OrderItem> result = orderItemService.findUserCartItemsList(userId);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderItemRepository, times(1)).findByUserIdAndOrderIsNull(userId);
    }


    // findUserCartItemsPage() method tests
    @Test
    void findUserCartItemsPage_ShouldReturnPage_WhenItemsExist() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        List<OrderItem> items = Collections.singletonList(new OrderItem());
        Page<OrderItem> expectedPage = new PageImpl<>(items);

        when(orderItemRepository.findByUserIdAndOrderIsNull(userId, pageable)).thenReturn(expectedPage);

        Page<OrderItem> result = orderItemService.findUserCartItemsPage(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(expectedPage, result);
        verify(orderItemRepository, times(1)).findByUserIdAndOrderIsNull(userId, pageable);
    }

    @Test
    void findUserCartItemsPage_ShouldReturnEmptyPage_WhenNoItemsFound() {
        Long userId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<OrderItem> emptyPage = Page.empty();

        when(orderItemRepository.findByUserIdAndOrderIsNull(userId, pageable)).thenReturn(emptyPage);

        Page<OrderItem> result = orderItemService.findUserCartItemsPage(userId, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(orderItemRepository, times(1)).findByUserIdAndOrderIsNull(userId, pageable);
    }


    // findProductUnitsInCart() method tests
    @Test
    void findProductUnitsInCart_ShouldReturnList_WhenProductExistsInCarts() {
        Long productId = 100L;
        List<OrderItem> expectedList = Arrays.asList(new OrderItem(), new OrderItem());

        when(orderItemRepository.findByProductIdAndOrderIsNull(productId)).thenReturn(expectedList);

        List<OrderItem> result = orderItemService.findProductUnitsInCart(productId);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(expectedList, result);
        verify(orderItemRepository, times(1)).findByProductIdAndOrderIsNull(productId);
    }
}
