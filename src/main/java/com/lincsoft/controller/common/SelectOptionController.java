package com.lincsoft.controller.common;

import com.lincsoft.common.SelectOption;
import com.lincsoft.common.SelectOptionProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Select option controller for frontend dropdown/select components.
 *
 * <p>Provides a unified API endpoint for retrieving select option lists from various entity types.
 * Each entity type is handled by a {@link SelectOptionProvider} implementation that is
 * automatically discovered and registered via Spring dependency injection.
 *
 * <p>To add support for a new entity type, simply create a new {@link SelectOptionProvider}
 * implementation annotated with {@code @Component}. No modification to this controller is required.
 *
 * <p>Example: {@code GET /api/common/select-options?type=role}
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@RestController
@RequestMapping("/api/common/select-options")
public class SelectOptionController {

  /** Registry of select option providers, keyed by type identifier. */
  private final Map<String, SelectOptionProvider> providerMap;

  /**
   * Constructs the controller with all available select option providers.
   *
   * <p>Spring automatically injects all beans implementing {@link SelectOptionProvider}. The
   * providers are indexed by their {@link SelectOptionProvider#getType()} value for O(1) lookup.
   *
   * @param providers list of all SelectOptionProvider implementations
   */
  public SelectOptionController(List<SelectOptionProvider> providers) {
    this.providerMap =
        providers.stream()
            .collect(Collectors.toMap(SelectOptionProvider::getType, Function.identity()));
  }

  /**
   * Get select option list by type.
   *
   * @param type the entity type identifier (e.g., "role", "user")
   * @return list of select options with value and label
   * @throws IllegalArgumentException if the type is not supported
   */
  @GetMapping
  public List<SelectOption> getSelectOptions(@RequestParam String type) {
    SelectOptionProvider provider = providerMap.get(type);
    if (provider == null) {
      throw new IllegalArgumentException("Unsupported select option type: " + type);
    }
    return provider.getOptions();
  }
}
