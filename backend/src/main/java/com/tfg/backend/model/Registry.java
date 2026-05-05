package com.tfg.backend.model;

import com.tfg.backend.dto.EntityType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.TimeSeries;
import org.springframework.data.mongodb.core.timeseries.Granularity;

import java.util.Date;

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

    public Registry(EntityType entityType, RegistryType dataType,
                    Double value,
                    String shopId, String shopName,
                    String userId, String userName,
                    String productId, String productName,
                    String orderId, String orderName) {

        this.timestamp = new Date();

        this.metadata = new Metadata();
        this.metadata.setEntityType(entityType);
        this.metadata.setDataType(dataType);

        this.metadata.setShopId(shopId);
        this.metadata.setShopName(shopName);
        this.metadata.setUserId(userId);
        this.metadata.setUserName(userName);
        this.metadata.setProductId(productId);
        this.metadata.setProductName(productName);
        this.metadata.setOrderId(orderId);
        this.metadata.setOrderName(orderName);

        this.metrics = new Metrics(value, null);
    }

    @Setter
    @Getter
    public static class Metadata {
        private EntityType entityType;
        private RegistryType dataType;

        private String shopId;
        private String userId;
        private String productId;
        private String orderId;

        private String shopName;
        private String userName;
        private String productName;
        private String orderName;

        public Metadata() {}
    }

    @Setter
    @Getter
    public static class Metrics {
        private Double value;
        private Double total;

        public Metrics() {}

        public Metrics(Double value, Double total) {
            this.value = value;
            this.total = total;
        }
    }
}