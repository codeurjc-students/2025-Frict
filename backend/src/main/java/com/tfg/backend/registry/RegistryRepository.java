package com.tfg.backend.registry;

import com.tfg.backend.notification.EntityType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class RegistryRepository {

    private final MongoTemplate mongoTemplate;

    public List<Document> getRegistryData(
            Date startDate, Date endDate, String viewType, String interval,
            EntityType entityType, RegistryType dataType,
            String metricMode,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds) {

        Criteria matchCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);

        if (entityType != null) matchCriteria.and("metadata.entityType").is(entityType);
        if (dataType != null) matchCriteria.and("metadata.dataType").is(dataType);
        if (storeIds != null && !storeIds.isEmpty()) matchCriteria.and("metadata.storeId").in(storeIds);
        if (userIds != null && !userIds.isEmpty()) matchCriteria.and("metadata.userId").in(userIds);
        if (productIds != null && !productIds.isEmpty()) matchCriteria.and("metadata.productId").in(productIds);
        if (orderIds != null && !orderIds.isEmpty()) matchCriteria.and("metadata.orderId").in(orderIds);

        List<AggregationOperation> operations = new ArrayList<>();
        operations.add(Aggregation.match(matchCriteria));

        if ("GRAPH".equalsIgnoreCase(viewType)) {
            operations.add(Aggregation.sort(Sort.Direction.ASC, "timestamp"));

            Object groupAccumulator;
            if ("TOTAL".equalsIgnoreCase(metricMode)) {
                groupAccumulator = new Document("$last", "$metrics.total");
            } else {
                groupAccumulator = new Document("$sum", "$metrics.value");
            }

            AggregationOperation groupStage = context -> new Document("$group",
                    new Document("_id",
                            new Document("$dateTrunc",
                                    new Document("date", "$timestamp")
                                            .append("unit", interval != null ? interval.toLowerCase() : "day")
                            )
                    )
                            .append("totalValue", groupAccumulator) // <-- USAMOS LA VARIABLE DINÁMICA
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

    public List<String> getActiveEntityTypes() {
        return mongoTemplate.findDistinct(new Query(), "metadata.entityType", "registries", String.class);
    }

    public List<String> getActiveDataTypes(EntityType entityType) {
        Query query = new Query(Criteria.where("metadata.entityType").is(entityType));
        return mongoTemplate.findDistinct(query, "metadata.dataType", "registries", String.class);
    }

    public Map<String, List<String>> getCrossReferences(EntityType entityType, RegistryType dataType) {
        Query query = new Query(
                Criteria.where("metadata.entityType").is(entityType)
                        .and("metadata.dataType").is(dataType)
        );

        Map<String, List<String>> references = new HashMap<>();

        references.put("storeId", mongoTemplate.findDistinct(query, "metadata.storeId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        references.put("productId", mongoTemplate.findDistinct(query, "metadata.productId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        references.put("userId", mongoTemplate.findDistinct(query, "metadata.userId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        references.put("orderId", mongoTemplate.findDistinct(query, "metadata.orderId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        return references;
    }
}