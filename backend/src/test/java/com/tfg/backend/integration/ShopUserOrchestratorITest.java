package com.tfg.backend.integration;

import com.tfg.backend.model.Shop;
import com.tfg.backend.model.User;
import com.tfg.backend.repository.ShopRepository;
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

    private User activeUser;
    private Shop localShop;

    @BeforeEach
    void setUpOrchestrator() {
        // 1. Guardar y capturar el usuario
        activeUser = new User("Orch User", "user_orch", "orch@test.com", "pass", "USER,MANAGER");
        activeUser = userRepository.saveAndFlush(activeUser);

        // 2. Guardar y capturar la tienda
        localShop = new Shop("Local Store", null, 3000.0);
        localShop.setReferenceCode("SHOP-LOC-001");
        localShop = shopRepository.saveAndFlush(localShop);

        // 3. Autenticar al usuario para el contexto de seguridad
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
        // Preparamos el Map simulando el body del controlador
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
}