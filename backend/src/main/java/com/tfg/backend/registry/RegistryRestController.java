package com.tfg.backend.registry;

import com.tfg.backend.notification.EntityType;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.format.annotation.DateTimeFormat;
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
    public List<Document> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam String viewType,
            @RequestParam(required = false, defaultValue = "day") String interval,
            @RequestParam(required = false) EntityType entityType,
            @RequestParam(required = false) RegistryType dataType,
            @RequestParam(required = false) List<String> storeIds,
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> orderIds) {

        return registryService.getRegistryStats(startDate, endDate, viewType, interval,
                entityType, dataType, storeIds, userIds, productIds, orderIds);
    }

    // 1. Selector de Entidades
    @GetMapping("/entities")
    public List<EntityType> getAvailableEntities() {
        return registryService.getActiveEntityTypes().stream()
                .map(EntityType::valueOf) // Aplica @JsonValue para enviar la traducción
                .toList();
    }

    // 2. Selector de Métricas
    @GetMapping("/metrics")
    public List<RegistryType> getAvailableMetrics(@RequestParam EntityType entityType) {
        return registryService.getActiveDataTypes(entityType).stream()
                .map(RegistryType::valueOf) // Aplica @JsonValue para enviar la traducción
                .toList();
    }

    // 3. Mapa de IDs Asociados (Se devuelven los IDs tal cual están en la base de datos)
    @GetMapping("/references")
    public Map<String, List<String>> getReferences(
            @RequestParam EntityType entityType,
            @RequestParam RegistryType dataType) {

        return registryService.getCrossReferences(entityType, dataType);
    }
}