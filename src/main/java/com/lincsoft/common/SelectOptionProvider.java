package com.lincsoft.common;

import java.util.List;

/**
 * Strategy interface for providing select option data.
 *
 * <p>Each implementation provides select options for a specific entity type (e.g., roles, users).
 * Implementations are automatically discovered by Spring and registered in the {@code
 * SelectOptionController} via constructor injection.
 *
 * <p>To add a new select option type, simply create a new {@code @Component} class implementing
 * this interface. No controller modification is required (Open-Closed Principle).
 *
 * @author 林创科技
 * @since 2026-04-21
 */
public interface SelectOptionProvider {

  /**
   * Returns the type identifier for this provider.
   *
   * <p>Used as the {@code type} query parameter value in the API endpoint. Must be unique across
   * all providers (e.g., "role", "user").
   *
   * @return the type identifier string
   */
  String getType();

  /**
   * Returns the list of select options for this entity type.
   *
   * @return list of select options with value (ID) and label (display name)
   */
  List<SelectOption> getOptions();
}
