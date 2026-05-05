package com.tfg.backend.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextAsyncConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() {
        // Establishes that the children threads (as the ones created in @Async methods) inherit the parent Spring Security Context
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }
}
