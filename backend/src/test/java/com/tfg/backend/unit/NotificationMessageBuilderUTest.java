package com.tfg.backend.unit;

import com.tfg.backend.dto.EventAction;
import com.tfg.backend.event.ShopStockEvent;
import com.tfg.backend.model.NotificationRole;
import com.tfg.backend.service.NotificationMessageBuilder;
import com.tfg.backend.service.NotificationMessageBuilder.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NotificationMessageBuilderUTest {

    private final NotificationMessageBuilder builder = new NotificationMessageBuilder();

    private static final Pattern MONGO_ID = Pattern.compile("[a-f0-9]{24}");

    // --- Shared realistic contexts ---

    private OrderMessageContext orderCtx() {
        return new OrderMessageContext("ORD-2026-0001", "ORDER_MADE", "SENT",
                "admin.user", "lucia.gomez", "1234-ABC", "Tienda Centro");
    }

    private TruckMessageContext truckCtx() {
        return new TruckMessageContext("1234-ABC", "TR-001", "IDLE", "IN_DELIVERY",
                "admin.user", "juan.perez", "carlos.ruiz", "Tienda Centro");
    }

    private ShopMessageContext shopCtx() {
        return new ShopMessageContext("Tienda Centro", "SH-001", null, null,
                "admin.user", "manager.user", "1234-ABC");
    }

    private ProductMessageContext productCtx() {
        return new ProductMessageContext("Aceite de oliva 1L", "PRD-0042", null, null,
                "admin.user", "Tienda Centro");
    }

    private UserMessageContext userCtx() {
        return new UserMessageContext("pedro.sanchez", "Pedro Sánchez", "USER", null, null,
                "admin.user", "Tienda Centro", null);
    }

    private ReviewMessageContext reviewCtx() {
        return new ReviewMessageContext("lucia.gomez", "Aceite de oliva 1L", "PRD-0042", 5, "lucia.gomez");
    }

    private ShopStockMessageContext stockCtx() {
        return new ShopStockMessageContext("Tienda Centro", "SH-001", "Aceite de oliva 1L", "PRD-0042",
                0, 20, 5, "manager.user");
    }

    private void assertNoMongoId(NotificationMessage msg) {
        assertFalse(MONGO_ID.matcher(msg.subject()).find(),
                "subject must not contain a MongoDB ObjectId: " + msg.subject());
        assertFalse(MONGO_ID.matcher(msg.description()).find(),
                "description must not contain a MongoDB ObjectId: " + msg.description());
    }

    // ==========================================
    // ORDER
    // ==========================================
    @Nested
    @DisplayName("buildForOrder")
    class BuildForOrderTests {

        @Test
        @DisplayName("CREATED — ADMIN subject contains refCode and description mentions customer")
        void created_Admin() {
            NotificationMessage msg = builder.buildForOrder(EventAction.CREATED, NotificationRole.ADMIN, orderCtx());
            assertTrue(msg.subject().contains("ORD-2026-0001"));
            assertTrue(msg.description().contains("lucia.gomez"));
            assertNoMongoId(msg);
        }

        @Test
        @DisplayName("CREATED — MANAGER description mentions shop name")
        void created_Manager() {
            NotificationMessage msg = builder.buildForOrder(EventAction.CREATED, NotificationRole.MANAGER, orderCtx());
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("CREATED — DRIVER description mentions shop name")
        void created_Driver() {
            NotificationMessage msg = builder.buildForOrder(EventAction.CREATED, NotificationRole.DRIVER, orderCtx());
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("CREATED — CUSTOMER subject and description both contain refCode")
        void created_Customer() {
            NotificationMessage msg = builder.buildForOrder(EventAction.CREATED, NotificationRole.CUSTOMER, orderCtx());
            assertTrue(msg.subject().contains("ORD-2026-0001"));
            assertTrue(msg.description().contains("ORD-2026-0001"));
        }

        @Test
        @DisplayName("STATUS_CHANGED — ADMIN and CUSTOMER produce distinct subjects")
        void statusChanged_AdminVsCustomer() {
            NotificationMessage admin = builder.buildForOrder(EventAction.STATUS_CHANGED, NotificationRole.ADMIN, orderCtx());
            NotificationMessage customer = builder.buildForOrder(EventAction.STATUS_CHANGED, NotificationRole.CUSTOMER, orderCtx());
            assertNotEquals(admin.subject(), customer.subject());
            assertTrue(admin.description().contains("lucia.gomez"), "ADMIN should see customer in description");
        }

        @Test
        @DisplayName("STATUS_CHANGED — DRIVER description mentions truck plate")
        void statusChanged_Driver() {
            NotificationMessage msg = builder.buildForOrder(EventAction.STATUS_CHANGED, NotificationRole.DRIVER, orderCtx());
            assertTrue(msg.description().contains("1234-ABC"));
        }

        @Test
        @DisplayName("STATUS_CHANGED — MANAGER description mentions shop name")
        void statusChanged_Manager() {
            NotificationMessage msg = builder.buildForOrder(EventAction.STATUS_CHANGED, NotificationRole.MANAGER, orderCtx());
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("ASSIGNED — DRIVER and CUSTOMER produce distinct subjects")
        void assigned_RolesDiffer() {
            NotificationMessage driver = builder.buildForOrder(EventAction.ASSIGNED, NotificationRole.DRIVER, orderCtx());
            NotificationMessage customer = builder.buildForOrder(EventAction.ASSIGNED, NotificationRole.CUSTOMER, orderCtx());
            assertNotEquals(driver.subject(), customer.subject());
        }

        @Test
        @DisplayName("NEW_COMMENT — CUSTOMER and DRIVER produce distinct subjects from default")
        void newComment_RolesDiffer() {
            NotificationMessage admin = builder.buildForOrder(EventAction.NEW_COMMENT, NotificationRole.ADMIN, orderCtx());
            NotificationMessage customer = builder.buildForOrder(EventAction.NEW_COMMENT, NotificationRole.CUSTOMER, orderCtx());
            NotificationMessage driver = builder.buildForOrder(EventAction.NEW_COMMENT, NotificationRole.DRIVER, orderCtx());
            assertNotEquals(admin.subject(), customer.subject());
            assertNotEquals(admin.subject(), driver.subject());
        }

        @Test
        @DisplayName("DELETED — CUSTOMER gets a distinct subject from ADMIN default")
        void deleted_CustomerVsDefault() {
            NotificationMessage customer = builder.buildForOrder(EventAction.DELETED, NotificationRole.CUSTOMER, orderCtx());
            NotificationMessage admin = builder.buildForOrder(EventAction.DELETED, NotificationRole.ADMIN, orderCtx());
            assertNotEquals(customer.subject(), admin.subject());
        }

        @Test
        @DisplayName("Null context values produce readable fallbacks with no 'null' literal")
        void nullContext_FallsBack() {
            OrderMessageContext ctx = new OrderMessageContext(null, null, null, null, null, null, null);
            NotificationMessage msg = builder.buildForOrder(EventAction.CREATED, NotificationRole.ADMIN, ctx);
            assertNotNull(msg.subject());
            assertFalse(msg.subject().isBlank());
            assertFalse(msg.description().contains("null"));
        }
    }

    // ==========================================
    // TRUCK
    // ==========================================
    @Nested
    @DisplayName("buildForTruck")
    class BuildForTruckTests {

        @Test
        @DisplayName("CREATED — ADMIN subject contains plate and description mentions actor")
        void created_Admin() {
            NotificationMessage msg = builder.buildForTruck(EventAction.CREATED, NotificationRole.ADMIN, truckCtx());
            assertTrue(msg.subject().contains("1234-ABC"));
            assertTrue(msg.description().contains("admin.user"));
        }

        @Test
        @DisplayName("CREATED — non-ADMIN roles get a generic message")
        void created_DefaultRole() {
            NotificationMessage msg = builder.buildForTruck(EventAction.CREATED, NotificationRole.DRIVER, truckCtx());
            assertFalse(msg.subject().isBlank());
            assertFalse(msg.description().isBlank());
        }

        @Test
        @DisplayName("ASSIGNED — ADMIN, MANAGER, DRIVER produce distinct subjects")
        void assigned_ThreeRolesDiffer() {
            NotificationMessage admin = builder.buildForTruck(EventAction.ASSIGNED, NotificationRole.ADMIN, truckCtx());
            NotificationMessage manager = builder.buildForTruck(EventAction.ASSIGNED, NotificationRole.MANAGER, truckCtx());
            NotificationMessage driver = builder.buildForTruck(EventAction.ASSIGNED, NotificationRole.DRIVER, truckCtx());
            assertNotEquals(admin.subject(), manager.subject());
            assertNotEquals(manager.subject(), driver.subject());
        }

        @Test
        @DisplayName("ASSIGNED — ADMIN description shows old and new driver")
        void assigned_Admin_ShowsDriverChange() {
            NotificationMessage msg = builder.buildForTruck(EventAction.ASSIGNED, NotificationRole.ADMIN, truckCtx());
            assertTrue(msg.description().contains("juan.perez"));
            assertTrue(msg.description().contains("carlos.ruiz"));
        }

        @Test
        @DisplayName("ASSIGNED — MANAGER description mentions new driver and shop")
        void assigned_Manager() {
            NotificationMessage msg = builder.buildForTruck(EventAction.ASSIGNED, NotificationRole.MANAGER, truckCtx());
            assertTrue(msg.description().contains("juan.perez"));
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("ASSIGNED — DRIVER description mentions plate and shop")
        void assigned_Driver() {
            NotificationMessage msg = builder.buildForTruck(EventAction.ASSIGNED, NotificationRole.DRIVER, truckCtx());
            assertTrue(msg.description().contains("1234-ABC"));
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("STATUS_CHANGED — DRIVER and MANAGER produce distinct subjects")
        void statusChanged_RolesDiffer() {
            NotificationMessage driver = builder.buildForTruck(EventAction.STATUS_CHANGED, NotificationRole.DRIVER, truckCtx());
            NotificationMessage manager = builder.buildForTruck(EventAction.STATUS_CHANGED, NotificationRole.MANAGER, truckCtx());
            assertNotEquals(driver.subject(), manager.subject());
        }

        @Test
        @DisplayName("UPDATED produces a non-null message")
        void updated() {
            NotificationMessage msg = builder.buildForTruck(EventAction.UPDATED, NotificationRole.ADMIN, truckCtx());
            assertNotNull(msg.subject());
            assertFalse(msg.description().isBlank());
        }

        @Test
        @DisplayName("DELETED produces a message containing the plate")
        void deleted() {
            NotificationMessage msg = builder.buildForTruck(EventAction.DELETED, NotificationRole.ADMIN, truckCtx());
            assertTrue(msg.subject().contains("1234-ABC"));
        }
    }

    // ==========================================
    // SHOP
    // ==========================================
    @Nested
    @DisplayName("buildForShop")
    class BuildForShopTests {

        @Test
        @DisplayName("CREATED contains shop name in subject and description")
        void created() {
            NotificationMessage msg = builder.buildForShop(EventAction.CREATED, NotificationRole.ADMIN, shopCtx());
            assertTrue(msg.subject().contains("Tienda Centro"));
            assertTrue(msg.description().contains("admin.user"));
        }

        @Test
        @DisplayName("ASSIGNED — ADMIN, MANAGER, DRIVER produce distinct subjects")
        void assigned_RolesDiffer() {
            NotificationMessage admin = builder.buildForShop(EventAction.ASSIGNED, NotificationRole.ADMIN, shopCtx());
            NotificationMessage manager = builder.buildForShop(EventAction.ASSIGNED, NotificationRole.MANAGER, shopCtx());
            NotificationMessage driver = builder.buildForShop(EventAction.ASSIGNED, NotificationRole.DRIVER, shopCtx());
            assertNotEquals(admin.subject(), manager.subject());
            assertNotEquals(manager.subject(), driver.subject());
        }

        @Test
        @DisplayName("ASSIGNED — ADMIN description mentions truck and shop")
        void assigned_Admin() {
            NotificationMessage msg = builder.buildForShop(EventAction.ASSIGNED, NotificationRole.ADMIN, shopCtx());
            assertTrue(msg.description().contains("1234-ABC"));
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("STATUS_CHANGED — MANAGER and CUSTOMER produce distinct subjects")
        void statusChanged_ManagerVsCustomer() {
            NotificationMessage manager = builder.buildForShop(EventAction.STATUS_CHANGED, NotificationRole.MANAGER, shopCtx());
            NotificationMessage customer = builder.buildForShop(EventAction.STATUS_CHANGED, NotificationRole.CUSTOMER, shopCtx());
            assertNotEquals(manager.subject(), customer.subject());
        }

        @Test
        @DisplayName("UPDATED and DELETED produce non-null messages")
        void updatedAndDeleted() {
            assertNotNull(builder.buildForShop(EventAction.UPDATED, NotificationRole.ADMIN, shopCtx()));
            assertNotNull(builder.buildForShop(EventAction.DELETED, NotificationRole.ADMIN, shopCtx()));
        }
    }

    // ==========================================
    // PRODUCT
    // ==========================================
    @Nested
    @DisplayName("buildForProduct")
    class BuildForProductTests {

        @Test
        @DisplayName("CREATED — CUSTOMER differs from ADMIN")
        void created_RolesDiffer() {
            NotificationMessage admin = builder.buildForProduct(EventAction.CREATED, NotificationRole.ADMIN, productCtx());
            NotificationMessage customer = builder.buildForProduct(EventAction.CREATED, NotificationRole.CUSTOMER, productCtx());
            assertNotEquals(admin.subject(), customer.subject());
            assertTrue(admin.description().contains("admin.user"));
        }

        @Test
        @DisplayName("STATUS_CHANGED — ADMIN, MANAGER and CUSTOMER produce distinct subjects")
        void statusChanged_RolesDiffer() {
            NotificationMessage admin = builder.buildForProduct(EventAction.STATUS_CHANGED, NotificationRole.ADMIN, productCtx());
            NotificationMessage manager = builder.buildForProduct(EventAction.STATUS_CHANGED, NotificationRole.MANAGER, productCtx());
            NotificationMessage customer = builder.buildForProduct(EventAction.STATUS_CHANGED, NotificationRole.CUSTOMER, productCtx());
            assertNotEquals(admin.subject(), manager.subject());
            assertNotEquals(admin.subject(), customer.subject());
        }

        @Test
        @DisplayName("STATUS_CHANGED — ADMIN description mentions actor")
        void statusChanged_Admin_ContainsActor() {
            NotificationMessage msg = builder.buildForProduct(EventAction.STATUS_CHANGED, NotificationRole.ADMIN, productCtx());
            assertTrue(msg.description().contains("admin.user"));
        }

        @Test
        @DisplayName("DELETED description mentions product name and ref code")
        void deleted_ContainsIdentifiers() {
            NotificationMessage msg = builder.buildForProduct(EventAction.DELETED, NotificationRole.ADMIN, productCtx());
            assertTrue(msg.description().contains("Aceite de oliva 1L"));
            assertTrue(msg.description().contains("PRD-0042"));
        }

        @Test
        @DisplayName("UPDATED produces a non-null message")
        void updated() {
            assertNotNull(builder.buildForProduct(EventAction.UPDATED, NotificationRole.ADMIN, productCtx()));
        }

        @Test
        @DisplayName("Null product name falls back to refCode")
        void nullName_FallsBackToRefCode() {
            ProductMessageContext ctx = new ProductMessageContext(null, "PRD-FALLBACK", null, null, "actor", null);
            NotificationMessage msg = builder.buildForProduct(EventAction.DELETED, NotificationRole.ADMIN, ctx);
            assertTrue(msg.description().contains("PRD-FALLBACK"));
        }
    }

    // ==========================================
    // USER
    // ==========================================
    @Nested
    @DisplayName("buildForUser")
    class BuildForUserTests {

        @Test
        @DisplayName("NEW_COMMENT (ban) — ADMIN description contains target username and display name")
        void newComment_Admin_ContainsIdentifiers() {
            NotificationMessage msg = builder.buildForUser(EventAction.NEW_COMMENT, NotificationRole.ADMIN, userCtx());
            assertTrue(msg.description().contains("pedro.sanchez"));
            assertTrue(msg.description().contains("Pedro Sánchez"));
        }

        @Test
        @DisplayName("NEW_COMMENT (ban) — MANAGER description mentions shop")
        void newComment_Manager_ContainsShop() {
            NotificationMessage msg = builder.buildForUser(EventAction.NEW_COMMENT, NotificationRole.MANAGER, userCtx());
            assertTrue(msg.description().contains("Tienda Centro"));
            assertNotEquals(
                    builder.buildForUser(EventAction.NEW_COMMENT, NotificationRole.ADMIN, userCtx()).subject(),
                    msg.subject()
            );
        }

        @Test
        @DisplayName("ASSIGNED — ADMIN and MANAGER produce distinct subjects")
        void assigned_RolesDiffer() {
            NotificationMessage admin = builder.buildForUser(EventAction.ASSIGNED, NotificationRole.ADMIN, userCtx());
            NotificationMessage manager = builder.buildForUser(EventAction.ASSIGNED, NotificationRole.MANAGER, userCtx());
            assertNotEquals(admin.subject(), manager.subject());
        }

        @Test
        @DisplayName("STATUS_CHANGED — ADMIN and non-ADMIN produce distinct subjects")
        void statusChanged_AdminVsDefault() {
            NotificationMessage admin = builder.buildForUser(EventAction.STATUS_CHANGED, NotificationRole.ADMIN, userCtx());
            NotificationMessage driver = builder.buildForUser(EventAction.STATUS_CHANGED, NotificationRole.DRIVER, userCtx());
            assertNotEquals(admin.subject(), driver.subject());
        }

        @Test
        @DisplayName("CREATED contains target username in subject")
        void created_ContainsUsername() {
            NotificationMessage msg = builder.buildForUser(EventAction.CREATED, NotificationRole.ADMIN, userCtx());
            assertTrue(msg.subject().contains("pedro.sanchez"));
        }

        @Test
        @DisplayName("DELETED and UPDATED produce non-null messages")
        void deletedAndUpdated() {
            assertNotNull(builder.buildForUser(EventAction.DELETED, NotificationRole.ADMIN, userCtx()));
            assertNotNull(builder.buildForUser(EventAction.UPDATED, NotificationRole.ADMIN, userCtx()));
        }
    }

    // ==========================================
    // REVIEW
    // ==========================================
    @Nested
    @DisplayName("buildForReview")
    class BuildForReviewTests {

        @Test
        @DisplayName("CREATED — ADMIN/MANAGER description mentions reviewer and rating")
        void created_AdminManager() {
            NotificationMessage admin = builder.buildForReview(EventAction.CREATED, NotificationRole.ADMIN, reviewCtx());
            assertTrue(admin.description().contains("lucia.gomez"));
            assertTrue(admin.description().contains("5★"));
            assertNoMongoId(admin);
        }

        @Test
        @DisplayName("CREATED — CUSTOMER produces a distinct subject from ADMIN")
        void created_CustomerVsAdmin() {
            NotificationMessage admin = builder.buildForReview(EventAction.CREATED, NotificationRole.ADMIN, reviewCtx());
            NotificationMessage customer = builder.buildForReview(EventAction.CREATED, NotificationRole.CUSTOMER, reviewCtx());
            assertNotEquals(admin.subject(), customer.subject());
        }

        @Test
        @DisplayName("CREATED — null rating produces no rating marker")
        void created_NullRating_NoStarSymbol() {
            ReviewMessageContext ctx = new ReviewMessageContext("lucia.gomez", "Aceite de oliva 1L", "PRD-0042", null, "lucia.gomez");
            NotificationMessage msg = builder.buildForReview(EventAction.CREATED, NotificationRole.ADMIN, ctx);
            assertFalse(msg.description().contains("★"));
        }

        @Test
        @DisplayName("UPDATED description mentions reviewer and product")
        void updated() {
            NotificationMessage msg = builder.buildForReview(EventAction.UPDATED, NotificationRole.ADMIN, reviewCtx());
            assertTrue(msg.description().contains("Aceite de oliva 1L"));
            assertTrue(msg.description().contains("lucia.gomez"));
        }

        @Test
        @DisplayName("DELETED description mentions product name")
        void deleted() {
            NotificationMessage msg = builder.buildForReview(EventAction.DELETED, NotificationRole.ADMIN, reviewCtx());
            assertTrue(msg.description().contains("Aceite de oliva 1L"));
        }
    }

    // ==========================================
    // STOCK
    // ==========================================
    @Nested
    @DisplayName("buildForStockRestocked and buildForStockLow")
    class StockTests {

        @Test
        @DisplayName("RESTOCKED — ADMIN subject contains product and shop names")
        void restocked_Admin_ContainsIdentifiers() {
            NotificationMessage msg = builder.buildForStockRestocked(NotificationRole.ADMIN, stockCtx());
            assertTrue(msg.subject().contains("Aceite de oliva 1L"));
            assertTrue(msg.description().contains("Tienda Centro"));
            assertNoMongoId(msg);
        }

        @Test
        @DisplayName("RESTOCKED — ADMIN description shows added units and before/after counts")
        void restocked_Admin_ShowsUnitChanges() {
            NotificationMessage msg = builder.buildForStockRestocked(NotificationRole.ADMIN, stockCtx());
            assertTrue(msg.description().contains("20")); // newUnits
            assertTrue(msg.description().contains("0"));  // oldUnits
        }

        @Test
        @DisplayName("RESTOCKED — CUSTOMER and ADMIN produce distinct subjects")
        void restocked_RolesDiffer() {
            NotificationMessage admin = builder.buildForStockRestocked(NotificationRole.ADMIN, stockCtx());
            NotificationMessage customer = builder.buildForStockRestocked(NotificationRole.CUSTOMER, stockCtx());
            assertNotEquals(admin.subject(), customer.subject());
        }

        @Test
        @DisplayName("RESTOCKED — all four roles produce non-null messages")
        void restocked_AllRoles() {
            for (NotificationRole role : NotificationRole.values()) {
                NotificationMessage msg = builder.buildForStockRestocked(role, stockCtx());
                assertNotNull(msg, "Expected non-null for role " + role);
                assertFalse(msg.subject().isBlank());
            }
        }

        @Test
        @DisplayName("LOW_STOCK — MANAGER description contains new units and threshold")
        void lowStock_Manager_ContainsUnitsAndThreshold() {
            ShopStockMessageContext ctx = new ShopStockMessageContext(
                    "Tienda Centro", "SH-001", "Aceite de oliva 1L", "PRD-0042", 6, 3, 5, "manager.user");
            NotificationMessage msg = builder.buildForStockLow(NotificationRole.MANAGER, ctx);
            assertTrue(msg.description().contains("3"));  // newUnits
            assertTrue(msg.description().contains("5"));  // threshold
            assertTrue(msg.description().contains("Tienda Centro"));
        }

        @Test
        @DisplayName("LOW_STOCK — MANAGER and CUSTOMER produce distinct subjects")
        void lowStock_ManagerVsCustomer() {
            ShopStockMessageContext ctx = new ShopStockMessageContext(
                    "Tienda Centro", "SH-001", "Aceite", "PRD-0042", 6, 2, 5, "manager.user");
            NotificationMessage manager = builder.buildForStockLow(NotificationRole.MANAGER, ctx);
            NotificationMessage customer = builder.buildForStockLow(NotificationRole.CUSTOMER, ctx);
            assertNotEquals(manager.subject(), customer.subject());
        }

        @Test
        @DisplayName("LOW_STOCK — all four roles produce non-null messages")
        void lowStock_AllRoles() {
            for (NotificationRole role : NotificationRole.values()) {
                NotificationMessage msg = builder.buildForStockLow(role, stockCtx());
                assertNotNull(msg, "Expected non-null for role " + role);
                assertFalse(msg.subject().isBlank());
            }
        }

        @Test
        @DisplayName("buildForStock(RESTOCKED) produces the same result as buildForStockRestocked directly")
        void buildForStock_Restocked_DispatchesCorrectly() {
            NotificationMessage direct = builder.buildForStockRestocked(NotificationRole.ADMIN, stockCtx());
            NotificationMessage via = builder.buildForStock(ShopStockEvent.StockAction.RESTOCKED, NotificationRole.ADMIN, stockCtx());
            assertEquals(direct.subject(), via.subject());
            assertEquals(direct.description(), via.description());
        }

        @Test
        @DisplayName("buildForStock(LOW_STOCK) produces the same result as buildForStockLow directly")
        void buildForStock_LowStock_DispatchesCorrectly() {
            ShopStockMessageContext ctx = new ShopStockMessageContext(
                    "Tienda Centro", "SH-001", "Aceite", "PRD-0042", 6, 2, 5, "manager.user");
            NotificationMessage direct = builder.buildForStockLow(NotificationRole.MANAGER, ctx);
            NotificationMessage via = builder.buildForStock(ShopStockEvent.StockAction.LOW_STOCK, NotificationRole.MANAGER, ctx);
            assertEquals(direct.subject(), via.subject());
            assertEquals(direct.description(), via.description());
        }
    }

    // ==========================================
    // CROSS-CUTTING: No MongoDB ObjectId in any message
    // ==========================================
    @Test
    @DisplayName("No generated message for any entity/action/role contains a MongoDB-style hex ObjectId")
    void noMessageContainsMongoId() {
        for (NotificationRole role : NotificationRole.values()) {
            for (EventAction action : EventAction.values()) {
                assertNoMongoId(builder.buildForOrder(action, role, orderCtx()));
                assertNoMongoId(builder.buildForTruck(action, role, truckCtx()));
                assertNoMongoId(builder.buildForShop(action, role, shopCtx()));
                assertNoMongoId(builder.buildForProduct(action, role, productCtx()));
                assertNoMongoId(builder.buildForUser(action, role, userCtx()));
                assertNoMongoId(builder.buildForReview(action, role, reviewCtx()));
            }
            assertNoMongoId(builder.buildForStockRestocked(role, stockCtx()));
            assertNoMongoId(builder.buildForStockLow(role, stockCtx()));
        }
    }
}