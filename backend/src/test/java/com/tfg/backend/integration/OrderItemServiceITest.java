package com.tfg.backend.integration;

import com.tfg.backend.model.Order;
import com.tfg.backend.model.OrderItem;
import com.tfg.backend.model.Product;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.OrderItemRepository;
import com.tfg.backend.repository.OrderRepository;
import com.tfg.backend.repository.ProductRepository;
import com.tfg.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // Rolls back the MySQL database after each test
class OrderItemServiceITest {

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private User userA;
    private User userB;
    private Product productLaptop;
    private Product productMouse;

    @BeforeEach
    void setUpDatabase() {
        // 1. Create and save real Users
        userA = new User("Alice", "alice_test", "alice@mail.com", "pass", "USER");
        userB = new User("Bob", "bob_test", "bob@mail.com", "pass", "USER");
        userRepository.saveAll(List.of(userA, userB));

        // 2. Create and save real Products
        productLaptop = new Product("Laptop", "Gaming Laptop", 1500.0, 1000.0);
        productMouse = new Product("Mouse", "Wireless Mouse", 50.0, 20.0);
        productRepository.saveAll(List.of(productLaptop, productMouse));

        // 3. Create and save a real Order (represents a completed purchase)
        Order completedOrder = new Order();
        completedOrder.setUser(userA);
        completedOrder.setReferenceCode("REF-TEST-123");
        completedOrder.setTotalCost(1500.0);
        completedOrder.setSubtotalCost(1500.0);
        completedOrder.setShippingCost(0.0);
        completedOrder.setTotalDiscount(0.0);
        completedOrder.setTotalItems(1);

        orderRepository.save(completedOrder);

        // 4. Populate OrderItems (Cart vs Purchased)

        // User A - In Cart: 2 Laptops
        OrderItem cartItemA1 = new OrderItem(productLaptop, userA, 2);
        cartItemA1.setOrder(null); // Explicitly null for clarity
        orderItemRepository.save(cartItemA1);

        // User A - Purchased: 1 Laptop (Linked to the completed order)
        OrderItem purchasedItemA = new OrderItem(productLaptop, userA, 1);
        purchasedItemA.setOrder(completedOrder); // Not null!
        orderItemRepository.save(purchasedItemA);

        // User B - In Cart: 3 Laptops and 1 Mouse
        OrderItem cartItemB1 = new OrderItem(productLaptop, userB, 3);
        OrderItem cartItemB2 = new OrderItem(productMouse, userB, 1);
        orderItemRepository.saveAll(List.of(cartItemB1, cartItemB2));
    }

    @Test
    @DisplayName("findByUserIdAndOrderIsNull (List) retrieves only items currently in the user's cart")
    void testFindByUserIdAndOrderIsNull_List() {
        List<OrderItem> userACart = orderItemRepository.findByUserIdAndOrderIsNull(userA.getId());

        assertAll(
                () -> assertEquals(1, userACart.size(), "User A should have exactly 1 item line in the cart"),
                () -> assertNull(userACart.getFirst().getOrder(), "The retrieved item must not belong to an order"),
                () -> assertEquals(2, userACart.getFirst().getQuantity(), "Should retrieve the 2 laptops in cart, ignoring the 1 purchased laptop")
        );
    }

    @Test
    @DisplayName("findByUserIdAndOrderIsNull (Page) retrieves paginated cart items for a user")
    void testFindByUserIdAndOrderIsNull_Page() {
        Page<OrderItem> userBCartPage = orderItemRepository.findByUserIdAndOrderIsNull(userB.getId(), PageRequest.of(0, 10));

        assertAll(
                () -> assertEquals(2, userBCartPage.getTotalElements(), "User B should have 2 item lines in the cart"),
                () -> assertTrue(userBCartPage.getContent().stream().allMatch(item -> item.getOrder() == null), "All items must be in the cart phase")
        );
    }

    @Test
    @DisplayName("findByProductIdAndOrderIsNull retrieves all unpurchased units of a product across all users' carts")
    void testFindByProductIdAndOrderIsNull() {
        // Looking for Laptops stuck in carts (User A has 2, User B has 3)
        List<OrderItem> laptopsInCarts = orderItemRepository.findByProductIdAndOrderIsNull(productLaptop.getId());

        assertAll(
                () -> assertEquals(2, laptopsInCarts.size(), "There should be 2 cart lines containing the laptop"),
                () -> assertTrue(laptopsInCarts.stream().anyMatch(item -> item.getUser().getId().equals(userA.getId()))),
                () -> assertTrue(laptopsInCarts.stream().anyMatch(item -> item.getUser().getId().equals(userB.getId()))),
                () -> assertTrue(laptopsInCarts.stream().allMatch(item -> item.getOrder() == null), "Must only retrieve cart items")
        );
    }

    @Test
    @DisplayName("findByProductIdAndOrderIsNotNull retrieves only successfully purchased units of a product")
    void testFindByProductIdAndOrderIsNotNull() {
        // Looking for Laptops that are part of an actual Order
        List<OrderItem> purchasedLaptops = orderItemRepository.findByProductIdAndOrderIsNotNull(productLaptop.getId());

        assertAll(
                () -> assertEquals(1, purchasedLaptops.size(), "Only 1 line of laptops has been actually purchased"),
                () -> assertNotNull(purchasedLaptops.getFirst().getOrder(), "The retrieved item MUST belong to an order"),
                () -> assertEquals(userA.getId(), purchasedLaptops.getFirst().getUser().getId(), "It was purchased by User A"),
                () -> assertEquals(1, purchasedLaptops.getFirst().getQuantity(), "User A purchased exactly 1 unit")
        );
    }

    @Test
    @DisplayName("findByProductIdAndUserIdAndOrderIsNull safely checks if a specific user has a specific product in their cart")
    void testFindByProductIdAndUserIdAndOrderIsNull() {
        // 1. Existing cart item: User B has a Mouse in the cart
        Optional<OrderItem> userBMouse = orderItemRepository.findByProductIdAndUserIdAndOrderIsNull(productMouse.getId(), userB.getId());

        // 2. Non-existing cart item: User A does NOT have a Mouse in the cart
        Optional<OrderItem> userAMouse = orderItemRepository.findByProductIdAndUserIdAndOrderIsNull(productMouse.getId(), userA.getId());

        // 3. Purchased item check: User A bought a Laptop, but is it in the cart too? Yes, they have a separate cart line for it.
        Optional<OrderItem> userALaptop = orderItemRepository.findByProductIdAndUserIdAndOrderIsNull(productLaptop.getId(), userA.getId());

        assertAll(
                () -> assertTrue(userBMouse.isPresent(), "Should find the mouse in User B's cart"),
                () -> assertFalse(userAMouse.isPresent(), "Should NOT find a mouse in User A's cart"),
                () -> assertTrue(userALaptop.isPresent(), "Should find the laptop in User A's cart"),
                () -> assertEquals(2, userALaptop.get().getQuantity(), "Should specifically fetch the cart line (qty 2), ignoring the order line (qty 1)")
        );
    }
}