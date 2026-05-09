package com.tfg.backend.service;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.dto.PdfReportDTO;
import com.tfg.backend.model.Registry;
import com.tfg.backend.model.RegistryType;
import com.tfg.backend.repository.RegistryRepository;
import com.tfg.backend.utils.PdfService;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class RegistryService {

    private final PdfService pdfService;
    private final RegistryRepository repository;

    public Page<org.bson.Document> getRegistryStats(
            Date startDate, Date endDate, String viewType, String interval,
            EntityType entityType, RegistryType dataType,
            String metricMode,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds,
            int page, int size) {

        return repository.getRegistryData(startDate, endDate, viewType, interval,
                entityType, dataType, metricMode, storeIds, userIds, productIds, orderIds, page, size);
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


    public Set<String> getInteractedProductReferences(String userId, List<RegistryType> actionTypes) {
        return repository.getInteractedProductIds(userId, actionTypes);
    }

    public List<String> getTopViewedReferences(int limit, Collection<String> excludedRefs) {
        return repository.getMostViewedProductReferences(limit, excludedRefs);
    }


    public Double calculateNextTotal(EntityType entityType, String entityId, RegistryType dataType, Double variation) {
        if (variation == null) return null;

        Double lastTotal = repository.getLastTotal(entityType, entityId, dataType);

        return lastTotal + variation;
    }

    public Registry save(Registry r){
        return this.repository.save(r);
    }

    public byte[] generateRegistryReport(PdfReportDTO request) {
        Page<Document> allData = this.getRegistryStats(
                request.getStartDate(), request.getEndDate(), "TABLE", null,
                request.getEntityType(), request.getDataType(), request.getMetricMode(),
                request.getStoreIds(), request.getUserIds(), request.getProductIds(),
                request.getOrderIds(), 0, Integer.MAX_VALUE);

        return pdfService.generateCustomPdfReport(allData.getContent(), request);
    }
}