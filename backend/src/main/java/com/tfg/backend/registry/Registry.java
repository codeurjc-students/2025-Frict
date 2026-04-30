package com.tfg.backend.registry;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.TimeSeries;
import org.springframework.data.mongodb.core.timeseries.Granularity;

import java.util.Date;
import java.util.Map;

@Getter
@Setter
@TimeSeries(collection = "registries", timeField = "timestamp", metaField = "metadata", granularity = Granularity.HOURS)
public class Registry {

    @Id
    private String id;
    private Date timestamp;
    private Metadata metadata;
    private Metrics metrics;

    public Registry() {}

    public Registry(Date timestamp, Metadata metadata, Metrics metrics) {
        this.timestamp = timestamp;
        this.metadata = metadata;
        this.metrics = metrics;
    }

    @Setter
    @Getter
    public static class Metadata {
        private String entityType;
        private String entityId;
        private String dataType;

        private String storeId;
        private String userId;
        private String productId;
        private String orderId;

        public Metadata() {}

        public Metadata(String entityType, String entityId, String dataType) {
            this.entityType = entityType;
            this.entityId = entityId;
            this.dataType = dataType;
        }

    }

    @Setter
    @Getter
    public static class Metrics {
        private double value;
        private Map<String, Object> details;

        public Metrics() {}

        public Metrics(double value, Map<String, Object> details) {
            this.value = value;
            this.details = details;
        }
    }
}