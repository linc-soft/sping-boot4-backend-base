package com.lincsoft.common;

/**
 * Generic select option for frontend dropdown/select components.
 *
 * <p>Provides a unified {@code value + label + description} format that can be directly consumed by
 * frontend UI frameworks (Ant Design, Element Plus, etc.) without field mapping.
 *
 * <p>The {@code value} field is typed as {@link Object} to allow either a numeric ID ({@link Long})
 * or a string identifier (such as {@code username}). This matches the frontend Zod schema {@code
 * z.union([z.string(), z.number()])}. Jackson serializes a {@link Long} as a JSON number and a
 * {@link String} as a JSON string automatically; no custom serializer is required.
 *
 * <p>Construction must go through one of the {@link #of} static factory methods, which restrict the
 * runtime type of {@code value} to {@link Long} or {@link String}.
 *
 * @param value the option value: either {@link Long} (entity ID) or {@link String} (e.g. username)
 * @param label the option display label (entity name)
 * @param description the option description (optional additional info)
 * @author 林创科技
 * @since 2026-04-21
 */
public record SelectOption(Object value, String label, String description) {

  public static SelectOption of(Long value, String label, String description) {
    return new SelectOption(value, label, description);
  }

  public static SelectOption of(String value, String label, String description) {
    return new SelectOption(value, label, description);
  }

  public static SelectOption of(Long value, String label) {
    return new SelectOption(value, label, null);
  }

  public static SelectOption of(String value, String label) {
    return new SelectOption(value, label, null);
  }
}
