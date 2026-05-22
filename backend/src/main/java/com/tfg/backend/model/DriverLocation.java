package com.tfg.backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "driver_locations")
public class DriverLocation {

    @Id
    private String driverUsername;
    private String driverName;
    private Date pingDateTime;
    private AddressSnapshot address;
}