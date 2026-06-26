package com.tfg.backend.config;

import com.tfg.backend.event.NotificationChangeStreamListener;
import com.tfg.backend.model.Notification;
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

        // Generics are inferred from the listener; no need to force them on the builder
        ChangeStreamRequest<Notification> request = ChangeStreamRequest.builder(listener)
                .collection("notifications")
                .build();

        container.register(request, Notification.class);
        return container;
    }
}