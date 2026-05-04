package com.tfg.backend.dto;

import com.tfg.backend.model.RegistryType;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PdfExportRequestDTO {
    private Date startDate;
    private Date endDate;
    private EntityType entityType;
    private RegistryType dataType;
    private String metricMode;
    private List<String> storeIds;
    private List<String> productIds;
    private List<String> userIds;
    private List<String> orderIds;
    private String interval;
    private String chartImage;
}