package com.tfg.backend.integration;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.ShopUserOrchestrator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ShopUserOrchestratorITest {

    @Autowired private ShopUserOrchestrator orchestrator;
    @Autowired private ShopRepository shopRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private OrderRepository orderRepository;

    private User activeUser;
    private Shop localShop;

    @BeforeEach
    void setUpOrchestrator() {
        // 1. Save and capture the user
        activeUser = new User("Orch User", "user_orch", "orch@test.com", "pass", "USER,MANAGER");
        activeUser = userRepository.saveAndFlush(activeUser);

        // 2. Save and capture the shop
        localShop = new Shop("Local Store", null, 3000.0);
        localShop.setReferenceCode("SHOP-LOC-001");
        localShop = shopRepository.saveAndFlush(localShop);

        // 3. Authenticate the user for the security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(activeUser.getUsername(), "pass", java.util.List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("setSelectedShop extracts ID from map and correctly assigns local shop to logged user")
    void testSetSelectedShop_UpdatesLoggedUser() {
        // Simulate the controller request body as a map
        Map<String, Long> requestBody = new HashMap<>();
        requestBody.put("shopId", localShop.getId());

        boolean result = orchestrator.setSelectedShop(requestBody);

        assertTrue(result);
        User dbUser = userRepository.findById(activeUser.getId()).orElseThrow();
        assertNotNull(dbUser.getSelectedShop());
        assertEquals(localShop.getId(), dbUser.getSelectedShop().getId(), "The logged user should now have the local shop assigned");
    }

    @Test
    @DisplayName("setAssignedManager links and unlinks a manager from a shop correctly")
    void testSetAssignedManager_LinkAndUnlink() {
        // ACT 1: Link Manager (true)
        orchestrator.setAssignedManager(localShop.getId(), activeUser.getId(), true);

        Shop dbShop = shopRepository.findById(localShop.getId()).orElseThrow();
        assertNotNull(dbShop.getAssignedManager());
        assertEquals(activeUser.getId(), dbShop.getAssignedManager().getId(), "The user must be assigned as the shop manager");

        // ACT 2: Unlink Manager (false)
        orchestrator.setAssignedManager(localShop.getId(), activeUser.getId(), false);

        dbShop = shopRepository.findById(localShop.getId()).orElseThrow();
        assertNull(dbShop.getAssignedManager(), "The manager should be successfully unlinked");
    }

    @Test
    @DisplayName("Create Shop: Persists correctly with nested address")
    void testCreateShop_SavesAddressAndShop() {
        ShopDTO dto = new ShopDTO();
        dto.setName("New Outlet");
        dto.setAssignedBudget(2000.0);

        AddressDTO addressDTO = new AddressDTO();
        addressDTO.setStreet("Gran Via");
        addressDTO.setCity("Madrid");
        addressDTO.setCountry("Spain");
        dto.setAddress(addressDTO);

        Shop createdShop = orchestrator.createShop(dto);

        Shop dbShop = shopRepository.findById(createdShop.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("New Outlet", dbShop.getName()),
                () -> assertEquals(2000.0, dbShop.getAssignedBudget()),
                () -> assertNotNull(dbShop.getAddress()),
                () -> assertEquals("Gran Via", dbShop.getAddress().getStreet())
        );
    }

    @Test
    @DisplayName("Delete Shop: Strictly nullifies relations and automatically cancels pending orders")
    void testDeleteShop_CascadesUnlinksAndCancelsOrders() {
        // Setup complex scenario locally
        User customer = new User("Customer", "customer_del", "cust_del@test.com", "pass", "USER");
        customer = userRepository.saveAndFlush(customer);

        Shop mainShop = new Shop("Main Tech Shop", null, 1000.0);
        mainShop.setReferenceCode("SHOP-MAIN-DEL-123");
        mainShop = shopRepository.saveAndFlush(mainShop);

        customer.setSelectedShop(mainShop);
        mainShop.getCustomers().add(customer);
        customer = userRepository.saveAndFlush(customer);
        mainShop = shopRepository.saveAndFlush(mainShop);

        Truck deliveryTruck = new Truck();
        deliveryTruck.setReferenceCode("TRUCK-DEL-123");
        deliveryTruck.setPlateNumber("9999-ZZZ");
        deliveryTruck.setMaxCapacity(10);
        deliveryTruck.setAssignedShop(mainShop);
        mainShop.getAssignedTrucks().add(deliveryTruck);
        deliveryTruck = truckRepository.saveAndFlush(deliveryTruck);

        Order pendingOrder = new Order();
        pendingOrder.setUser(customer);
        pendingOrder.setReferenceCode("ORD-DEL-123");
        pendingOrder.setAssignedShop(mainShop);
        pendingOrder.setTotalCost(100.0);
        pendingOrder.changeOrderStatus(OrderStatus.ORDER_MADE, "Order placed");
        mainShop.getAssignedOrders().add(pendingOrder);
        pendingOrder = orderRepository.saveAndFlush(pendingOrder);

        // Act: Delete the shop via Orchestrator
        orchestrator.deleteShop(mainShop.getId());

        // Assertions
        assertFalse(shopRepository.existsById(mainShop.getId()), "Shop should be deleted from DB");

        Truck dbTruck = truckRepository.findById(deliveryTruck.getId()).orElseThrow();
        assertNull(dbTruck.getAssignedShop(), "Truck must be unlinked from the deleted shop");

        User dbCustomer = userRepository.findById(customer.getId()).orElseThrow();
        assertNull(dbCustomer.getSelectedShop(), "Customer's selected shop must be nullified");

        Order dbOrder = orderRepository.findById(pendingOrder.getId()).orElseThrow();
        assertAll(
                () -> assertNull(dbOrder.getAssignedShop(), "Order must be unlinked from shop"),
                () -> assertEquals(OrderStatus.CANCELLED, dbOrder.getCurrentStatus(), "Pending order must be automatically cancelled"),
                () -> assertTrue(dbOrder.getHistory().getLast().getUpdates().getLast().getDescription().contains("eliminada"))
        );
    }
}