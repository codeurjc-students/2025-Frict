package com.tfg.backend.registry;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/registry")
public class RegistryRestController {

    private final RegistryService registryService;

    @GetMapping("/stats")
    public List<Document> getStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @RequestParam String viewType,
            @RequestParam(required = false, defaultValue = "day") String interval,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String dataType,
            @RequestParam(required = false) List<String> storeIds,
            @RequestParam(required = false) List<String> userIds,
            @RequestParam(required = false) List<String> productIds,
            @RequestParam(required = false) List<String> orderIds) {

        return registryService.getRegistryStats(
                startDate, endDate, viewType, interval,
                entityType, dataType,
                storeIds, userIds, productIds, orderIds
        );
    }

    @GetMapping("/references")
    public List<String> getReferences(
            @RequestParam String targetEntityType,
            @RequestParam String associatedEntity) {

        return registryService.getUniqueReferences(targetEntityType, associatedEntity);
    }
}