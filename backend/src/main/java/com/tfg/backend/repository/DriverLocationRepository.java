package com.tfg.backend.repository;

import com.tfg.backend.model.DriverLocation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DriverLocationRepository extends MongoRepository<DriverLocation, String> {
}