package com.tfg.backend.utils;

/**
 * A generic wrapper used to return the result of a "save" repository operation when creation or update is decided by
 * the corresponding service.
 * It helps the controller determine the appropriate HTTP status code (200 OK vs 201 Created).
 *
 * @param <T>  The type of the entity being processed (e.g., OrderItem, Product, Category).
 * @param data The actual entity or DTO resulting from the operation.
 * @param isNew A boolean flag indicating if a new resource was created (true)
 * or if an existing one was updated (false).
 */
public record SaveResult<T>(T data, boolean isNew) {
}
