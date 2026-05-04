package com.tfg.backend.repository;

import com.tfg.backend.model.UserConnection;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConnectionRepository extends MongoRepository<UserConnection, String> {
}
