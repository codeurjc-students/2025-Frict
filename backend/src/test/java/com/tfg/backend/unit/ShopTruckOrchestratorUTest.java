package com.tfg.backend.unit;

import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.Truck;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.ShopTruckOrchestrator;
import com.tfg.backend.service.TruckService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopTruckOrchestratorUTest {

    @Mock private ShopService shopService;
    @Mock private TruckService truckService;
    @Mock private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private ShopTruckOrchestrator orchestrator;

    private Shop shop;
    private Truck truck;
    private TruckDTO truckDTO;

    @BeforeEach
    void setUp() {
        shop = new Shop();
        shop.setId(10L);
        shop.setAssignedTrucks(new ArrayList<>());

        truck = new Truck();
        truck.setId(100L);
        truck.setAssignedShop(shop);

        truckDTO = new TruckDTO();
        truckDTO.setShopId(10L);
    }

    // --- READ METHODS TESTS ---
    @Nested
    @DisplayName("Tests for retrieval and read logic")
    class ReadMethodsTests {

        @Test
        @DisplayName("getShopByAssignedTruckId returns shop if truck has one assigned")
        void getShopByAssignedTruckId_Success() {
            when(truckService.findTruckHelper(100L)).thenReturn(truck);

            Shop result = orchestrator.getShopByAssignedTruckId(100L);

            assertNotNull(result);
            assertEquals(10L, result.getId());
        }

        @Test
        @DisplayName("getShopByAssignedTruckId throws 404 NOT_FOUND if truck is an orphan (no shop assigned)")
        void getShopByAssignedTruckId_ThrowsNotFound_WhenNoShop() {
            truck.setAssignedShop(null); // Orphan truck
            when(truckService.findTruckHelper(100L)).thenReturn(truck);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> orchestrator.getShopByAssignedTruckId(100L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("This truck is not assigned to any shop.", ex.getReason());
        }

        @Test
        @DisplayName("getAllShopTrucks fetches shop and returns its internal list")
        void getAllShopTrucks_Success() {
            shop.getAssignedTrucks().add(truck);
            when(shopService.findShopHelper(10L)).thenReturn(shop);

            List<Truck> result = orchestrator.getAllShopTrucks(10L);

            assertEquals(1, result.size());
            assertTrue(result.contains(truck));
        }
    }

    // --- ASSIGNMENT AND MUTATION TESTS ---
    @Nested
    @DisplayName("Tests for creation, update and assignment routing")
    class MutationTests {

        @Test
        @DisplayName("setAssignedTruck assigns shop to truck when state is true")
        void setAssignedTruck_Assigns_WhenTrue() {
            truck.setAssignedShop(null);

            when(shopService.findShopHelper(10L)).thenReturn(shop);
            when(truckService.findTruckHelper(100L)).thenReturn(truck);

            Truck result = orchestrator.setAssignedTruck(10L, 100L, true);

            assertEquals(shop, result.getAssignedShop());
        }

        @Test
        @DisplayName("setAssignedTruck unlinks shop from truck when state is false")
        void setAssignedTruck_Unlinks_WhenFalse() {
            when(shopService.findShopHelper(10L)).thenReturn(shop);
            when(truckService.findTruckHelper(100L)).thenReturn(truck); // Currently has shop assigned

            Truck result = orchestrator.setAssignedTruck(10L, 100L, false);

            assertNull(result.getAssignedShop());
        }

        @Test
        @DisplayName("createTruck fetches shop before delegating if shopId is provided")
        void createTruck_WithShop_DelegatesCorrectly() {
            when(shopService.findShopHelper(10L)).thenReturn(shop);
            when(truckService.createTruck(truckDTO, shop)).thenReturn(truck);

            Truck result = orchestrator.createTruck(truckDTO);

            assertEquals(truck, result);
            verify(shopService).findShopHelper(10L);
            verify(truckService).createTruck(truckDTO, shop);
        }

        @Test
        @DisplayName("createTruck passes null shop to service if shopId is not provided in DTO")
        void createTruck_WithoutShop_DelegatesCorrectly() {
            truckDTO.setShopId(null);
            when(truckService.createTruck(truckDTO, null)).thenReturn(truck);

            Truck result = orchestrator.createTruck(truckDTO);

            assertEquals(truck, result);
            verify(shopService, never()).findShopHelper(any()); // Shouldn't try to fetch shop
            verify(truckService).createTruck(truckDTO, null);
        }

        @Test
        @DisplayName("updateTruck fetches shop and delegates if shopId is provided")
        void updateTruck_WithShop_DelegatesCorrectly() {
            when(shopService.findShopHelper(10L)).thenReturn(shop);
            when(truckService.updateTruck(100L, truckDTO, shop)).thenReturn(truck);

            Truck result = orchestrator.updateTruck(100L, truckDTO);

            assertEquals(truck, result);
            verify(shopService).findShopHelper(10L);
            verify(truckService).updateTruck(100L, truckDTO, shop);
        }

        @Test
        @DisplayName("updateTruck passes null shop to service if shopId is missing")
        void updateTruck_WithoutShop_DelegatesCorrectly() {
            truckDTO.setShopId(null);
            when(truckService.updateTruck(100L, truckDTO, null)).thenReturn(truck);

            Truck result = orchestrator.updateTruck(100L, truckDTO);

            assertEquals(truck, result);
            verify(shopService, never()).findShopHelper(any());
            verify(truckService).updateTruck(100L, truckDTO, null);
        }
    }
}