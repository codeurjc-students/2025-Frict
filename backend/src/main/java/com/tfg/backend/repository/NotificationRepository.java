package com.tfg.backend.repository;

import com.tfg.backend.dto.EntityType;
import com.tfg.backend.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    List<Notification> findByUsernameAndIsReadFalseOrderByTimestampDesc(String username);
    Page<Notification> findByUsernameOrderByTimestampDesc(String username, Pageable pageable);
    Page<Notification> findByUsernameAndTypeOrderByTimestampDesc(String username, EntityType type, Pageable pageable);
    void deleteByIdAndUsername(String id, String username);
    long countByUsernameAndTimestampAfter(String username, Instant timestamp);
}