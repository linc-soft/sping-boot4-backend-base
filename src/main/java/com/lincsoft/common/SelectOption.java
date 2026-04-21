package com.lincsoft.common;

/**
 * Generic select option for frontend dropdown/select components.
 *
 * <p>Provides a unified {@code value + label} format that can be directly consumed by frontend UI
 * frameworks (Ant Design, Element Plus, etc.) without field mapping.
 *
 * @param value the option value (entity ID)
 * @param label the option display label (entity name)
 * @author 林创科技
 * @since 2026-04-21
 */
public record SelectOption(Long value, String label) {}
