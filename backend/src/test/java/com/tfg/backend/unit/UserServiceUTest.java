package com.tfg.backend.unit;

import com.tfg.backend.dto.*;
import com.tfg.backend.event.UserEvent;
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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        @Test
        @DisplayName("getLoginInfo throws 500 if authenticated principal has no DB record")
        void getLoginInfo_ThrowsInternalServerError_WhenUserMissing() {
            when(authentication.getName()).thenReturn("ghost");
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getLoginInfo());
            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatusCode());
        }

        @Test
        @DisplayName("getLoginInfo throws NO_CONTENT when context has no authentication")
        void getLoginInfo_ThrowsNoContent_WhenAuthenticationIsNull() {
            when(securityContext.getAuthentication()).thenReturn(null);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getLoginInfo());
            assertEquals(HttpStatus.NO_CONTENT, ex.getStatusCode());
        }

        @Test
        @DisplayName("getLoginInfo throws NO_CONTENT when principal is anonymous")
        void getLoginInfo_ThrowsNoContent_WhenAnonymous() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getLoginInfo());
            assertEquals(HttpStatus.NO_CONTENT, ex.getStatusCode());
        }

        @Test
        @DisplayName("getLoggedUser returns the user when authenticated")
        void getLoggedUser_ReturnsUser_WhenAuthenticated() {
            Optional<User> result = userService.getLoggedUser();
            assertTrue(result.isPresent());
            assertEquals(loggedUser, result.get());
        }

        @Test
        @DisplayName("getLoggedUser returns empty when no authentication")
        void getLoggedUser_ReturnsEmpty_WhenAuthenticationIsNull() {
            when(securityContext.getAuthentication()).thenReturn(null);
            assertTrue(userService.getLoggedUser().isEmpty());
        }

        @Test
        @DisplayName("getLoggedUser returns empty when principal is anonymous")
        void getLoggedUser_ReturnsEmpty_WhenAnonymous() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            assertTrue(userService.getLoggedUser().isEmpty());
        }

        @Test
        @DisplayName("getLoggedUserUsername returns name when authenticated")
        void getLoggedUserUsername_ReturnsName() {
            assertEquals("testuser", userService.getLoggedUserUsername());
        }

        @Test
        @DisplayName("getLoggedUserUsername returns null when authentication is null")
        void getLoggedUserUsername_NullWhenAuthIsNull() {
            when(securityContext.getAuthentication()).thenReturn(null);
            assertNull(userService.getLoggedUserUsername());
        }

        @Test
        @DisplayName("getLoggedUserUsername returns null when principal is anonymous")
        void getLoggedUserUsername_NullWhenAnonymous() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            assertNull(userService.getLoggedUserUsername());
        }

        @Test
        @DisplayName("getLoggedUserRole returns first authority when authenticated")
        void getLoggedUserRole_ReturnsFirstAuthority() {
            Collection<? extends GrantedAuthority> auths = List.of(new SimpleGrantedAuthority("ADMIN"));
            doReturn(auths).when(authentication).getAuthorities();
            assertEquals("ADMIN", userService.getLoggedUserRole());
        }

        @Test
        @DisplayName("getLoggedUserRole returns null when authentication is null")
        void getLoggedUserRole_NullWhenAuthIsNull() {
            when(securityContext.getAuthentication()).thenReturn(null);
            assertNull(userService.getLoggedUserRole());
        }

        @Test
        @DisplayName("getLoggedUserRole returns null when principal is anonymous")
        void getLoggedUserRole_NullWhenAnonymous() {
            when(authentication.getPrincipal()).thenReturn("anonymousUser");
            assertNull(userService.getLoggedUserRole());
        }

        @Test
        @DisplayName("findUserHelper(Long) returns user when present")
        void findUserHelperById_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));
            assertEquals(loggedUser, userService.findUserHelper(1L));
        }

        @Test
        @DisplayName("findUserHelper(Long) throws NOT_FOUND when missing")
        void findUserHelperById_ThrowsNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.findUserHelper(99L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("findUserHelper(String) returns user when present")
        void findUserHelperByUsername_Success() {
            assertEquals(loggedUser, userService.findUserHelper("testuser"));
        }

        @Test
        @DisplayName("findUserHelper(String) throws NOT_FOUND when missing")
        void findUserHelperByUsername_ThrowsNotFound() {
            when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.findUserHelper("ghost"));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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

        @Test
        @DisplayName("updateLoggedUserData accepts new free username without collision")
        void updateLoggedUserData_AcceptsNewFreeUsername() {
            UserDTO dto = new UserDTO();
            dto.setName("Renamed");
            dto.setUsername("freshUsername");
            dto.setEmail("e@m.com");
            dto.setPhone("0");
            when(userRepository.existsByUsername("freshUsername")).thenReturn(false);

            User result = userService.updateLoggedUserData(dto);

            assertEquals("freshUsername", result.getUsername());
        }

        @Test
        @DisplayName("registerUser throws UNAUTHORIZED when email is already taken")
        void registerUser_ThrowsUnauthorized_WhenEmailTaken() {
            UserSignupDTO dto = new UserSignupDTO();
            dto.setUsername("freshUser");
            dto.setEmail("taken@mail.com");

            when(userRepository.existsByUsername("freshUser")).thenReturn(false);
            when(userRepository.existsByEmail("taken@mail.com")).thenReturn(true);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.registerUser(dto));
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
        }

        @Test
        @DisplayName("registerUser uses the provided role when not empty")
        void registerUser_UsesProvidedRole() {
            UserSignupDTO dto = new UserSignupDTO();
            dto.setName("Mary");
            dto.setUsername("mary123");
            dto.setEmail("mary@mail.com");
            dto.setPassword("pwd");
            dto.setRole("MANAGER");

            when(passwordEncoder.encode("pwd")).thenReturn("enc");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User result = userService.registerUser(dto);

            assertTrue(result.getRoles().contains("MANAGER"));
            verify(eventPublisher).publishEvent(any(UserEvent.class));
        }

        @Test
        @DisplayName("registerUser defaults to USER role when role is null")
        void registerUser_DefaultsToUser_WhenRoleNull() {
            UserSignupDTO dto = new UserSignupDTO();
            dto.setName("Nina");
            dto.setUsername("nina");
            dto.setEmail("nina@m.com");
            dto.setPassword("p");
            dto.setRole(null);

            when(passwordEncoder.encode("p")).thenReturn("e");
            when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

            User result = userService.registerUser(dto);
            assertTrue(result.getRoles().contains("USER"));
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

        @Test
        @DisplayName("applyShopSelection accepts a null shop and skips clearing when cart is empty")
        void applyShopSelection_NullShopAndEmptyCart() {
            boolean result = userService.applyShopSelection(loggedUser, null);

            assertTrue(result);
            assertNull(loggedUser.getSelectedShop());
            assertTrue(loggedUser.getAllOrderItems().isEmpty());
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

        @Test
        @DisplayName("editAddress updates fields when the ID exists")
        void editAddress_Success() {
            Address addr = new Address();
            addr.setId(7L);
            addr.setAlias("Old");
            loggedUser.getAddresses().add(addr);

            AddressDTO dto = new AddressDTO();
            dto.setId(7L);
            dto.setAlias("New");
            dto.setStreet("Mayor");
            dto.setNumber("1");
            dto.setFloor("2");
            dto.setPostalCode("28001");
            dto.setCity("Madrid");
            dto.setCountry("Spain");

            userService.editAddress(dto);

            assertEquals("New", addr.getAlias());
            assertEquals("Mayor", addr.getStreet());
            assertEquals("Madrid", addr.getCity());
            assertEquals("Spain", addr.getCountry());
        }

        @Test
        @DisplayName("deleteAddress removes the address when present")
        void deleteAddress_Success() {
            Address addr = new Address();
            addr.setId(3L);
            loggedUser.getAddresses().add(addr);

            userService.deleteAddress(3L);

            assertTrue(loggedUser.getAddresses().isEmpty());
        }

        @Test
        @DisplayName("deleteAddress throws NOT_FOUND when ID does not exist")
        void deleteAddress_ThrowsNotFound() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.deleteAddress(404L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("createPaymentCard parses MM/yy and adds the card to the list")
        void createPaymentCard_Success() {
            PaymentCardDTO dto = new PaymentCardDTO();
            dto.setAlias("My Card");
            dto.setCardOwnerName("Owner");
            dto.setNumber("4000111122223333");
            dto.setCvv("123");
            dto.setDueDate("11/30");

            userService.createPaymentCard(dto);

            assertEquals(1, loggedUser.getCards().size());
            assertEquals(YearMonth.of(2030, 11), loggedUser.getCards().getFirst().getDueDate());
        }

        @Test
        @DisplayName("editPaymentCard throws NOT_FOUND when the card ID does not exist")
        void editPaymentCard_ThrowsNotFound() {
            PaymentCardDTO dto = new PaymentCardDTO();
            dto.setId(999L);
            dto.setDueDate("12/30");

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.editPaymentCard(dto));
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

        @Test
        @DisplayName("deleteUserImage is a no-op when the user already has the default image")
        void deleteUserImage_NoOpWhenAlreadyDefault() {
            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(true);

                userService.deleteUserImage();

                verify(imageService, never()).deleteFile(any());
            }
        }

        @Test
        @DisplayName("uploadUserImage throws NOT_FOUND when user does not exist")
        void uploadUserImage_ThrowsNotFound_WhenUserMissing() {
            MultipartFile file = new MockMultipartFile("f", new byte[0]);
            when(userRepository.findById(404L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.uploadUserImage(404L, file));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
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

        @Test
        @DisplayName("deleteUserById deletes a basic USER without unlinking shops/trucks")
        void deleteUserById_BasicUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));

            userService.deleteUserById(1L);

            verify(userRepository).delete(loggedUser);
            verify(eventPublisher).publishEvent(any(UserEvent.class));
        }

        @Test
        @DisplayName("deleteUserById throws NOT_FOUND when the user does not exist")
        void deleteUserById_ThrowsNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.deleteUserById(999L));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(userRepository, never()).delete(any(User.class));
        }

        @Test
        @DisplayName("toggleUserBanById flips the ban flag and publishes a UserEvent")
        void toggleUserBanById_BansUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));

            User result = userService.toggleUserBanById(1L, true);

            assertTrue(result.isBanned());
            verify(eventPublisher).publishEvent(any(UserEvent.class));
        }

        @Test
        @DisplayName("anonymizeLoggedUser anonymizes the current logged user and publishes UserEvent")
        void anonymizeLoggedUser_Success() {
            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(true);
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                User result = userService.anonymizeLoggedUser();

                assertTrue(result.isDeleted());
                assertTrue(result.getUsername().startsWith("deleteduser_"));
                verify(eventPublisher).publishEvent(any(UserEvent.class));
            }
        }

        @Test
        @DisplayName("anonAllUsers anonymizes non-admin users only")
        void anonAllUsers_SkipsAdmins() {
            when(userRepository.findAll()).thenReturn(List.of(loggedUser, adminUser));

            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(true);
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                boolean result = userService.anonAllUsers();

                assertTrue(result);
                assertTrue(loggedUser.isDeleted(), "Non-admin user must be anonymized");
                assertFalse(adminUser.isDeleted(), "Admin user must be preserved");
                verify(eventPublisher, times(1)).publishEvent(any(UserEvent.class));
            }
        }

        @Test
        @DisplayName("anonUserById anonymizes the targeted user")
        void anonUserById_Success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));

            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(true);
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                User result = userService.anonUserById(1L);

                assertTrue(result.isDeleted());
                verify(eventPublisher).publishEvent(any(UserEvent.class));
            }
        }

        @Test
        @DisplayName("deleteAllUsers triggers hard-delete for non-admins and skips admins")
        void deleteAllUsers_SkipsAdmins() {
            when(userRepository.findAll()).thenReturn(List.of(loggedUser, adminUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));

            boolean result = userService.deleteAllUsers();

            assertTrue(result);
            verify(userRepository).delete(loggedUser);
            verify(userRepository, never()).delete(adminUser);
        }

        @Test
        @DisplayName("anonymizeUser leaves the image untouched when it is already the default")
        void anonymizeUser_DoesNotDeleteDefaultImage() {
            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(true);
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                userService.anonymizeUser(loggedUser);

                verify(imageService, never()).deleteFile(any());
                assertTrue(loggedUser.isDeleted());
            }
        }

        @Test
        @DisplayName("anonymizeUser handles a null userImage without errors")
        void anonymizeUser_HandlesNullUserImage() {
            loggedUser.setUserImage(null);

            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                userService.anonymizeUser(loggedUser);

                verify(imageService, never()).deleteFile(any());
                assertTrue(loggedUser.isDeleted());
            }
        }

        @Test
        @DisplayName("anonymizeUser handles a userImage with null S3 key without deleting")
        void anonymizeUser_HandlesNullS3Key() {
            loggedUser.getUserImage().setS3Key(null);

            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                userService.anonymizeUser(loggedUser);

                verify(imageService, never()).deleteFile(any());
                assertTrue(loggedUser.isDeleted());
            }
        }

        @Test
        @DisplayName("anonymizeUser handles null registeredOrders without errors")
        void anonymizeUser_HandlesNullRegisteredOrders() {
            loggedUser.setRegisteredOrders(null);

            try (MockedStatic<GlobalDefaults> mocked = mockStatic(GlobalDefaults.class)) {
                mocked.when(() -> GlobalDefaults.isDefaultUserImage(any())).thenReturn(true);
                mocked.when(GlobalDefaults::getDefaultUserImage).thenReturn(new ImageInfo());

                userService.anonymizeUser(loggedUser);

                assertTrue(loggedUser.isDeleted());
            }
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

    // --- NOTIFICATION HELPERS AND THIN REPOSITORY DELEGATIONS ---
    @Nested
    @DisplayName("Tests for Notification Helpers and Repository Delegations")
    class NotificationAndDelegationTests {

        @Test
        @DisplayName("getUsernamesBySelectedShop returns empty list when shopId is null")
        void getUsernamesBySelectedShop_NullId_ReturnsEmpty() {
            assertTrue(userService.getUsernamesBySelectedShop(null).isEmpty());
            verify(userRepository, never()).findUsernamesBySelectedShopId(any());
        }

        @Test
        @DisplayName("getUsernamesBySelectedShop delegates to repository when shopId is provided")
        void getUsernamesBySelectedShop_DelegatesToRepository() {
            when(userRepository.findUsernamesBySelectedShopId(10L)).thenReturn(List.of("a", "b"));
            assertEquals(List.of("a", "b"), userService.getUsernamesBySelectedShop(10L));
        }

        @Test
        @DisplayName("getUsernamesByFavoritedProduct returns empty list when productId is null")
        void getUsernamesByFavoritedProduct_NullId_ReturnsEmpty() {
            assertTrue(userService.getUsernamesByFavoritedProduct(null).isEmpty());
            verify(userRepository, never()).findUsernamesByFavouriteProductId(any());
        }

        @Test
        @DisplayName("getUsernamesByFavoritedProduct delegates to repository when productId is provided")
        void getUsernamesByFavoritedProduct_DelegatesToRepository() {
            when(userRepository.findUsernamesByFavouriteProductId(5L)).thenReturn(List.of("alice"));
            assertEquals(List.of("alice"), userService.getUsernamesByFavoritedProduct(5L));
        }

        @Test
        @DisplayName("getUsernamesWithProductInCart returns empty list when productId is null")
        void getUsernamesWithProductInCart_NullId_ReturnsEmpty() {
            assertTrue(userService.getUsernamesWithProductInCart(null).isEmpty());
            verify(userRepository, never()).findUsernamesByProductInCart(any());
        }

        @Test
        @DisplayName("getUsernamesWithProductInCart delegates to repository when productId is provided")
        void getUsernamesWithProductInCart_DelegatesToRepository() {
            when(userRepository.findUsernamesByProductInCart(5L)).thenReturn(List.of("bob"));
            assertEquals(List.of("bob"), userService.getUsernamesWithProductInCart(5L));
        }

        @Test
        @DisplayName("findAvailableDrivers delegates to repository with DRIVER role filter")
        void findAvailableDrivers_DelegatesToRepository() {
            when(userRepository.findByRolesContainingAndAssignedTruckIsNull("DRIVER")).thenReturn(List.of(loggedUser));
            assertEquals(List.of(loggedUser), userService.findAvailableDrivers());
        }

        @Test
        @DisplayName("findAllByRole delegates to repository with the given role")
        void findAllByRole_DelegatesToRepository() {
            when(userRepository.findByRolesContaining("MANAGER")).thenReturn(List.of(loggedUser));
            assertEquals(List.of(loggedUser), userService.findAllByRole("MANAGER"));
        }

        @Test
        @DisplayName("getAllAdminUsernames returns usernames whose role is ADMIN")
        void getAllAdminUsernames_DelegatesToRepository() {
            when(userRepository.findUsernamesByRole("ADMIN")).thenReturn(List.of("admin"));
            assertEquals(List.of("admin"), userService.getAllAdminUsernames());
        }

        @Test
        @DisplayName("save delegates to repository.save")
        void save_DelegatesToRepository() {
            when(userRepository.save(loggedUser)).thenReturn(loggedUser);
            assertEquals(loggedUser, userService.save(loggedUser));
        }

        @Test
        @DisplayName("saveAll delegates to repository.saveAll")
        void saveAll_DelegatesToRepository() {
            List<User> users = List.of(loggedUser, adminUser);
            when(userRepository.saveAll(users)).thenReturn(users);
            assertEquals(users, userService.saveAll(users));
        }

        @Test
        @DisplayName("delete delegates to repository.delete")
        void delete_DelegatesToRepository() {
            userService.delete(loggedUser);
            verify(userRepository).delete(loggedUser);
        }

        @Test
        @DisplayName("existence and status flags delegate to the corresponding repository methods")
        void existenceFlags_DelegateToRepository() {
            when(userRepository.existsByUsername("u")).thenReturn(true);
            when(userRepository.existsByEmail("e@m.com")).thenReturn(true);
            when(userRepository.existsByUsernameAndIsBannedTrue("u")).thenReturn(true);
            when(userRepository.existsByUsernameAndIsDeletedTrue("u")).thenReturn(true);
            when(userRepository.existsByEmailAndIsBannedTrue("e@m.com")).thenReturn(true);
            when(userRepository.existsByEmailAndIsDeletedTrue("e@m.com")).thenReturn(true);

            assertAll(
                    () -> assertTrue(userService.existsByUsername("u")),
                    () -> assertTrue(userService.existsByEmail("e@m.com")),
                    () -> assertTrue(userService.isUsernameTaken("u")),
                    () -> assertTrue(userService.isEmailTaken("e@m.com")),
                    () -> assertTrue(userService.isBannedByUsername("u")),
                    () -> assertTrue(userService.isDeletedByUsername("u")),
                    () -> assertTrue(userService.isBannedByEmail("e@m.com")),
                    () -> assertTrue(userService.isDeletedByEmail("e@m.com"))
            );
        }

        @Test
        @DisplayName("getUsernamesByFavoritedProductAndSelectedShop returns empty list when either ID is null")
        void getUsernamesByFavoritedProductAndSelectedShop_NullId_ReturnsEmpty() {
            assertTrue(userService.getUsernamesByFavoritedProductAndSelectedShop(null, 5L).isEmpty());
            assertTrue(userService.getUsernamesByFavoritedProductAndSelectedShop(10L, null).isEmpty());
            verify(userRepository, never()).findUsernamesByFavoritedProductAndSelectedShop(any(), any());
        }

        @Test
        @DisplayName("getUsernamesByFavoritedProductAndSelectedShop delegates to repository when both IDs are present")
        void getUsernamesByFavoritedProductAndSelectedShop_DelegatesToRepository() {
            when(userRepository.findUsernamesByFavoritedProductAndSelectedShop(10L, 5L)).thenReturn(List.of("alice", "bob"));
            assertEquals(List.of("alice", "bob"), userService.getUsernamesByFavoritedProductAndSelectedShop(10L, 5L));
        }

        @Test
        @DisplayName("getUsernamesBySelectedShopAndProductInFavoritesOrCart returns empty list when either ID is null")
        void getUsernamesBySelectedShopAndProductInFavoritesOrCart_NullId_ReturnsEmpty() {
            assertTrue(userService.getUsernamesBySelectedShopAndProductInFavoritesOrCart(null, 5L).isEmpty());
            assertTrue(userService.getUsernamesBySelectedShopAndProductInFavoritesOrCart(10L, null).isEmpty());
            verify(userRepository, never()).findUsernamesBySelectedShopAndProductInFavoritesOrCart(any(), any());
        }

        @Test
        @DisplayName("getUsernamesBySelectedShopAndProductInFavoritesOrCart delegates to repository when both IDs are present")
        void getUsernamesBySelectedShopAndProductInFavoritesOrCart_DelegatesToRepository() {
            when(userRepository.findUsernamesBySelectedShopAndProductInFavoritesOrCart(10L, 5L)).thenReturn(List.of("charlie"));
            assertEquals(List.of("charlie"), userService.getUsernamesBySelectedShopAndProductInFavoritesOrCart(10L, 5L));
        }

        @Test
        @DisplayName("findAll/findById/findByUsername/findByEmail delegate to the repository")
        void simpleFinders_DelegateToRepository() {
            when(userRepository.findAll()).thenReturn(List.of(loggedUser));
            when(userRepository.findById(1L)).thenReturn(Optional.of(loggedUser));
            when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(loggedUser));

            assertAll(
                    () -> assertEquals(List.of(loggedUser), userService.findAll()),
                    () -> assertEquals(Optional.of(loggedUser), userService.findById(1L)),
                    () -> assertEquals(Optional.of(loggedUser), userService.findByUsername("testuser")),
                    () -> assertEquals(Optional.of(loggedUser), userService.findByEmail("test@email.com"))
            );
        }
    }
}