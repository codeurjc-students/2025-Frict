package com.tfg.backend.unit;

import com.tfg.backend.model.OrderItem;
import com.tfg.backend.repository.OrderItemRepository;
import com.tfg.backend.service.OrderItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderItemServiceUTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemService orderItemService;

    private OrderItem orderItem;

    @BeforeEach
    void setUp() {
        // Setup a real POJO for testing
        orderItem = new OrderItem();
        orderItem.setId(1L);
    }

    @Nested
    @DisplayName("Tests for findOrderItemHelper")
    class FindOrderItemHelperTests {

        @Test
        @DisplayName("Returns OrderItem successfully when it exists in the repository")
        void findOrderItemHelper_Success() {
            // Arrange
            when(orderItemRepository.findById(1L)).thenReturn(Optional.of(orderItem));

            // Act
            OrderItem result = orderItemService.findOrderItemHelper(1L);

            // Assert
            assertNotNull(result, "The returned OrderItem should not be null");
            assertEquals(1L, result.getId(), "The ID of the returned item should match the requested one");
            verify(orderItemRepository).findById(1L);
        }

        @Test
        @DisplayName("Throws 404 NOT_FOUND when OrderItem does not exist")
        void findOrderItemHelper_ThrowsNotFound_WhenMissing() {
            // Arrange
            when(orderItemRepository.findById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orderItemService.findOrderItemHelper(1L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode(), "Should throw 404 NOT_FOUND");
            assertEquals("Order item with ID 1 does not exist.", ex.getReason(), "Exception reason message must match exactly");
            verify(orderItemRepository).findById(1L);
        }
    }
}