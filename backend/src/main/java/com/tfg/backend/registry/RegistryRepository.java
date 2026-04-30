package com.tfg.backend.registry;


import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class RegistryRepository {

    private final MongoTemplate mongoTemplate;

    public List<Document> getRegistryData(
            Date startDate, Date endDate, String viewType, String interval,
            String entityType, String dataType,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds) {

        Criteria matchCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);

        if (entityType != null && !entityType.isEmpty()) matchCriteria.and("metadata.entityType").is(entityType);
        if (dataType != null && !dataType.isEmpty()) matchCriteria.and("metadata.dataType").is(dataType);

        // Dynamic filters by lists of entities reference codes
        if (storeIds != null && !storeIds.isEmpty()) matchCriteria.and("metadata.storeId").in(storeIds);
        if (userIds != null && !userIds.isEmpty()) matchCriteria.and("metadata.userId").in(userIds);
        if (productIds != null && !productIds.isEmpty()) matchCriteria.and("metadata.productId").in(productIds);
        if (orderIds != null && !orderIds.isEmpty()) matchCriteria.and("metadata.orderId").in(orderIds);

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(Aggregation.match(matchCriteria));

        if ("GRAPH".equalsIgnoreCase(viewType)) {
            // Massive BSON aggregation
            AggregationOperation groupStage = context -> new Document("$group",
                    new Document("_id",
                            new Document("$dateTrunc",
                                    new Document("date", "$timestamp")
                                            .append("unit", interval != null ? interval.toLowerCase() : "day")
                            )
                    )
                            .append("totalValue", new Document("$sum", "$metrics.value"))
                            .append("recordCount", new Document("$sum", 1))
            );
            operations.add(groupStage);
            operations.add(Aggregation.sort(Sort.Direction.ASC, "_id"));
        } else {
            operations.add(Aggregation.sort(Sort.Direction.DESC, "timestamp"));
            operations.add(Aggregation.limit(100));
        }

        Aggregation aggregation = Aggregation.newAggregation(operations);
        return mongoTemplate.aggregate(aggregation, "registries", Document.class).getMappedResults();
    }

    public List<String> getUniqueReferenceCodes(String targetEntityType, String associatedEntity) {
        // 1. Map entity name associated to DB real field
        String fieldToFind = switch (associatedEntity.toUpperCase()) {
            case "STORE" -> "metadata.storeId";
            case "USER" -> "metadata.userId";
            case "PRODUCT" -> "metadata.productId";
            case "ORDER" -> "metadata.orderId";
            default -> throw new IllegalArgumentException("Entidad asociada no válida: " + associatedEntity);
        };

        // 2. Filter by main entity (targetEntityType)
        Query query = new Query(Criteria.where("metadata.entityType").is(targetEntityType));

        // 3. Request the distinct values of that field
        return mongoTemplate.findDistinct(query, fieldToFind, "registries", String.class);
    }
}