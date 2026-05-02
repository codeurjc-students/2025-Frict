package com.tfg.backend.registry;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.PdfExportRequest;
import com.tfg.backend.notification.EntityType;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registry")
@Tag(name = "Registry Management", description = "Registry management")
public class RegistryRestController {

    private final RegistryService registryService;

    @GetMapping("/stats")
    public PageResponse<Document> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam String viewType,
            @RequestParam(required = false, defaultValue = "day") String interval,
            @RequestParam(required = false, defaultValue = "VALUE") String metricMode,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) RegistryType dataType,
            @RequestParam(required = false) List<String> storeIds,
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> orderIds) {

        Page<Document> pagedResult = registryService.getRegistryStats(startDate, endDate, viewType, interval,
                entityType, dataType, metricMode, storeIds, userIds, productIds, orderIds, page, size);

        return PageFormatter.toPageResponse(pagedResult);
    }

    @GetMapping("/entities")
    public List<EntityType> getAvailableEntities() {
        return registryService.getActiveEntityTypes().stream()
                .map(EntityType::valueOf)
                .toList();
    }

    @GetMapping("/metrics")
    public List<RegistryType> getAvailableMetrics(@RequestParam EntityType entityType) {
        return registryService.getActiveDataTypes(entityType).stream()
                .map(RegistryType::valueOf)
                .toList();
    }

    @GetMapping("/references")
    public Map<String, List<String>> getReferences(
            @RequestParam EntityType entityType,
            @RequestParam RegistryType dataType) {

        return registryService.getCrossReferences(entityType, dataType);
    }

    @PostMapping(value = "/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCustomPdf(@RequestBody PdfExportRequest request) {

        Page<Document> allData = registryService.getRegistryStats(
                request.getStartDate(), request.getEndDate(), "TABLE", null,
                request.getEntityType(), request.getDataType(), request.getMetricMode(),
                request.getStoreIds(), request.getUserIds(), request.getProductIds(),
                request.getOrderIds(), 0, Integer.MAX_VALUE);
        
        byte[] pdfBytes = registryService.generateCustomPdf(allData.getContent(), request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "informe_personalizado.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}