package com.tfg.backend.notification;

import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUsernameAndIsReadFalseOrderByTimestampDesc(String username);
}