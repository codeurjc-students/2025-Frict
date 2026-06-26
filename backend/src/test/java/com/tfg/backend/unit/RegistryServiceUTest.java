package com.tfg.backend.unit;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.PdfReportDTO;
import com.tfg.backend.model.Registry;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.repository.RegistryRepository;
import com.tfg.backend.service.RegistryService;
import com.tfg.backend.utils.PdfService;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistryServiceUTest {

    @Mock
    private RegistryRepository repository;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private RegistryService registryService;

    private Date startDate;
    private Date endDate;

    @BeforeEach
    void setUp() {
        startDate = new Date(0);
        endDate = new Date();
    }

    // --- QUERY DELEGATION ---
    @Nested
    @DisplayName("Tests for read-only query delegation")
    class QueryDelegationTests {

        @Test
        @DisplayName("getRegistryStats delegates all parameters to repository and returns the page")
        void getRegistryStats_DelegatesToRepository() {
            Page<Document> expected = new PageImpl<>(List.of(new Document("key", "value")));
            when(repository.getRegistryData(
                    eq(startDate), eq(endDate), eq("TABLE"), isNull(),
                    eq(EntityType.PRODUCT), eq(RegistryType.PRODUCT_VIEWS), eq("SUM"),
                    isNull(), isNull(), isNull(), isNull(),
                    eq(0), eq(10)))
                    .thenReturn(expected);

            Page<Document> result = registryService.getRegistryStats(
                    startDate, endDate, "TABLE", null,
                    EntityType.PRODUCT, RegistryType.PRODUCT_VIEWS, "SUM",
                    null, null, null, null, 0, 10);

            assertEquals(expected, result);
        }

        @Test
        @DisplayName("getActiveEntityTypes delegates to repository")
        void getActiveEntityTypes_DelegatesToRepository() {
            List<String> expected = List.of("PRODUCT", "ORDER");
            when(repository.getActiveEntityTypes()).thenReturn(expected);

            List<String> result = registryService.getActiveEntityTypes();

            assertEquals(expected, result);
            verify(repository).getActiveEntityTypes();
        }

        @Test
        @DisplayName("getActiveDataTypes delegates to repository filtering by entityType")
        void getActiveDataTypes_DelegatesToRepository() {
            List<String> expected = List.of("PRODUCT_VIEWS", "PRODUCT_UNITS_SOLD");
            when(repository.getActiveDataTypes(EntityType.PRODUCT)).thenReturn(expected);

            List<String> result = registryService.getActiveDataTypes(EntityType.PRODUCT);

            assertEquals(expected, result);
            verify(repository).getActiveDataTypes(EntityType.PRODUCT);
        }

        @Test
        @DisplayName("getCrossReferences delegates to repository and returns the map")
        void getCrossReferences_DelegatesToRepository() {
            Map<String, List<String>> expected = Map.of("shopId", List.of("shop-1", "shop-2"));
            when(repository.getCrossReferences(EntityType.SHOP, RegistryType.SHOP_BUDGET)).thenReturn(expected);

            Map<String, List<String>> result = registryService.getCrossReferences(EntityType.SHOP, RegistryType.SHOP_BUDGET);

            assertEquals(expected, result);
            verify(repository).getCrossReferences(EntityType.SHOP, RegistryType.SHOP_BUDGET);
        }

        @Test
        @DisplayName("getInteractedProductReferences delegates to repository with userId and actionTypes")
        void getInteractedProductReferences_DelegatesToRepository() {
            Set<String> expected = Set.of("prod-1", "prod-2");
            List<RegistryType> actionTypes = List.of(RegistryType.PRODUCT_VIEWS, RegistryType.PRODUCT_UNITS_SOLD);
            when(repository.getInteractedProductIds("user-1", actionTypes)).thenReturn(expected);

            Set<String> result = registryService.getInteractedProductReferences("user-1", actionTypes);

            assertEquals(expected, result);
            verify(repository).getInteractedProductIds("user-1", actionTypes);
        }

        @Test
        @DisplayName("getTopViewedReferences delegates to repository with limit and exclusions")
        void getTopViewedReferences_DelegatesToRepository() {
            List<String> expected = List.of("prod-3", "prod-4");
            Collection<String> excluded = List.of("prod-1", "prod-2");
            when(repository.getMostViewedProductReferences(5, excluded)).thenReturn(expected);

            List<String> result = registryService.getTopViewedReferences(5, excluded);

            assertEquals(expected, result);
            verify(repository).getMostViewedProductReferences(5, excluded);
        }
    }

    // --- SAVE ---
    @Nested
    @DisplayName("Tests for save")
    class SaveTests {

        @Test
        @DisplayName("save delegates to repository and returns the persisted registry")
        void save_DelegatesToRepository() {
            Registry registry = new Registry();
            when(repository.save(registry)).thenReturn(registry);

            Registry result = registryService.save(registry);

            assertSame(registry, result);
            verify(repository).save(registry);
        }
    }

    // --- CALCULATE NEXT TOTAL ---
    @Nested
    @DisplayName("Tests for calculateNextTotal")
    class CalculateNextTotalTests {

        @Test
        @DisplayName("calculateNextTotal returns null when variation is null")
        void calculateNextTotal_ReturnsNull_WhenVariationIsNull() {
            Double result = registryService.calculateNextTotal(EntityType.PRODUCT, "prod-1", RegistryType.PRODUCT_VIEWS, null);

            assertNull(result);
            verify(repository, never()).getLastTotal(any(), any(), any());
        }

        @Test
        @DisplayName("calculateNextTotal sums last total with variation when both are present")
        void calculateNextTotal_SumsLastTotalAndVariation() {
            when(repository.getLastTotal(EntityType.PRODUCT, "prod-1", RegistryType.PRODUCT_VIEWS)).thenReturn(100.0);

            Double result = registryService.calculateNextTotal(EntityType.PRODUCT, "prod-1", RegistryType.PRODUCT_VIEWS, 25.0);

            assertEquals(125.0, result);
        }

        @Test
        @DisplayName("calculateNextTotal returns variation alone when last total is zero (no prior records)")
        void calculateNextTotal_ReturnsVariation_WhenLastTotalIsZero() {
            when(repository.getLastTotal(EntityType.PRODUCT, "prod-new", RegistryType.PRODUCT_VIEWS)).thenReturn(0.0);

            Double result = registryService.calculateNextTotal(EntityType.PRODUCT, "prod-new", RegistryType.PRODUCT_VIEWS, 10.0);

            assertEquals(10.0, result);
        }
    }

    // --- GENERATE PDF REPORT ---
    @Nested
    @DisplayName("Tests for generateRegistryReport")
    class GeneratePdfReportTests {

        @Test
        @DisplayName("generateRegistryReport fetches all data in TABLE mode and delegates to PdfService")
        void generateRegistryReport_FetchesAllDataAndGeneratesPdf() {
            PdfReportDTO request = new PdfReportDTO();
            request.setStartDate(startDate);
            request.setEndDate(endDate);
            request.setEntityType(EntityType.PRODUCT);
            request.setDataType(RegistryType.PRODUCT_VIEWS);
            request.setMetricMode("SUM");
            request.setStoreIds(null);
            request.setUserIds(null);
            request.setProductIds(null);
            request.setOrderIds(null);

            List<Document> docs = List.of(new Document("key", "value"));
            Page<Document> page = new PageImpl<>(docs, PageRequest.of(0, Integer.MAX_VALUE), docs.size());
            byte[] expectedPdf = new byte[]{1, 2, 3};

            when(repository.getRegistryData(
                    eq(startDate), eq(endDate), eq("TABLE"), isNull(),
                    eq(EntityType.PRODUCT), eq(RegistryType.PRODUCT_VIEWS), eq("SUM"),
                    isNull(), isNull(), isNull(), isNull(),
                    eq(0), eq(Integer.MAX_VALUE)))
                    .thenReturn(page);

            when(pdfService.generateCustomPdfReport(docs, request)).thenReturn(expectedPdf);

            byte[] result = registryService.generateRegistryReport(request);

            assertArrayEquals(expectedPdf, result);
            verify(pdfService).generateCustomPdfReport(docs, request);
        }
    }
}