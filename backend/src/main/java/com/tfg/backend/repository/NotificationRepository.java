package com.tfg.backend.repository;

import com.tfg.backend.model.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUsernameAndIsReadFalseOrderByTimestampDesc(String username);
}