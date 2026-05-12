package com.tfg.backend.integration;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.PdfReportDTO;
import com.tfg.backend.model.Registry;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.service.RegistryService;
import com.tfg.backend.utils.PdfService;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for RegistryService.
 * Validates time-series data storage and complex aggregation queries against a real MongoDB instance.
 * PdfService is mocked since PDF generation does not interact with the database.
 */
@SpringBootTest
@ActiveProfiles("test")
public class RegistryServiceITest {

    @Autowired private RegistryService registryService;
    @Autowired private MongoTemplate mongoTemplate;

    @MockitoBean private PdfService pdfService;

    private Date startDate;
    private Date endDate;

    @BeforeEach
    void setUp() {
        // Wide date window guaranteed to include all test data inserted in each test
        startDate = new Date(System.currentTimeMillis() - 60_000);
        endDate = new Date(System.currentTimeMillis() + 60_000);
    }

    @AfterEach
    void cleanUp() {
        mongoTemplate.remove(new Query(), "registries");
    }

    // --- SAVE ---

    @Test
    @DisplayName("save: Persists a Registry document in MongoDB and returns the saved instance")
    void testSave_RegistryIsPersisted() {
        Registry registry = new Registry(
                EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 5.0,
                null, null, null, null, "prod-save-test", "Test Product", null, null);

        Registry saved = registryService.save(registry);

        assertNotNull(saved.getId(), "Saved registry must have an assigned MongoDB ID");
    }

    // --- ACTIVE ENTITY TYPES ---

    @Test
    @DisplayName("getActiveEntityTypes: Returns distinct entity types present in the collection")
    void testGetActiveEntityTypes_ReturnsDistinctTypes() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-1", "P1", null, null));
        registryService.save(new Registry(EntityType.ORDER, RegistryType.ORDERS_COMPLETED, 1.0,
                null, null, null, null, null, null, "order-1", "O1"));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-2", "P2", null, null));

        List<String> entityTypes = registryService.getActiveEntityTypes();

