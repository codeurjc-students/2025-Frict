package com.tfg.backend.unit;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.service.TruckService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.dto.StatDTO;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TruckServiceUTest {

    @Mock
    private UserService userService;

    @Mock
    private TruckRepository truckRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private TruckService truckService;

    private Truck truck;
    private User driver;
    private Shop shop;
    private TruckDTO truckDTO;
    private AddressDTO addressDTO;

    @BeforeEach
    void setUp() {
        // Setup Shop
        shop = new Shop();
        shop.setId(10L);
        shop.setAssignedTrucks(new ArrayList<>());

        // Setup Driver
        driver = new User();
        driver.setId(5L);
        driver.setRoles(new HashSet<>(List.of("DRIVER")));

        // Setup Truck with internal history to avoid NullPointerExceptions
        truck = new Truck();
        truck.setId(100L);
        truck.setPlateNumber("1234-ABC");
        truck.setMaxOrderCapacity(10);
        truck.setAssignedShop(shop);
        truck.setAssignedDriver(driver);
        truck.setOrdersToDeliver(new HashSet<>());

        // Link driver back to truck
        driver.setAssignedTruck(truck);

        // Link shop back to truck
        shop.getAssignedTrucks().add(truck);

        // Setup DTOs
        addressDTO = new AddressDTO();
        addressDTO.setStreet("Delivery St");
        addressDTO.setCity("Madrid");
        addressDTO.setLatitude(40.0);
        addressDTO.setLongitude(-3.0);

        truckDTO = new TruckDTO();
        truckDTO.setPlateNumber("9999-XYZ");
        truckDTO.setMaxOrderCapacity(15);
        truckDTO.setAddress(addressDTO);
    }

    // --- HELPER & FETCH TESTS ---
    @Nested
    @DisplayName("Tests for internal helpers and specific read methods")
    class ReadAndHelperTests {

        @Test
        @DisplayName("findTruckHelper returns Truck if it exists in DB")
        void findTruckHelper_Success() {
            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));

            Truck result = truckService.findTruckHelper(100L);

            assertNotNull(result);
            assertEquals(100L, result.getId());
        }

        @Test
        @DisplayName("findTruckHelper throws 404 NOT_FOUND if Truck is missing")
        void findTruckHelper_ThrowsNotFound() {
            when(truckRepository.findById(999L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> truckService.findTruckHelper(999L));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Truck with ID 999 does not exist.", ex.getReason());
        }

        @Test
        @DisplayName("getAssignedTruckByDriverId fetches user and returns their assigned truck")
        void getAssignedTruckByDriverId_Success() {
            when(userService.findUserHelper(5L)).thenReturn(driver);

            Truck result = truckService.getAssignedTruckByDriverId(5L);

            assertEquals(truck, result);
        }
    }

    // --- CRUD & ASSIGNMENT TESTS ---
    @Nested
    @DisplayName("Tests for creation, updates, and assignments")
    class CrudAndAssignmentTests {

        @Test
        @DisplayName("createTruck maps DTO and assigns shop correctly")
        void createTruck_Success() {
            when(truckRepository.save(any(Truck.class))).thenAnswer(i -> i.getArgument(0));

            Truck result = truckService.createTruck(truckDTO, shop);

            assertEquals("9999-XYZ", result.getPlateNumber());
            assertEquals(15, result.getMaxOrderCapacity());
            assertEquals(shop, result.getAssignedShop());
            assertNotNull(result.getAddress());
            assertEquals("Madrid", result.getAddress().getCity());
            assertEquals(40.0, result.getAddress().getLatitude());

            verify(truckRepository).save(any(Truck.class));
        }

        @Test
        @DisplayName("updateTruck modifies fields via DTO and updates assigned shop")
        void updateTruck_Success() {
            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));

            Shop newShop = new Shop();
            newShop.setId(20L);

            Truck result = truckService.updateTruck(100L, truckDTO, newShop);

            assertEquals("9999-XYZ", result.getPlateNumber());
            assertEquals(15, result.getMaxOrderCapacity());
            assertEquals(newShop, result.getAssignedShop());
        }

        @Test
        @DisplayName("setAssignedDriver links the user when state is true")
        void setAssignedDriver_True_LinksUser() {
            truck.setAssignedDriver(null); // Clear initial driver

            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));
            when(userService.findUserHelper(5L)).thenReturn(driver);

            Truck result = truckService.setAssignedDriver(5L, 100L, true);

            assertEquals(driver, result.getAssignedDriver());
        }

        @Test
        @DisplayName("setAssignedDriver unlinks the user when state is false")
        void setAssignedDriver_False_UnlinksUser() {
            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));

            Truck result = truckService.setAssignedDriver(5L, 100L, false);

            assertNull(result.getAssignedDriver());
        }
    }

    // --- STATUS UPDATES TESTS ---
    @Nested
    @DisplayName("Tests for status update logic")
    class StatusUpdateTests {

        @Test
        @DisplayName("commentAndOrUpdateTruckStatus adds a comment to existing status log if status is the same")
        void commentAndOrUpdateTruckStatus_SameStatus_AddsCommentOnly() {
            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));

            truckService.commentAndOrUpdateTruckStatus(100L, TruckStatus.AVAILABLE, "Truck washed");

            assertEquals(TruckStatus.AVAILABLE, truck.getHistory().getLast().getStatus());
            assertEquals(1, truck.getHistory().size(), "Should still be 1 status log");
            assertEquals(2, truck.getHistory().getLast().getUpdates().size(), "Should have 2 comments inside the log");
            assertEquals("Truck washed", truck.getHistory().getLast().getUpdates().getLast().getDescription());
        }

        @Test
        @DisplayName("commentAndOrUpdateTruckStatus creates a new status log if status changes")
        void commentAndOrUpdateTruckStatus_DifferentStatus_ChangesStatus() {
            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));

            truckService.commentAndOrUpdateTruckStatus(100L, TruckStatus.ON_ROUTE, "Departed");

            assertEquals(TruckStatus.ON_ROUTE, truck.getHistory().getLast().getStatus());
            assertEquals(2, truck.getHistory().size(), "Should have created a new status log");
            assertEquals("Departed", truck.getHistory().getLast().getUpdates().getFirst().getDescription());
        }
    }

    // --- DELETION LOGIC TESTS ---
    @Nested
    @DisplayName("Tests for complex deletion logic")
    class DeletionTests {

        @Test
        @DisplayName("deleteTruck unlinks everything and reverts ON_DELIVERY orders to SENT")
        void deleteTruck_Success_CascadesAndRevertsOrders() {
            // Setup an ON_DELIVERY order assigned to this truck
            Order onDeliveryOrder = new Order();
            onDeliveryOrder.changeOrderStatus(OrderStatus.ON_DELIVERY, "Delivering");
            onDeliveryOrder.setAssignedTruck(truck);

            // Setup a SENT order assigned to this truck (should NOT be reverted)
            Order sentOrder = new Order();
            sentOrder.changeOrderStatus(OrderStatus.SENT, "Waiting in truck");
            sentOrder.setAssignedTruck(truck);

            truck.getOrdersToDeliver().addAll(List.of(onDeliveryOrder, sentOrder));

            when(truckRepository.findById(100L)).thenReturn(Optional.of(truck));

            // Act
            truckService.deleteTruck(100L);

            // 1. Assert Shop is unlinked
            assertNull(truck.getAssignedShop());
            assertFalse(shop.getAssignedTrucks().contains(truck));

            // 2. Assert Driver is unlinked
            assertNull(truck.getAssignedDriver());
            assertNull(driver.getAssignedTruck());

            // 3. Assert Orders logic
            assertTrue(truck.getOrdersToDeliver().isEmpty(), "Truck's order list must be cleared");
            assertNull(onDeliveryOrder.getAssignedTruck(), "Order must be unlinked from truck");
            assertNull(sentOrder.getAssignedTruck(), "Order must be unlinked from truck");

            // Specifically verify the rollback of the ON_DELIVERY order
            assertEquals(OrderStatus.SENT, onDeliveryOrder.getCurrentStatus(), "ON_DELIVERY order must be rolled back to SENT");
            assertEquals("El camión ha sido borrado y el pedido ha vuelto al estado anterior.",
                    onDeliveryOrder.getHistory().getLast().getUpdates().getLast().getDescription());

            // Specifically verify the SENT order was untouched in terms of status
            assertEquals(OrderStatus.SENT, sentOrder.getCurrentStatus(), "SENT order status should remain untouched");

            // 4. Verify DB deletion
            verify(truckRepository).delete(truck);
        }
    }

    // --- METRICS TESTS ---
    @Nested
    @DisplayName("Tests for Truck Statistics")
    class MetricsTests {

        @Test
        @DisplayName("Returns correct metrics for ADMIN role fetching global data")
        void getTruckStatistics_ForAdmin() {
            User admin = new User();
            admin.setRoles(new HashSet<>(List.of("ADMIN")));

            when(truckRepository.countTrucksByStatus(List.of(TruckStatus.AVAILABLE))).thenReturn(10L);
            when(truckRepository.countTrucksByStatus(List.of(TruckStatus.ON_ROUTE))).thenReturn(5L);
            when(truckRepository.countTrucksByStatus(List.of(TruckStatus.MAINTENANCE))).thenReturn(2L);
            when(truckRepository.countTrucksByStatus(List.of(TruckStatus.OUT_OF_SERVICE))).thenReturn(1L);

            List<StatDTO> stats = truckService.getTruckStatistics(admin);

            assertEquals(4, stats.size());
            assertEquals("Disponibles", stats.get(0).label());
            assertEquals(10L, stats.get(0).value());
            assertEquals(5L, stats.get(1).value());
            assertEquals(2L, stats.get(2).value());
            assertEquals(1L, stats.get(3).value());
        }

        @Test
        @DisplayName("Returns correct metrics for MANAGER role fetching local data")
        void getTruckStatistics_ForManager() {
            User manager = new User();
            manager.setId(88L);
            manager.setRoles(new HashSet<>(List.of("MANAGER")));

            when(truckRepository.countTrucksByManagerIdAndStatus(88L, List.of(TruckStatus.AVAILABLE))).thenReturn(8L);
            when(truckRepository.countTrucksByManagerIdAndStatus(88L, List.of(TruckStatus.ON_ROUTE))).thenReturn(3L);
            when(truckRepository.countTrucksByManagerIdAndStatus(88L, List.of(TruckStatus.MAINTENANCE))).thenReturn(1L);
            when(truckRepository.countTrucksByManagerIdAndStatus(88L, List.of(TruckStatus.OUT_OF_SERVICE))).thenReturn(0L);

            List<StatDTO> stats = truckService.getTruckStatistics(manager);

            assertEquals(4, stats.size());
            assertEquals("Disponibles", stats.get(0).label());
            assertEquals(8L, stats.get(0).value());
            assertEquals(3L, stats.get(1).value());
        }

        @Test
        @DisplayName("Returns empty metrics for users without relevant roles")
        void getTruckStatistics_ForUser() {
            User standardUser = new User();
            standardUser.setRoles(new HashSet<>(List.of("USER")));

            List<StatDTO> stats = truckService.getTruckStatistics(standardUser);

            assertTrue(stats.isEmpty());
        }
    }
}