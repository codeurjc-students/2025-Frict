package com.tfg.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationLocationDTO {
    private NotificationDTO notification;
    private int pageIndex;
}
