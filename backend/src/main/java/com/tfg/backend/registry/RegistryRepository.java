package com.tfg.backend.registry;

import com.tfg.backend.notification.EntityType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

    public Page<Document> getRegistryData(
            Date startDate, Date endDate, String viewType, String interval,
            EntityType entityType, RegistryType dataType,
            String metricMode,
            List<String> storeIds, List<String> userIds,
            List<String> productIds, List<String> orderIds,
            int page, int size) {

        Criteria matchCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);

        if (entityType != null) matchCriteria.and("metadata.entityType").is(entityType);
        if (dataType != null) matchCriteria.and("metadata.dataType").is(dataType);
        if (storeIds != null && !storeIds.isEmpty()) matchCriteria.and("metadata.storeId").in(storeIds);
        if (userIds != null && !userIds.isEmpty()) matchCriteria.and("metadata.userId").in(userIds);
        if (productIds != null && !productIds.isEmpty()) matchCriteria.and("metadata.productId").in(productIds);
        if (orderIds != null && !orderIds.isEmpty()) matchCriteria.and("metadata.orderId").in(orderIds);

        if ("GRAPH".equalsIgnoreCase(viewType)) {
            List<AggregationOperation> operations = new ArrayList<>();
            operations.add(Aggregation.match(matchCriteria));
            operations.add(Aggregation.sort(Sort.Direction.ASC, "timestamp"));

            Object groupAccumulator = "TOTAL".equalsIgnoreCase(metricMode)
                    ? new Document("$last", "$metrics.total")
                    : new Document("$sum", "$metrics.value");

            AggregationOperation groupStage = context -> new Document("$group",
                    new Document("_id",
                            new Document("$dateTrunc",
                                    new Document("date", "$timestamp")
                                            .append("unit", interval != null ? interval.toLowerCase() : "day")
                            )
                    )
                            .append("totalValue", groupAccumulator)
                            .append("recordCount", new Document("$sum", 1))
            );
            operations.add(groupStage);
            operations.add(Aggregation.sort(Sort.Direction.ASC, "_id"));

            Aggregation aggregation = Aggregation.newAggregation(operations);
            List<Document> result = mongoTemplate.aggregate(aggregation, "registries", Document.class).getMappedResults();

            return new PageImpl<>(result, PageRequest.of(0, Math.max(1, result.size())), result.size());
        } else {
            Query query = new Query(matchCriteria);
            long total = mongoTemplate.count(query, "registries");

            PageRequest pageRequest = PageRequest.of(page, size);
            query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
            query.with(pageRequest);

            List<Document> result = mongoTemplate.find(query, Document.class, "registries");
            return new PageImpl<>(result, pageRequest, total);
        }
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