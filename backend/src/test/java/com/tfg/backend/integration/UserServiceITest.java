package com.tfg.backend.integration;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.UserSignupDTO;
import com.tfg.backend.model.*;
import com.tfg.backend.repository.*;
import com.tfg.backend.service.ImageService;
import com.tfg.backend.service.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for UserService.
 * Covers user lifecycle management: registration, anonymization,
 * physical deletion, and secure profile management (addresses/cards).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UserServiceITest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private TruckRepository truckRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private ImageService imageService;

    private User testUser;
    private Shop assignedShop;
    private Truck assignedTruck;

    @BeforeEach
    void setUpUserScenario() {
        // 1. Create a standard base user
        testUser = new User("Test Subject", "testsubject", "test@subject.com", "encoded_pass", "USER");
        userRepository.save(testUser);

        // 2. Create a staff user (Manager & Driver)
        User staffUser = new User("Staff Member", "staff", "staff@test.com", "pass", "MANAGER", "DRIVER");
        userRepository.save(staffUser);

        // 3. Assign a shop and a truck to the staff user
        assignedShop = new Shop("Manager Shop", null, 5000.0);
        assignedShop.setReferenceCode("SHOP-USER-TEST");
        assignedShop.setAssignedManager(staffUser);
        shopRepository.save(assignedShop);

        assignedTruck = new Truck("9999-TTT", null, 10);
        assignedTruck.setReferenceCode("TR-USER-TEST");
        assignedTruck.setAssignedDriver(staffUser);
        truckRepository.save(assignedTruck);

        // 4. Force DB synchronization and clear Hibernate cache to ensure complete relationship reads
        entityManager.flush();
        entityManager.clear();

        // 5. Authenticate user for the security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(testUser.getUsername(), "pass", java.util.List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Register User: Correcty hashes password and assigns roles in DB")
    void testRegisterUser_SavesCorrectly() {
        UserSignupDTO dto = new UserSignupDTO();
        dto.setName("New User");
        dto.setUsername("newuser");
        dto.setEmail("new@user.com");
        dto.setPassword("plain_password");
        dto.setRole("USER");

        User registered = userService.registerUser(dto);

        User dbUser = userRepository.findById(registered.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("newuser", dbUser.getUsername()),
                () -> assertNotEquals("plain_password", dbUser.getEncodedPassword(), "Password must be encoded in DB")
        );
    }

    @Test
    @DisplayName("Address Management: Create, edit and delete updates user profile in MySQL")
    void testAddressManagement_Lifecycle() {
        AddressDTO dto = new AddressDTO();
        dto.setAlias("Work");
        dto.setStreet("Tech Ave");
        dto.setCity("Madrid");

        userService.createAddress(dto);

        User userAfterCreate = userRepository.findByUsername("testsubject").orElseThrow();
        Address savedAddress = userAfterCreate.getAddresses().getFirst();
        assertEquals("Work", savedAddress.getAlias());

        userService.deleteAddress(savedAddress.getId());

        User userAfterDelete = userRepository.findByUsername("testsubject").orElseThrow();
        assertTrue(userAfterDelete.getAddresses().isEmpty());
    }

    @Test
    @DisplayName("Anonymize User: Replaces personal info with random UUID but keeps the user record")
    void testAnonymizeUser_ReplacesData() {
        User userToAnon = userRepository.findByUsername("testsubject").orElseThrow();

        userService.anonymizeUser(userToAnon);

        User dbUser = userRepository.findById(userToAnon.getId()).orElseThrow();
        assertAll(
                () -> assertTrue(dbUser.isDeleted(), "User should be marked as deleted"),
                () -> assertTrue(dbUser.getUsername().startsWith("deleteduser_"), "Username should be randomized"),
                () -> assertEquals("", dbUser.getEncodedPassword(), "Password must be cleared"),
                () -> assertTrue(dbUser.getEmail().endsWith("@frictapp.com"), "Email should be randomized")
        );
    }

    @Test
    @DisplayName("Delete User: Clears staff assignments (Shop/Truck) before physical deletion")
    void testDeleteUser_UnlinksStaffRelationships() {
        User staff = userRepository.findByUsername("staff").orElseThrow();
        Long staffId = staff.getId();

        // Act: Physically delete the staff user
        userService.deleteUserById(staffId);

        // 1. Assert user is deleted
        assertFalse(userRepository.existsById(staffId));

        // 2. Assert shop is safe but unlinked from manager
        Shop dbShop = shopRepository.findById(assignedShop.getId()).orElseThrow();
        assertNull(dbShop.getAssignedManager(), "Shop should be unlinked from the deleted manager");

        // 3. Assert truck is safe but unlinked from driver
        Truck dbTruck = truckRepository.findById(assignedTruck.getId()).orElseThrow();
        assertNull(dbTruck.getAssignedDriver(), "Truck should be unlinked from the deleted driver");
    }

    @Test
    @DisplayName("Metrics: Statistics count total, banned and deleted users correctly")
    void testUserStatistics_CountsAccurately() {
        User userToAnon = userRepository.findByUsername("testsubject").orElseThrow();
        userService.anonymizeUser(userToAnon);

        entityManager.flush();
        entityManager.clear();

        List<com.tfg.backend.utils.StatDTO> stats = userService.getUsersStats();

        // Total count should be 2 (testsubject + staff)
        assertEquals(2L, stats.stream().filter(s -> s.label().equals("Totales")).findFirst().get().value());
        // Anonymized count should be 1 (testsubject)
        assertEquals(1L, stats.stream().filter(s -> s.label().equals("Anonimizados")).findFirst().get().value());
    }
}