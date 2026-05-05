package com.tfg.backend.unit;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.model.Truck;
import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderStatus;
import com.tfg.backend.model.ImageInfo;
import com.tfg.backend.dto.ShopDTO;
import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.service.ShopService;
import com.tfg.backend.service.ShopUserOrchestrator;
import com.tfg.backend.service.UserService;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.utils.GlobalDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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
    @Mock private ImageService imageService;
    @Mock private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private ShopUserOrchestrator orchestrator;

    private User loggedUser;
    private Shop shop;
    private User managerUser;
    private ShopDTO shopDTO;
    private AddressDTO addressDTO;

    @BeforeEach
    void setUp() {
        loggedUser = new User();
        loggedUser.setId(1L);

        managerUser = new User();
        managerUser.setId(5L);

        shop = new Shop();
        shop.setId(10L);
        shop.setReferenceCode("SH-TEST");
        shop.setAssignedManager(managerUser);

        loggedUser.setSelectedShop(shop);

        ImageInfo img = new ImageInfo();
        img.setS3Key("shop-pic.jpg");
        shop.setImage(img);

        addressDTO = new AddressDTO();
        addressDTO.setAlias("Main Store");
        addressDTO.setStreet("Gran Via");
        addressDTO.setNumber("12");
        addressDTO.setCity("Madrid");
        addressDTO.setLatitude(40.4168);
        addressDTO.setLongitude(-3.7038);

        shopDTO = new ShopDTO();
        shopDTO.setName("New Shop Name");
        shopDTO.setAssignedBudget(5000.0);
        shopDTO.setAddress(addressDTO);

        lenient().when(userService.findLoggedUserHelper()).thenReturn(loggedUser);
    }

    @Nested
    @DisplayName("Tests for basic Shop creation and updates")
    class CrudTests {

        @Test
        @DisplayName("createShop maps DTO properties to a new Entity and saves it")
        void createShop_Success() {
            when(shopService.save(any(Shop.class))).thenAnswer(i -> i.getArgument(0));

            Shop result = orchestrator.createShop(shopDTO);

            assertEquals("New Shop Name", result.getName());
            assertEquals(5000.0, result.getAssignedBudget());
            assertNotNull(result.getAddress());
            assertEquals("Main Store", result.getAddress().getAlias());
            assertEquals(40.4168, result.getAddress().getLatitude());

            verify(shopService).save(any(Shop.class));
        }

        @Test
        @DisplayName("updateShop updates existing entity fields using DTO data")
        void updateShop_Success() {
            when(shopService.findShopHelper(10L)).thenReturn(shop);

            Shop result = orchestrator.updateShop(10L, shopDTO);

            assertEquals("New Shop Name", result.getName());
            assertEquals(5000.0, result.getAssignedBudget());
            assertEquals("Madrid", result.getAddress().getCity());
        }

        @Test
        @DisplayName("deleteShop unlinks memory collections, cancels active orders and deletes S3 image")
        void deleteShop_CascadesAndUnlinks() {
            // Setup Truck
            Truck truck = new Truck();
            truck.setAssignedShop(shop);
            shop.getAssignedTrucks().add(truck);

            // Setup Customer
            User customer = new User();
            customer.setSelectedShop(shop);
            shop.getCustomers().add(customer);

            // Setup Orders (One active, one completed)
            Order activeOrder = new Order();
            activeOrder.changeOrderStatus(OrderStatus.ORDER_MADE, "Init");
            activeOrder.setAssignedShop(shop);

            Order completedOrder = new Order();
            completedOrder.changeOrderStatus(OrderStatus.COMPLETED, "Done");
            completedOrder.setAssignedShop(shop);

            shop.getAssignedOrders().addAll(List.of(activeOrder, completedOrder));

            when(shopService.findShopHelper(10L)).thenReturn(shop);

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultShopImage(shop.getImage())).thenReturn(false);

                // Act
                orchestrator.deleteShop(10L);

                // Assert unlinking
                assertTrue(shop.getAssignedTrucks().isEmpty());
                assertNull(truck.getAssignedShop());

                assertTrue(shop.getCustomers().isEmpty());
                assertNull(customer.getSelectedShop());

                assertTrue(shop.getAssignedOrders().isEmpty());
                assertNull(activeOrder.getAssignedShop());
                assertNull(completedOrder.getAssignedShop());

                // Assert business logic: Active order must be cancelled
                assertEquals(OrderStatus.CANCELLED, activeOrder.getCurrentStatus());
                assertEquals("La tienda a la que estaba asignado el pedido ha sido eliminada.",
                        activeOrder.getHistory().getLast().getUpdates().getLast().getDescription());

                // Completed order must remain completed
                assertEquals(OrderStatus.COMPLETED, completedOrder.getCurrentStatus());

                // Assert deletions
                verify(imageService).deleteFile("shop-pic.jpg");
                verify(shopService).delete(shop);
            }
        }
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

