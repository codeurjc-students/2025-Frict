package com.tfg.backend.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUsernameAndIsReadFalseOrderByTimestampDesc(String username);
    Page<Notification> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
    void deleteByIdAndUsername(String id, String username);
}