        assertAll(
                () -> assertTrue(entityTypes.contains("PRODUCT"), "PRODUCT type must be present"),
                () -> assertTrue(entityTypes.contains("ORDER"), "ORDER type must be present"),
                () -> assertEquals(2, new HashSet<>(entityTypes).size(), "Must return exactly 2 distinct types")
        );
    }

    // --- ACTIVE DATA TYPES ---

    @Test
    @DisplayName("getActiveDataTypes: Returns only data types associated with the given entity type")
    void testGetActiveDataTypes_FiltersByEntityType() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-1", "P1", null, null));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_UNITS_SOLD, 2.0,
                null, null, null, null, "prod-1", "P1", null, null));
        registryService.save(new Registry(EntityType.ORDER, RegistryType.ORDERS_COMPLETED, 1.0,
                null, null, null, null, null, null, "order-1", "O1"));

        List<String> dataTypes = registryService.getActiveDataTypes(EntityType.PRODUCT);

        assertAll(
                () -> assertTrue(dataTypes.contains("PRODUCT_VIEWS"), "PRODUCT_VIEWS must be present"),
                () -> assertTrue(dataTypes.contains("PRODUCT_UNITS_SOLD"), "PRODUCT_UNITS_SOLD must be present"),
                () -> assertFalse(dataTypes.contains("ORDERS_COMPLETED"), "ORDER data types must not appear")
        );
    }

    // --- CROSS REFERENCES ---

    @Test
    @DisplayName("getCrossReferences: Returns maps of entity IDs segmented by reference category")
    void testGetCrossReferences_ReturnsEntityIdsByCategory() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                "shop-1", "Shop 1", "user-1", "User 1", "prod-1", "Product 1", null, null));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                "shop-2", "Shop 2", "user-2", "User 2", "prod-2", "Product 2", null, null));

        Map<String, List<String>> refs = registryService.getCrossReferences(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS);

        assertAll(
                () -> assertTrue(refs.get("productId").contains("prod-1"), "prod-1 must be in productId references"),
                () -> assertTrue(refs.get("productId").contains("prod-2"), "prod-2 must be in productId references"),
                () -> assertTrue(refs.get("shopId").contains("shop-1"), "shop-1 must be in shopId references"),
                () -> assertTrue(refs.get("userId").contains("user-1"), "user-1 must be in userId references")
        );
    }

    // --- INTERACTED PRODUCT REFERENCES ---

    @Test
    @DisplayName("getInteractedProductReferences: Returns only products the user has interacted with via the given action types")
    void testGetInteractedProductReferences_ReturnsProductsForUser() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, "user-A", "User A", "prod-viewed", "Prod Viewed", null, null));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_UNITS_SOLD, 1.0,
                null, null, "user-A", "User A", "prod-bought", "Prod Bought", null, null));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, "user-B", "User B", "prod-other-user", "Prod Other", null, null));

        Set<String> interacted = registryService.getInteractedProductReferences(
                "user-A", List.of(RegistryType.PRODUCT_VIEWS, RegistryType.PRODUCT_UNITS_SOLD));

        assertAll(
                () -> assertTrue(interacted.contains("prod-viewed"), "Viewed product must be included"),
                () -> assertTrue(interacted.contains("prod-bought"), "Bought product must be included"),
                () -> assertFalse(interacted.contains("prod-other-user"), "Other user's products must be excluded")
        );
    }

    // --- TOP VIEWED REFERENCES ---

    @Test
    @DisplayName("getTopViewedReferences: Returns top N products by view count, excluding specified ones")
    void testGetTopViewedReferences_ReturnsTopNExcludingSpecified() {
        for (int i = 0; i < 5; i++) {
            registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                    null, null, null, null, "prod-popular", "Popular", null, null));
        }
        for (int i = 0; i < 3; i++) {
            registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                    null, null, null, null, "prod-mid", "Mid", null, null));
        }
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-low", "Low", null, null));

        // Exclude the most popular to verify exclusion logic
        List<String> top2 = registryService.getTopViewedReferences(2, List.of("prod-popular"));

        assertAll(
                () -> assertEquals(2, top2.size()),
                () -> assertFalse(top2.contains("prod-popular"), "Excluded product must not appear"),
                () -> assertTrue(top2.contains("prod-mid"), "Second most viewed must be present")
        );
    }

    // --- CALCULATE NEXT TOTAL ---

    @Test
    @DisplayName("calculateNextTotal: Adds variation to the last stored total for the entity")
    void testCalculateNextTotal_AddsVariationToLastTotal() {
        Registry registry = new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 10.0,
                null, null, null, null, "prod-total-test", "Product", null, null);
        registry.getMetrics().setTotal(100.0);
        registryService.save(registry);

        Double result = registryService.calculateNextTotal(
                EntityType.PRODUCT, "prod-total-test", RegistryType.PRODUCT_VIEWS, 25.0);

        assertEquals(125.0, result, "Must return lastTotal (100) + variation (25)");
    }

    @Test
    @DisplayName("calculateNextTotal: Returns just the variation when there are no prior records (base total is 0)")
    void testCalculateNextTotal_ReturnsVariation_WhenNoPriorRecords() {
        Double result = registryService.calculateNextTotal(
                EntityType.PRODUCT, "brand-new-product", RegistryType.PRODUCT_VIEWS, 15.0);

        assertEquals(15.0, result, "Must return variation alone when no history exists (0 + 15)");
    }

    @Test
    @DisplayName("calculateNextTotal: Returns null when variation is null, without querying the database")
    void testCalculateNextTotal_ReturnsNull_WhenVariationIsNull() {
        Double result = registryService.calculateNextTotal(
                EntityType.PRODUCT, "prod-null-test", RegistryType.PRODUCT_VIEWS, null);

        assertNull(result, "Must short-circuit and return null when variation is null");
    }

    // --- GET REGISTRY STATS (TABLE VIEW) ---

    @Test
    @DisplayName("getRegistryStats (TABLE): Returns paginated documents within the requested date range")
    void testGetRegistryStats_TABLE_ReturnsPaginatedResults() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-1", "P1", null, null));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 2.0,
                null, null, null, null, "prod-2", "P2", null, null));
        registryService.save(new Registry(EntityType.ORDER, RegistryType.ORDERS_COMPLETED, 1.0,
                null, null, null, null, null, null, "order-1", "O1"));

        Page<Document> page = registryService.getRegistryStats(
                startDate, endDate, "TABLE", null,
                null, null, null,
                null, null, null, null,
                0, 10);

        assertEquals(3, page.getTotalElements(), "Must return all 3 inserted documents");
    }

    @Test
    @DisplayName("getRegistryStats (TABLE): Correctly filters by entityType and dataType")
    void testGetRegistryStats_TABLE_FiltersByEntityAndDataType() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-1", "P1", null, null));
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_UNITS_SOLD, 5.0,
                null, null, null, null, "prod-1", "P1", null, null));
        registryService.save(new Registry(EntityType.ORDER, RegistryType.ORDERS_COMPLETED, 1.0,
                null, null, null, null, null, null, "order-1", "O1"));

        Page<Document> page = registryService.getRegistryStats(
                startDate, endDate, "TABLE", null,
                EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, null,
                null, null, null, null,
                0, 10);

        assertAll(
                () -> assertEquals(1, page.getTotalElements(), "Must return only PRODUCT / PRODUCT_VIEWS records"),
                () -> assertEquals("PRODUCT_VIEWS", page.getContent().get(0)
                        .get("metadata", Document.class).getString("dataType"),
                        "Returned document must have the correct dataType")
        );
    }

    @Test
    @DisplayName("getRegistryStats (TABLE): Respects pagination parameters")
    void testGetRegistryStats_TABLE_PaginatesCorrectly() {
        for (int i = 0; i < 5; i++) {
            registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, (double) i,
                    null, null, null, null, "prod-" + i, "P" + i, null, null));
        }

        Page<Document> firstPage = registryService.getRegistryStats(
                startDate, endDate, "TABLE", null,
                null, null, null,
                null, null, null, null,
                0, 2);

        assertAll(
                () -> assertEquals(5, firstPage.getTotalElements()),
                () -> assertEquals(2, firstPage.getContent().size(), "Page size must be respected")
        );
    }

    // --- GENERATE REGISTRY REPORT ---

    @Test
    @DisplayName("generateRegistryReport: Fetches all data and delegates to PdfService with the correct arguments")
    void testGenerateRegistryReport_DelegatesToPdfService() {
        registryService.save(new Registry(EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, 1.0,
                null, null, null, null, "prod-report", "P", null, null));

        PdfReportDTO request = new PdfReportDTO();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setEntityType(EntityType.PRODUCT);
        request.setDataType(RegistryType.PRODUCT_VIEWS);
        request.setMetricMode("SUM");

        byte[] expectedPdf = new byte[]{1, 2, 3};
        when(pdfService.generateCustomPdfReport(any(), eq(request))).thenReturn(expectedPdf);

        byte[] result = registryService.generateRegistryReport(request);

        assertArrayEquals(expectedPdf, result);
        verify(pdfService).generateCustomPdfReport(any(), eq(request));
    }
}