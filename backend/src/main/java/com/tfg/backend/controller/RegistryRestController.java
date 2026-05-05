package com.tfg.backend.controller;

import com.tfg.backend.dto.PageResponse;
import com.tfg.backend.dto.PdfReportDTO;
import com.tfg.backend.dto.EntityType;
import com.tfg.backend.service.RegistryService;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.utils.PageFormatter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registry")
@Tag(name = "Registry Management", description = "Registry management")
public class RegistryRestController {

    private final RegistryService registryService;

    @Operation(summary = "(All) Get product views information")
    @GetMapping("/public/views")
    public PageResponse<Document> getPublicStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam String viewType,
            @RequestParam(required = false, defaultValue = "day") String interval,
            @RequestParam(required = false, defaultValue = "VALUE") String metricMode,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "10") int size,
            @RequestParam EntityType entityType,
            @RequestParam RegistryType dataType,
            @RequestParam List<String> productIds) {

        if (entityType != EntityType.PRODUCT || dataType != RegistryType.PRODUCT_VIEWS) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Este endpoint solo permite consultar visualizaciones públicas de productos.");
        }

        Page<Document> pagedResult = registryService.getRegistryStats(startDate, endDate, viewType, interval,
                entityType, dataType, metricMode, null, null, productIds, null, page, size);

        return PageFormatter.toPageResponse(pagedResult);
    }

    @Operation(summary = "(Admin) Get organization registry information for charts or tables")
    @GetMapping("/private/stats")
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
            @RequestParam(required = false) List<String> shopIds,
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> orderIds) {

        Page<Document> pagedResult = registryService.getRegistryStats(startDate, endDate, viewType, interval,
                entityType, dataType, metricMode, shopIds, userIds, productIds, orderIds, page, size);

        return PageFormatter.toPageResponse(pagedResult);
    }

    @Operation(summary = "(Admin) Get entities with registries")
    @GetMapping("/private/entities")
    public List<EntityType> getAvailableEntities() {
        return registryService.getActiveEntityTypes().stream()
                .map(EntityType::valueOf)
                .toList();
    }

    @Operation(summary = "(Admin) Get metrics from an entity")
    @GetMapping("/private/metrics")
    public List<RegistryType> getAvailableMetrics(@RequestParam EntityType entityType) {
        return registryService.getActiveDataTypes(entityType).stream()
                .map(RegistryType::valueOf)
                .toList();
    }

    @Operation(summary = "(Admin) Get associated entities list from an entity and a metric")
    @GetMapping("/private/references")
    public Map<String, List<String>> getReferences(
            @RequestParam EntityType entityType,
            @RequestParam RegistryType dataType) {

        return registryService.getCrossReferences(entityType, dataType);
    }

    @Operation(summary = "(Admin) Generate performance report from selected registry information")
    @PostMapping(value = "/private/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportCustomPdfReport(@RequestBody PdfReportDTO request) {
        byte[] pdfBytes = registryService.generateRegistryReport(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "informe_personalizado.pdf");

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}