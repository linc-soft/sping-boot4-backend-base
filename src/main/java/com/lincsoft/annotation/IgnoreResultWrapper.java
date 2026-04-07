package com.lincsoft.annotation;

import java.lang.annotation.*;

/**
 * Annotation that ignores the unified response format wrapper.
 *
 * <p>When applied to a method or class, the return value will not be automatically wrapped into
 * Result.
 *
 * @author 林创科技
 * @since 2026-04-07
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreResultWrapper {}
