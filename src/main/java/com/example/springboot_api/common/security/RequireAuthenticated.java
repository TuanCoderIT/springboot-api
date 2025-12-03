package com.example.springboot_api.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Annotation để đánh dấu method/class cần authentication
 * Sử dụng: @RequireAuthenticated thay vì @PreAuthorize("isAuthenticated()")
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("isAuthenticated()")
public @interface RequireAuthenticated {
}

