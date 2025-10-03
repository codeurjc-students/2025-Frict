package com.tfg.backend.utils;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/*
Handles the format exceptions thrown up from the services to the controllers:

EntityNotFoundException: An object could not be found in database
IllegalArgumentException / MethodArgumentNotValidException: The object that is saving in the database does not have the right fields or format

 */

@ControllerAdvice(basePackages = "com.tfg.backend.controller")
public class ServiceErrorHandler {

    @ExceptionHandler({ EntityNotFoundException.class, IllegalArgumentException.class, MethodArgumentNotValidException.class })
    public ResponseEntity<?> handleException(Exception ex) {
        return switch (ex) {
            case MethodArgumentNotValidException manvExp -> {
                var fieldError = manvExp.getFieldError();
                yield ResponseEntity.badRequest().body(fieldError != null ? fieldError.getDefaultMessage() : "Validation error");
            }
            case EntityNotFoundException enfExp -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(enfExp.getMessage());
            default -> ResponseEntity.badRequest().body(ex.getMessage());
        };
    }

}
