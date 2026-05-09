package com.tfg.backend.unit;

import com.tfg.backend.dto.*;
import com.tfg.backend.model.*;
import com.tfg.backend.model.Order;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.service.UserService;
import com.tfg.backend.utils.GlobalDefaults;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUTest {

    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ImageService imageService;
    @Mock private UserRepository userRepository;

    @Mock private Authentication authentication;
    @Mock private SecurityContext securityContext;
    @Mock private ApplicationEventPublisher eventPublisher; //Necessary to avoid errors trying to send notifications, but not used

    @InjectMocks
    private UserService userService;

    private User loggedUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Prepare Logged User with all mutable collections initialized
        loggedUser = new User();
        loggedUser.setId(1L);
        loggedUser.setUsername("testuser");
        loggedUser.setEmail("test@email.com");
        loggedUser.setRoles(new HashSet<>(List.of("USER")));
        loggedUser.setAddresses(new ArrayList<>());
        loggedUser.setCards(new ArrayList<>());
        loggedUser.setAllOrderItems(new ArrayList<>());
        loggedUser.setRegisteredOrders(new HashSet<>());
        loggedUser.setAssignedShops(new ArrayList<>());

        ImageInfo img = new ImageInfo();
        img.setS3Key("user-pic.jpg");
        loggedUser.setUserImage(img);

        adminUser = new User();
        adminUser.setId(99L);
        adminUser.setUsername("admin");
        adminUser.setRoles(new HashSet<>(List.of("ADMIN")));

        // Mock Security Context by default for methods requiring loggedUser
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.isAuthenticated()).thenReturn(true);
        lenient().when(authentication.getName()).thenReturn("testuser");
        lenient().when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(loggedUser));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // --- SESSION & SECURITY TESTS ---
    @Nested
    @DisplayName("Tests for Session Context and Helpers")
    class SessionAndHelperTests {

        @Test
        @DisplayName("getLoginInfo returns DTO if authenticated")
        void getLoginInfo_Success() {
            UserLoginDTO dto = userService.getLoginInfo();
            assertEquals("testuser", dto.getUsername());
        }

        @Test
        @DisplayName("getLoginInfo throws NO_CONTENT if not authenticated")
        void getLoginInfo_ThrowsNoContent_WhenNotAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(false);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getLoginInfo());
            assertEquals(HttpStatus.NO_CONTENT, ex.getStatusCode());
        }

        @Test
        @DisplayName("findLoggedUserHelper throws UNAUTHORIZED if completely unauthenticated (empty context)")
        void findLoggedUserHelper_ThrowsUnauthorized() {
            SecurityContextHolder.clearContext(); // Remove simulated session

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.findLoggedUserHelper());
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }
    }

    // --- REGISTRATION & PROFILE UPDATES ---
    @Nested
    @DisplayName("Tests for Registration and Profile logic")
    class RegistrationAndProfileTests {

        @Test
        @DisplayName("registerUser throws UNAUTHORIZED if username or email exists")
        void registerUser_ThrowsUnauthorized_WhenDuplicate() {
            UserSignupDTO dto = new UserSignupDTO();
            dto.setUsername("existing");
            dto.setEmail("existing@mail.com");

            when(userRepository.existsByUsername("existing")).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.registerUser(dto));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            assertEquals("This username or email is already taken.", ex.getReason());
        }

        @Test
        @DisplayName("registerUser encodes password and saves with default USER role")
        void registerUser_Success() {
            UserSignupDTO dto = new UserSignupDTO();
            dto.setName("John");
            dto.setUsername("john123");
            dto.setEmail("john@mail.com");
            dto.setPassword("rawPassword");
            dto.setRole(""); // Empty role defaults to "USER"

            when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User result = userService.registerUser(dto);

            assertEquals("john123", result.getUsername());
            assertEquals("encodedPassword", result.getEncodedPassword());
            assertTrue(result.getRoles().contains("USER"), "Role must default to USER");
        }

        @Test
        @DisplayName("updateLoggedUserData throws FORBIDDEN if new username is taken by another user")
        void updateLoggedUserData_ThrowsForbidden_WhenNewUsernameTaken() {
            UserDTO dto = new UserDTO();
            dto.setUsername("takenUser");

            when(userRepository.existsByUsername("takenUser")).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.updateLoggedUserData(dto));
            assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        }

        @Test
        @DisplayName("updateLoggedUserData applies changes correctly")
        void updateLoggedUserData_Success() {
            UserDTO dto = new UserDTO();
            dto.setName("New Name");
            dto.setUsername("testuser"); // Same username, no collision check needed
            dto.setEmail("new@mail.com");
            dto.setPhone("123456789");

            User result = userService.updateLoggedUserData(dto);

            assertEquals("New Name", result.getName());
            assertEquals("new@mail.com", result.getEmail());
            assertEquals("123456789", result.getPhone());
        }
    }

    // --- SHOP SELECTION ---
    @Nested
    @DisplayName("Tests for Shop Selection")
    class ShopSelectionTests {

        @Test
        @DisplayName("applyShopSelection updates shop and clears current cart items")
        void applyShopSelection_Success() {
            Shop newShop = new Shop();
            newShop.setId(10L);

            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null); // Indicates it's in the cart
            cartItem.setUser(loggedUser);

            OrderItem purchasedItem = new OrderItem();
            purchasedItem.setOrder(new Order()); // Already purchased
            purchasedItem.setUser(loggedUser);

            loggedUser.getAllOrderItems().addAll(List.of(cartItem, purchasedItem));

            boolean result = userService.applyShopSelection(loggedUser, newShop);

            assertTrue(result);
            assertEquals(newShop, loggedUser.getSelectedShop());
            assertFalse(loggedUser.getAllOrderItems().contains(cartItem), "Cart items must be cleared upon changing shop");
            assertTrue(loggedUser.getAllOrderItems().contains(purchasedItem), "Purchased items must be kept");
        }
    }

    // --- ADDRESS AND CARD OPERATIONS ---
    @Nested
    @DisplayName("Tests for Addresses and Payment Cards")
    class AddressAndCardTests {

        @Test
        @DisplayName("createAddress maps DTO and adds to user list")
        void createAddress_Success() {
            AddressDTO dto = new AddressDTO();
            dto.setAlias("Home");
            dto.setCity("Madrid");

            userService.createAddress(dto);

            assertEquals(1, loggedUser.getAddresses().size());
            assertEquals("Home", loggedUser.getAddresses().getFirst().getAlias());
        }

        @Test
        @DisplayName("editAddress throws NOT_FOUND if ID is missing")
        void editAddress_ThrowsNotFound() {
            AddressDTO dto = new AddressDTO();
            dto.setId(99L);

            assertThrows(ResponseStatusException.class, () -> userService.editAddress(dto));
        }

        @Test
        @DisplayName("editPaymentCard updates fields including YearMonth parsing")
        void editPaymentCard_Success() {
            PaymentCard card = new PaymentCard();
            card.setId(5L);
            card.setAlias("Old");
            loggedUser.getCards().add(card);

            PaymentCardDTO dto = new PaymentCardDTO();
            dto.setId(5L);
            dto.setAlias("New");
            dto.setDueDate("12/28"); // MM/yy format

            userService.editPaymentCard(dto);

            assertEquals("New", loggedUser.getCards().getFirst().getAlias());
            assertEquals(YearMonth.of(2028, 12), loggedUser.getCards().getFirst().getDueDate());
        }

        @Test
        @DisplayName("deletePaymentCard removes card if exists or throws NOT_FOUND")
        void deletePaymentCard_SuccessAndFail() {
            PaymentCard card = new PaymentCard();
            card.setId(5L);
            loggedUser.getCards().add(card);

            userService.deletePaymentCard(5L);
            assertTrue(loggedUser.getCards().isEmpty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.deletePaymentCard(99L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }
    }

    // --- IMAGE METHODS ---
    @Nested
    @DisplayName("Tests for Image Handlers")
    class ImageHandlingTests {

        @Test
        @DisplayName("deleteUserImage removes custom S3 file and applies default")
        void deleteUserImage_Success() {
            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(false);

                ImageInfo defaultImg = new ImageInfo();
                defaultImg.setS3Key("default.jpg");
                mockedDefaults.when(GlobalDefaults::getDefaultUserImage).thenReturn(defaultImg);

                userService.deleteUserImage();

                verify(imageService).deleteFile("user-pic.jpg");
                assertEquals("default.jpg", loggedUser.getUserImage().getS3Key());
            }
        }

        @Test
        @DisplayName("uploadUserImage delegates correctly to ImageService")
        void uploadUserImage_Success() {
            MultipartFile file = new MockMultipartFile("f", new byte[0]);
            ImageInfo newImg = new ImageInfo();
            newImg.setS3Key("new.jpg");

            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));
            when(imageService.processImageReplacement(any(), eq(file), eq("users"), any(), any())).thenReturn(newImg);

            userService.uploadUserImage(1L, file);

            assertEquals("new.jpg", loggedUser.getUserImage().getS3Key());
        }
    }

    // --- BANNING AND DELETION (CORE BUSINESS LOGIC) ---
    @Nested
    @DisplayName("Tests for Banning, Anonymization and Hard Deletion")
    class BanAndDeletionTests {

        @Test
        @DisplayName("toggleAllUsersBan bans users but skips ADMINS")
        void toggleAllUsersBan_SkipsAdmins() {
            when(userRepository.findAll()).thenReturn(List.of(loggedUser, adminUser));

            userService.toggleAllUsersBan(true);

            assertTrue(loggedUser.isBanned());
            assertFalse(adminUser.isBanned(), "Admin users cannot be mass-banned");
        }

        @Test
        @DisplayName("anonymizeUser scrambles identity, clears collections and addresses but keeps historical orders")
        void anonymizeUser_Success() {
            // Setup data to be destroyed
            loggedUser.setPhone("555-555-555");
            loggedUser.setOtpCode("1234");

            Address orderAddr = new Address();
            orderAddr.setStreet("Street");
            Order order = new Order();
            order.setFullSendingAddress(orderAddr);
            loggedUser.getRegisteredOrders().add(order);

            loggedUser.getAddresses().add(new Address());
            loggedUser.getCards().add(new PaymentCard());

            OrderItem cartItem = new OrderItem();
            cartItem.setOrder(null);
            loggedUser.getAllOrderItems().add(cartItem);

            try (MockedStatic<GlobalDefaults> mockedDefaults = mockStatic(GlobalDefaults.class)) {
                mockedDefaults.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(false);
                mockedDefaults.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                // Execute
                User result = userService.anonymizeUser(loggedUser);

                // Assert GDPR erasure
                assertTrue(result.getName().startsWith("Usuario eliminado"));
                assertTrue(result.getUsername().startsWith("deleteduser_"));
                assertEquals("", result.getEncodedPassword());
                assertNull(result.getPhone());
                assertNull(result.getOtpCode());
                assertTrue(result.isDeleted());

                // Assert collections clearing
                assertTrue(result.getAddresses().isEmpty(), "Addresses must be cleared");
                assertTrue(result.getCards().isEmpty(), "Cards must be cleared");
                assertTrue(result.getAllOrderItems().isEmpty(), "Cart items must be cleared");

                // Assert Historical Order Privacy
                assertNull(order.getFullSendingAddress(), "Order physical sending address must be destroyed for GDPR");

                // Assert Image
                verify(imageService).deleteFile("user-pic.jpg");
            }
        }

        @Test
        @DisplayName("deleteUserById cascades unlinking for Managers and Drivers")
        void deleteUserById_CascadesUnlinking() {
            // Setup as Manager
            loggedUser.getRoles().add("MANAGER");
            Shop shop = new Shop();
            shop.setAssignedManager(loggedUser);
            loggedUser.getAssignedShops().add(shop);

            // Setup as Driver
            loggedUser.getRoles().add("DRIVER");
            Truck truck = new Truck();
            truck.setAssignedDriver(loggedUser);
            loggedUser.setAssignedTruck(truck);

            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));

            userService.deleteUserById(1L);

            // Assert Unlinking
            assertNull(shop.getAssignedManager(), "Shop must lose its manager");
            assertTrue(loggedUser.getAssignedShops().isEmpty());

            assertNull(truck.getAssignedDriver(), "Truck must lose its driver");
            assertNull(loggedUser.getAssignedTruck());

            // Assert Hard Delete
            verify(userRepository).delete(loggedUser);
        }
    }

    // --- METRICS TESTS ---
    @Nested
    @DisplayName("Tests for Users Statistics")
    class MetricsTests {

        @Test
        @DisplayName("getUsersStats queries repository and builds correct list")
        void getUsersStats_Success() {
            when(userRepository.count()).thenReturn(100L);
            when(userRepository.countByIsBannedTrue()).thenReturn(5L);
            when(userRepository.countByIsDeletedTrue()).thenReturn(10L);
            when(userRepository.countByRolesContaining("ADMIN")).thenReturn(2L);
            when(userRepository.countByRolesContaining("MANAGER")).thenReturn(3L);
            when(userRepository.countByRolesContaining("DRIVER")).thenReturn(5L); // Total internals = 10L

            List<StatDTO> stats = userService.getUsersStats();

            assertEquals(4, stats.size());
            assertEquals("Totales", stats.get(0).label());
            assertEquals(100L, stats.get(0).value());
            assertEquals("Cuentas Internas", stats.get(3).label());
            assertEquals(10L, stats.get(3).value()); // 2 + 3 + 5
        }
    }
}