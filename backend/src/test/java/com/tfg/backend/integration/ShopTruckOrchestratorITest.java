package com.tfg.backend.integration;

import com.tfg.backend.dto.AddressDTO;
import com.tfg.backend.dto.TruckDTO;
import com.tfg.backend.model.Shop;
import com.tfg.backend.model.Truck;
import com.tfg.backend.repository.ShopRepository;
import com.tfg.backend.repository.TruckRepository;
import com.tfg.backend.service.ShopTruckOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ShopTruckOrchestratorITest {

    @Autowired private ShopTruckOrchestrator orchestrator;
    @Autowired private ShopRepository shopRepository;
    @Autowired private TruckRepository truckRepository;

    private Shop hubShop;
    private Truck transferTruck;

    @BeforeEach
    void setUpOrchestrator() {
        hubShop = new Shop("Central Hub", null, 50000.0);
        hubShop.setReferenceCode("SHOP-ORCH-001");
        hubShop = shopRepository.saveAndFlush(hubShop);

        transferTruck = new Truck("0000-ORC", null, 20);
        transferTruck.setReferenceCode("TR-ORCH-001");
        transferTruck.setAssignedShop(hubShop);
        transferTruck = truckRepository.saveAndFlush(transferTruck);
    }

    @Test
    @DisplayName("getShopByAssignedTruckId fetches the correct shop bypassing circular dependencies")
    void testGetShopByAssignedTruckId() {
        Shop result = orchestrator.getShopByAssignedTruckId(transferTruck.getId());

        assertNotNull(result);
        assertEquals(hubShop.getId(), result.getId());
    }

    @Test
    @DisplayName("setAssignedTruck links and unlinks a truck from a shop correctly")
    void testSetAssignedTruck_LinkAndUnlink() {
        // 1. Unlink (false)
        orchestrator.setAssignedTruck(hubShop.getId(), transferTruck.getId(), false);

        Truck unlinkedTruck = truckRepository.findById(transferTruck.getId()).orElseThrow();
        assertNull(unlinkedTruck.getAssignedShop(), "The truck should be unlinked from the shop");

        // 2. Link back (true)
        orchestrator.setAssignedTruck(hubShop.getId(), transferTruck.getId(), true);

        Truck linkedTruck = truckRepository.findById(transferTruck.getId()).orElseThrow();
        assertNotNull(linkedTruck.getAssignedShop());
        assertEquals(hubShop.getId(), linkedTruck.getAssignedShop().getId(), "The truck should be linked back to the shop");
    }

    @Test
    @DisplayName("createTruck successfully orchestrates DTO conversion and shop association")
    void testCreateTruck_WithShopAssociation() {
        TruckDTO dto = new TruckDTO();
        dto.setPlateNumber("9999-NEW");
        dto.setMaxOrderCapacity(15);
        dto.setShopId(hubShop.getId()); // Give the shop id for the orchestrator to search for it

        AddressDTO addr = new AddressDTO();
        addr.setStreet("Orchestrator Ave");
        addr.setCity("Madrid");
        dto.setAddress(addr);

        Truck createdTruck = orchestrator.createTruck(dto);

        Truck dbTruck = truckRepository.findById(createdTruck.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("9999-NEW", dbTruck.getPlateNumber()),
                () -> assertNotNull(dbTruck.getAssignedShop()),
                () -> assertEquals(hubShop.getId(), dbTruck.getAssignedShop().getId(), "Orchestrator must properly link the fetched shop")
        );
    }
}