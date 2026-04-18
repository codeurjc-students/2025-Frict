package com.tfg.backend.unit;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.ShopUserOrchestrator;
import com.tfg.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopUserOrchestratorUTest {

    @Mock private ShopService shopService;
    @Mock private UserService userService;
    @Mock private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private ShopUserOrchestrator orchestrator;

    private User loggedUser;
    private Shop shop;
    private User managerUser;

    @BeforeEach
    void setUp() {
        loggedUser = new User();
        loggedUser.setId(1L);

        managerUser = new User();
        managerUser.setId(5L);

        shop = new Shop();
        shop.setId(10L);
        shop.setAssignedManager(managerUser);
    }

    @Nested
    @DisplayName("Tests for Shop Selection and Reading")
    class SelectionAndReadTests {

        @Test
        @DisplayName("getAssignedShopsPage fetches logged user and requests shops from shopService")
        void getAssignedShopsPage_Success() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Shop> shopPage = new PageImpl<>(List.of(shop));

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findAllByAssignedManagerId(1L, pageable)).thenReturn(shopPage);

            Page<Shop> result = orchestrator.getAssignedShopsPage(pageable);

            assertEquals(1, result.getTotalElements());
            verify(shopService).findAllByAssignedManagerId(1L, pageable);
        }

        @Test
        @DisplayName("setSelectedShop extracts ID, fetches shop and delegates to userService")
        void setSelectedShop_WithShopId_Success() {
            Map<String, Long> requestBody = new HashMap<>();
            requestBody.put("shopId", 10L);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(shopService.findShopHelper(10L)).thenReturn(shop);
            when(userService.applyShopSelection(loggedUser, shop)).thenReturn(true);

            boolean result = orchestrator.setSelectedShop(requestBody);

            assertTrue(result);
            verify(shopService).findShopHelper(10L);
            verify(userService).applyShopSelection(loggedUser, shop);
        }

        @Test
        @DisplayName("setSelectedShop handles null shopId smoothly without fetching")
        void setSelectedShop_WithoutShopId_Success() {
            Map<String, Long> requestBody = new HashMap<>();
            requestBody.put("shopId", null);

            when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
            when(userService.applyShopSelection(loggedUser, null)).thenReturn(true);

            boolean result = orchestrator.setSelectedShop(requestBody);

            assertTrue(result);
            verify(shopService, never()).findShopHelper(any()); // Should not attempt to fetch a null shop
            verify(userService).applyShopSelection(loggedUser, null);
        }
    }

    @Nested
    @DisplayName("Tests for Manager Assignment")
    class ManagerAssignmentTests {

        @Test
        @DisplayName("setAssignedManager sets a new manager when state is true")
        void setAssignedManager_True_SetsNewManager() {
            shop.setAssignedManager(null); // Initially no manager

            when(shopService.findShopHelper(10L)).thenReturn(shop);
            when(userService.findUserHelper(5L)).thenReturn(managerUser);

            Shop result = orchestrator.setAssignedManager(10L, 5L, true);

            assertEquals(managerUser, result.getAssignedManager());
        }

        @Test
        @DisplayName("setAssignedManager removes manager if state is false AND ID matches the current manager")
        void setAssignedManager_False_RemovesManager_IfIdMatches() {
            // shop already has managerUser (ID: 5L) from setUp()
            when(shopService.findShopHelper(10L)).thenReturn(shop);

            // Removing ID 5L, which is the exact current manager
            Shop result = orchestrator.setAssignedManager(10L, 5L, false);

            assertNull(result.getAssignedManager());
        }

        @Test
        @DisplayName("setAssignedManager does NOT remove manager if state is false but IDs do not match")
        void setAssignedManager_False_DoesNotRemove_IfIdMismatch() {
            // shop has managerUser (ID: 5L)
            when(shopService.findShopHelper(10L)).thenReturn(shop);

            // Trying to remove ID 99L (maybe a concurrent outdated request or bad data)
            Shop result = orchestrator.setAssignedManager(10L, 99L, false);

            assertNotNull(result.getAssignedManager());
            assertEquals(5L, result.getAssignedManager().getId(), "The manager should remain untouched");
        }
    }
}