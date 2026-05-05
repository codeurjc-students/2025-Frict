package com.tfg.backend.config;

import com.tfg.backend.model.Notification;
import com.tfg.backend.event.NotificationChangeStreamListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.messaging.ChangeStreamRequest;
import org.springframework.data.mongodb.core.messaging.DefaultMessageListenerContainer;
import org.springframework.data.mongodb.core.messaging.MessageListenerContainer;

@Configuration
public class ChangeStreamConfig {

    @Bean
    public MessageListenerContainer messageListenerContainer(MongoTemplate template,
                                                             NotificationChangeStreamListener listener) {
        MessageListenerContainer container = new DefaultMessageListenerContainer(template);
        container.start();

        // Ya no hace falta forzar los genéricos, el builder lo lee del propio listener
        ChangeStreamRequest<Notification> request = ChangeStreamRequest.builder(listener)
                .collection("notifications")
                .build();

        container.register(request, Notification.class);
        return container;
    }
}