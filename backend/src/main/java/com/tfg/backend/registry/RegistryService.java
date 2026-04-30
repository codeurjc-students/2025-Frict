package com.tfg.backend.registry;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistryService {

    private final RegistryRepository repository;

    public List<Document> getRegistryStats(
            Date startDate, Date endDate, String viewType, String interval,
            String entityType, String dataType,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds) {

        return repository.getRegistryData(
                startDate, endDate, viewType, interval,
                entityType, dataType,
                storeIds, userIds, productIds, orderIds
        );
    }

    public List<String> getUniqueReferences(String targetEntityType, String associatedEntity) {
        return repository.getUniqueReferenceCodes(targetEntityType, associatedEntity);
    }
}