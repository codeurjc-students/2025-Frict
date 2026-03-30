package com.tfg.backend.integration;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.repository.UserRepository;
import com.tfg.backend.service.StatService;
import com.tfg.backend.utils.StatDTO;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for StatService.
 * Validates that security contexts are correctly extracted and that
 * delegated queries return accurate, role-filtered statistics from MySQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class StatServiceITest {

    @Autowired private StatService statService;
    @Autowired private UserRepository userRepository;
    @Autowired private ShopRepository shopRepository;
    @Autowired private EntityManager entityManager;

    private User adminUser;
    private User managerUser;
    private User standardUser;

    @BeforeEach
    void setUpStatsScenario() {
        // 1. Create Users with different roles
        adminUser = new User("Admin", "admin", "admin@test.com", "pass", "ADMIN");
        adminUser = userRepository.saveAndFlush(adminUser);

        managerUser = new User("Manager", "manager", "manager@test.com", "pass", "MANAGER");
        // Ensure the shops list is initialized to avoid NullPointerExceptions
        if (managerUser.getAssignedShops() == null) {
            managerUser.setAssignedShops(new java.util.ArrayList<>());
        }
        managerUser = userRepository.saveAndFlush(managerUser);

        standardUser = new User("User", "user", "user@test.com", "pass", "USER");
        standardUser = userRepository.saveAndFlush(standardUser);

        // 2. Create Shops
        // Shop 1: Assigned to the Manager
        Shop managerShop = new Shop("Manager Shop", null, 1000.0);
        managerShop.setReferenceCode("STAT-SHOP-01");
        managerShop.setAssignedManager(managerUser);
        managerUser.getAssignedShops().add(managerShop);
        shopRepository.saveAndFlush(managerShop);

        // Shop 2: Assigned to NO ONE (Only visible to Admin)
        Shop unassignedShop = new Shop("Unassigned Shop", null, 2500.0);
        unassignedShop.setReferenceCode("STAT-SHOP-02");
        shopRepository.saveAndFlush(unassignedShop);

        // 3. Clear Hibernate Cache to force database reads
        entityManager.flush();
        entityManager.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper to authenticate users with their proper roles in the SecurityContext.
     */
    private void authenticate(User user) {
        String role = user.getRoles().contains("ADMIN") ? "ADMIN" :
                (user.getRoles().contains("MANAGER") ? "MANAGER" : "USER");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), "pass", List.of(new SimpleGrantedAuthority(role)))
        );
    }

    @Test
    @DisplayName("getShopsStatsByRole: ADMIN sees global totals (All shops and combined budgets)")
    void testGetShopsStatsByRole_AsAdmin() {
        // Authenticate as Admin
        authenticate(adminUser);

        // Act
        List<StatDTO> stats = statService.getShopsStatsByRole();

        // Assert: Admin should see 2 shops and 3500.0 total budget
        assertFalse(stats.isEmpty());

        StatDTO shopsCount = stats.stream().filter(s -> s.label().equals("Tiendas")).findFirst().orElseThrow();
        StatDTO budgetTotal = stats.stream().filter(s -> s.label().equals("Presupuesto Total")).findFirst().orElseThrow();

        assertEquals(2L, shopsCount.value(), "Admin must count all shops in the system");
        assertEquals(3500.0, (Double) budgetTotal.value(), "Admin must see the sum of ALL budgets (1000 + 2500)");
    }

    @Test
    @DisplayName("getShopsStatsByRole: MANAGER sees only stats for their assigned shops")
    void testGetShopsStatsByRole_AsManager() {
        // Authenticate as Manager
        authenticate(managerUser);

        // Act
        List<StatDTO> stats = statService.getShopsStatsByRole();

        // Assert: Manager should see 1 shop and 1000.0 budget
        assertFalse(stats.isEmpty());

        StatDTO shopsCount = stats.stream().filter(s -> s.label().equals("Tiendas")).findFirst().orElseThrow();
        StatDTO budgetTotal = stats.stream().filter(s -> s.label().equals("Presupuesto Total")).findFirst().orElseThrow();

        assertEquals(1L, shopsCount.value(), "Manager must only count their assigned shop");
        assertEquals(1000.0, (Double) budgetTotal.value(), "Manager must only see their assigned shop's budget");
    }

    @Test
    @DisplayName("getShopsStatsByRole: Standard USER receives an empty list")
    void testGetShopsStatsByRole_AsStandardUser() {
        // Authenticate as standard User
        authenticate(standardUser);

        // Act
        List<StatDTO> stats = statService.getShopsStatsByRole();

        // Assert: Users don't have access to shop metrics
        assertTrue(stats.isEmpty(), "Standard users should receive an empty list of shop stats");
    }
}