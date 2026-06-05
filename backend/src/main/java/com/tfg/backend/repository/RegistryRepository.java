package com.tfg.backend.repository;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.Registry;
import com.tfg.backend.model.RegistryType;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
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
            List<String> shopIds, List<String> userIds,
            List<String> productIds, List<String> orderIds,
            int page, int size) {

        Criteria matchCriteria = Criteria.where("timestamp").gte(startDate).lte(endDate);

        if (entityType != null) matchCriteria.and("metadata.entityType").is(entityType);
        if (dataType != null) matchCriteria.and("metadata.dataType").is(dataType);
        if (shopIds != null && !shopIds.isEmpty()) matchCriteria.and("metadata.shopId").in(shopIds);
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
                    new Document("_id", buildDateGroupId("$timestamp", interval))
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

        references.put("shopId", mongoTemplate.findDistinct(query, "metadata.shopId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        references.put("productId", mongoTemplate.findDistinct(query, "metadata.productId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        references.put("userId", mongoTemplate.findDistinct(query, "metadata.userId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        references.put("orderId", mongoTemplate.findDistinct(query, "metadata.orderId", "registries", String.class)
                .stream().filter(Objects::nonNull).toList());

        return references;
    }


    public Set<String> getInteractedProductIds(String userId, List<RegistryType> actionTypes) {
        Query query = new Query(
                Criteria.where("metadata.userId").is(userId)
                        .and("metadata.dataType").in(actionTypes)
                        .and("metadata.productId").ne(null)
        );
        List<String> productIds = mongoTemplate.findDistinct(query, "metadata.productId", "registries", String.class);
        return new HashSet<>(productIds);
    }

    public List<String> getMostViewedProductReferences(int limit, Collection<String> excludedRefs) {
        List<AggregationOperation> operations = new ArrayList<>();

        operations.add(Aggregation.match(Criteria.where("metadata.dataType").is(RegistryType.PRODUCT_VIEWS)));

        if (excludedRefs != null && !excludedRefs.isEmpty()) {
            operations.add(Aggregation.match(Criteria.where("metadata.productId").nin(excludedRefs)));
        }

        operations.add(Aggregation.group("metadata.productId").sum("metrics.value").as("totalViews"));
        operations.add(Aggregation.sort(Sort.Direction.DESC, "totalViews"));
        operations.add(Aggregation.limit(limit));

        return mongoTemplate.aggregate(Aggregation.newAggregation(operations), "registries", Document.class)
                .getMappedResults().stream()
                .map(doc -> doc.getString("_id"))
                .filter(Objects::nonNull)
                .toList();
    }


    public Double getLastTotal(EntityType entityType, String entityId, RegistryType dataType) {
        if (entityType == null || entityId == null) return 0.0;

        String idField = "metadata." + entityType.name().toLowerCase() + "Id";

        Query query = new Query(
                Criteria.where(idField).is(entityId)
                        .and("metadata.dataType").is(dataType.name())
        );

        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));
        query.limit(1);

        Registry lastRecord = mongoTemplate.findOne(query, Registry.class, "registries");

        if (lastRecord != null && lastRecord.getMetrics() != null && lastRecord.getMetrics().getTotal() != null) {
            return lastRecord.getMetrics().getTotal();
        }

        return 0.0;
    }


    public List<String> getTopSalesProductReferencesInList(Collection<String> productRefs, int limit) {
        List<AggregationOperation> operations = new ArrayList<>();

        operations.add(Aggregation.match(Criteria.where("metadata.dataType").is(RegistryType.PRODUCT_UNITS_SOLD)));

        if (productRefs != null && !productRefs.isEmpty()) {
            operations.add(Aggregation.match(Criteria.where("metadata.productId").in(productRefs)));
        } else {
            return Collections.emptyList();
        }

        operations.add(Aggregation.sort(Sort.Direction.DESC, "timestamp"));

        operations.add(Aggregation.group("metadata.productId")
                .first("metrics.total").as("latestTotal"));

        operations.add(Aggregation.sort(Sort.Direction.DESC, "latestTotal"));

        operations.add(Aggregation.limit(limit));

        return mongoTemplate.aggregate(Aggregation.newAggregation(operations), "registries", Document.class)
                .getMappedResults().stream()
                .map(doc -> doc.getString("_id"))
                .filter(Objects::nonNull)
                .toList();
    }

    public Registry save(Registry r){
        return mongoTemplate.save(r, "registries");
    }

    // $dateTrunc (MongoDB 5.0+) is not supported by DocumentDB. This produces an equivalent
    // date-truncation expression using $dateFromParts, which is available from MongoDB 3.6.
    private Document buildDateGroupId(String field, String interval) {
        String unit = interval != null ? interval.toLowerCase() : "day";
        if ("week".equals(unit)) {
            return new Document("$dateFromParts", new Document()
                    .append("isoWeekYear", new Document("$isoWeekYear", field))
                    .append("isoWeek",     new Document("$isoWeek",     field)));
        }
        Document parts = new Document("year", new Document("$year", field));
        switch (unit) {
            case "hour":
                parts.append("month", new Document("$month",      field))
                     .append("day",   new Document("$dayOfMonth", field))
                     .append("hour",  new Document("$hour",       field));
                break;
            case "month":
                parts.append("month", new Document("$month", field));
                break;
            case "year":
                break;
            default: // day
                parts.append("month", new Document("$month",      field))
                     .append("day",   new Document("$dayOfMonth", field));
        }
        return new Document("$dateFromParts", parts);
    }


    public List<Document> getProductsTimelineStats(Collection<String> productRefs, String dataType, int days) {
        if (productRefs == null || productRefs.isEmpty()) {
            return Collections.emptyList();
        }

        Date sinceDate = new Date(System.currentTimeMillis() - ((long) days * 24 * 60 * 60 * 1000));
        List<AggregationOperation> operations = new ArrayList<>();

        operations.add(Aggregation.match(Criteria.where("metadata.productId").in(productRefs)
                .and("metadata.dataType").is(dataType)
                .and("timestamp").gte(sinceDate)));
        
        AggregationOperation groupStage = context -> new Document("$group",
                new Document("_id", buildDateGroupId("$timestamp", "day"))
                        .append("value", new Document("$sum", "$metrics.value"))
        );
        operations.add(groupStage);
        operations.add(Aggregation.sort(Sort.Direction.ASC, "_id"));

        return mongoTemplate.aggregate(Aggregation.newAggregation(operations), "registries", Document.class).getMappedResults();
    }


    public long getTotalViewsForProductReferences(Collection<String> productRefs) {
        if (productRefs == null || productRefs.isEmpty()) return 0;

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("metadata.productId").in(productRefs)
                        .and("metadata.dataType").is(RegistryType.PRODUCT_VIEWS)), // Ojo, sin .name() si se guarda como Enum
                Aggregation.group().sum("metrics.value").as("totalViews")
        );

        Document result = mongoTemplate.aggregate(aggregation, "registries", Document.class).getUniqueMappedResult();
        return result != null && result.get("totalViews") != null ? ((Number) result.get("totalViews")).longValue() : 0;
    }

    public long getTotalSalesForProductReferences(Collection<String> productRefs) {
        if (productRefs == null || productRefs.isEmpty()) return 0;

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("metadata.productId").in(productRefs)
                        .and("metadata.dataType").is(RegistryType.PRODUCT_UNITS_SOLD)),
                Aggregation.sort(Sort.Direction.DESC, "timestamp"),
                Aggregation.group("metadata.productId").first("metrics.total").as("latestTotal"),
                Aggregation.group().sum("latestTotal").as("totalSales")
        );

        Document result = mongoTemplate.aggregate(aggregation, "registries", Document.class).getUniqueMappedResult();
        return result != null && result.get("totalSales") != null ? ((Number) result.get("totalSales")).longValue() : 0;
    }
}