package com.tfg.backend.unit;

import com.tfg.backend.model.User;
import com.tfg.backend.service.*;
import com.tfg.backend.utils.StatDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatServiceUTest {

    @Mock private OrderService orderService;
    @Mock private UserService userService;
    @Mock private ShopService shopService;
    @Mock private TruckService truckService;

    @InjectMocks
    private StatService statService;

    private User loggedUser;

    @BeforeEach
    void setUp() {
        // Setup a real POJO for the user context
        loggedUser = new User();
        loggedUser.setId(1L);
    }

    @Test
    @DisplayName("getOrdersStatsByRole fetches logged user and delegates to OrderService")
    void getOrdersStatsByRole_DelegatesProperly() {
        List<StatDTO> mockStats = List.of(new StatDTO("Realizados", 10L));

        when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
        when(orderService.getOrdersStatistics(loggedUser)).thenReturn(mockStats);

        List<StatDTO> result = statService.getOrdersStatsByRole();

        assertEquals(1, result.size());
        assertEquals("Realizados", result.getFirst().label());
        assertEquals(10L, result.getFirst().value());

        verify(userService).findLoggedUserHelper();
        verify(orderService).getOrdersStatistics(loggedUser);
    }

    @Test
    @DisplayName("getShopsStatsByRole fetches logged user and delegates to ShopService")
    void getShopsStatsByRole_DelegatesProperly() {
        List<StatDTO> mockStats = List.of(new StatDTO("Tiendas", 5L));

        when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
        when(shopService.getShopsStatistics(loggedUser)).thenReturn(mockStats);

        List<StatDTO> result = statService.getShopsStatsByRole();

        assertEquals(1, result.size());
        assertEquals("Tiendas", result.getFirst().label());
        assertEquals(5L, result.getFirst().value());

        verify(userService).findLoggedUserHelper();
        verify(shopService).getShopsStatistics(loggedUser);
    }

    @Test
    @DisplayName("getTrucksStatsByRole fetches logged user and delegates to TruckService")
    void getTrucksStatsByRole_DelegatesProperly() {
        List<StatDTO> mockStats = List.of(new StatDTO("Disponibles", 3L));

        when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
        when(truckService.getTruckStatistics(loggedUser)).thenReturn(mockStats);

        List<StatDTO> result = statService.getTrucksStatsByRole();

        assertEquals(1, result.size());
        assertEquals("Disponibles", result.getFirst().label());
        assertEquals(3L, result.getFirst().value());

        verify(userService).findLoggedUserHelper();
        verify(truckService).getTruckStatistics(loggedUser);
    }
}