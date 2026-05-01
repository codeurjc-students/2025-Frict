package com.tfg.backend.registry;

import com.tfg.backend.notification.EntityType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistryService {

    private final RegistryRepository repository;

    public List<Document> getRegistryStats(
            Date startDate, Date endDate, String viewType, String interval,
            EntityType entityType, RegistryType dataType,
            String metricMode,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds) {

        return repository.getRegistryData(startDate, endDate, viewType, interval,
                entityType, dataType, metricMode, storeIds, userIds, productIds, orderIds);
    }

    public List<String> getActiveEntityTypes() {
        return repository.getActiveEntityTypes();
    }

    public List<String> getActiveDataTypes(EntityType entityType) {
        return repository.getActiveDataTypes(entityType);
    }

    public Map<String, List<String>> getCrossReferences(EntityType entityType, RegistryType dataType) {
        return repository.getCrossReferences(entityType, dataType);
    }
}