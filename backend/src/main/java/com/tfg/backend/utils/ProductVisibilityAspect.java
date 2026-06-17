package com.tfg.backend.utils;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class ProductVisibilityAspect {

    private final EntityManager entityManager;

    // Intercepts any method within repository package
    @Around("execution(* com.tfg.backend.repository..*(..))")
    public Object applyFilter(ProceedingJoinPoint joinPoint) throws Throwable {

        Session session = entityManager.unwrap(Session.class);

        if (shouldFilterProducts()) {
            session.enableFilter("activeProductFilter");
            session.enableFilter("activeProductReviewFilter");
        } else {
            session.disableFilter("activeProductFilter");
            session.disableFilter("activeProductReviewFilter");
        }

        return joinPoint.proceed();
    }

    private boolean shouldFilterProducts() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Filter products with anon users or logged users
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return true;
        }

        // Do not filter If user does have ADMIN or MANAGER role
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ADMIN") || a.getAuthority().equals("MANAGER"));

        return !isAdmin;
    }
}