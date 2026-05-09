package com.tfg.backend.integration;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.TruckService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TruckService.
 * Validates complex lifecycle operations including driver assignments,
 * shop linking, and the critical status rollback for orders when a truck is deleted.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TruckServiceITest {

    @Autowired private TruckService truckService;
    @Autowired private TruckRepository truckRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager entityManager;

    private User manager;
    private User driver;
    private Shop mainShop;
    private Truck activeTruck;
    private Order shippingOrder;

    @BeforeEach
    void setUpTruckScenario() {
        // 1. Create Manager
        manager = new User("Manager Test", "mng", "mng@test.com", "pass", "MANAGER");
        userRepository.save(manager);

        // 2. Create Shop and link manager
        mainShop = new Shop("Distribution Center", null, 50000.0);
        mainShop.setReferenceCode("SHOP-TRUCK-001");
        mainShop.setAssignedManager(manager); // Link manager to shop
        shopRepository.save(mainShop);

        // 3. Create Driver
        driver = new User("John Driver", "jdriver", "driver@test.com", "pass", "DRIVER");
        userRepository.save(driver);

        // 4. Create Truck
        activeTruck = new Truck("1234-XYZ", null, 15);
        activeTruck.setReferenceCode("TR-001");
        activeTruck.setAssignedShop(mainShop);
        activeTruck.setAssignedDriver(driver);
        activeTruck.changeTruckStatus(TruckStatus.AVAILABLE, "Initial state");
        truckRepository.save(activeTruck);

        // 5. Create Order
        shippingOrder = new Order();
        shippingOrder.setReferenceCode("ORD-TRUCK-99");
        shippingOrder.setAssignedShop(mainShop);
        shippingOrder.setAssignedTruck(activeTruck);
        shippingOrder.setTotalCost(50.0);
        shippingOrder.setSubtotalCost(50.0);
        shippingOrder.setShippingCost(0.0);
        shippingOrder.setTotalDiscount(0.0);
        shippingOrder.setTotalItems(1);
        shippingOrder.changeOrderStatus(OrderStatus.ON_DELIVERY, "Out for delivery");
        orderRepository.save(shippingOrder);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Create Truck: Persists correctly with its assigned shop and address")
    void testCreateTruck_SavesInDatabase() {
        TruckDTO dto = new TruckDTO();
        dto.setPlateNumber("5555-BBB");
        dto.setMaxOrderCapacity(20);

        AddressDTO addr = new AddressDTO();
        addr.setStreet("Industrial Way");
        addr.setCity("Madrid");
        dto.setAddress(addr);

        Truck saved = truckService.createTruck(dto, mainShop);

        Truck dbTruck = truckRepository.findById(saved.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("5555-BBB", dbTruck.getPlateNumber()),
                () -> assertEquals(mainShop.getId(), dbTruck.getAssignedShop().getId()),
                () -> assertEquals("Industrial Way", dbTruck.getAddress().getStreet())
        );
    }

    @Test
    @DisplayName("Set Assigned Driver: Correcty links the user to the truck in the database")
    void testSetAssignedDriver_UpdatesRelationship() {
        User newDriver = new User("New Driver", "ndriver", "nd@test.com", "pass", "DRIVER");
        userRepository.save(newDriver);

        truckService.setAssignedDriver(newDriver.getId(), activeTruck.getId(), true);

        Truck dbTruck = truckRepository.findById(activeTruck.getId()).orElseThrow();
        assertEquals(newDriver.getId(), dbTruck.getAssignedDriver().getId(), "Relationship should be updated in DB");
    }

    @Test
    @DisplayName("Delete Truck: Unlinks driver and shop, and rolls back ON_DELIVERY orders to SENT")
    void testDeleteTruck_CascadesSafetyLogic() {
        // Act: Delete the truck that is currently delivering an order
        truckService.deleteTruck(activeTruck.getId());

        // 1. Verify truck is gone
        assertFalse(truckRepository.existsById(activeTruck.getId()));

        // 2. Verify Driver is freed
        User dbDriver = userRepository.findById(driver.getId()).orElseThrow();
        assertNull(dbDriver.getAssignedTruck(), "Driver should no longer be assigned to the deleted truck");

        // 3. Verify Order rollback logic (Crucial Business Rule)
        Order dbOrder = orderRepository.findById(shippingOrder.getId()).orElseThrow();
        assertAll(
                () -> assertNull(dbOrder.getAssignedTruck(), "Order should be unlinked from the truck"),
                () -> assertEquals(OrderStatus.SENT, dbOrder.getCurrentStatus(), "Order status must revert from ON_DELIVERY to SENT"),
                () -> assertTrue(dbOrder.getHistory().getLast().getUpdates().getLast().getDescription().contains("borrado"), "History must explain the rollback")
        );
    }

    @Test
    @DisplayName("Metrics: Correctly counts trucks by status and manager ID using SQL joins")
    void testTruckMetrics_RepositoryQueries() {
        long adminCount = truckRepository.countTrucksByStatus(List.of(TruckStatus.AVAILABLE));
        long managerCount = truckRepository.countTrucksByManagerIdAndStatus(manager.getId(), List.of(TruckStatus.AVAILABLE));

        assertAll(
                () -> assertEquals(1, adminCount, "Admin should see 1 available truck"),
                () -> assertEquals(1, managerCount, "Manager should see 1 available truck in their shop")
        );
    }
}