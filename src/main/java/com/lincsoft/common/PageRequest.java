package com.lincsoft.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Set;
import lombok.Data;

/**
 * Page request base class.
 *
 * <p>Supports server-side sorting via {@code sortBy} and {@code sortOrder} parameters. Sort fields
 * are sent from the frontend in camelCase (matching JSON property names) and are automatically
 * converted to snake_case (matching database column names) in {@link #applySorting}.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Data
public abstract class PageRequest {
  /** Page number */
  @Min(value = 1, message = "Page number must be greater than or equal to 1")
  private int page;

  /** Page size */
  @Min(value = 10, message = "Page size must be greater than or equal to 10")
  @Max(value = 100, message = "Page size must be less than or equal to 100")
  private int size;

  /**
   * Comma-separated field names for sorting (camelCase), e.g. {@code "username,updateAt"}. Only
   * fields whose snake_case equivalent appears in {@code allowedColumns} are applied.
   */
  private String sortBy;

  /**
   * Comma-separated sort orders corresponding to {@link #sortBy}, e.g. {@code "asc,desc"}. If
   * shorter than {@code sortBy}, missing entries default to {@code "asc"}.
   */
  private String sortOrder;

  /** Get MyBatis-Plus pagination object */
  public <T> Page<T> toPage() {
    return new Page<>(page, size);
  }

  /**
   * Apply sorting to a QueryWrapper based on {@code sortBy}/{@code sortOrder} parameters.
   *
   * <p>Only columns in {@code allowedColumns} are accepted; invalid or disallowed columns are
   * silently ignored. If no sorting is specified, falls back to {@code defaultColumn}/{@code
   * defaultOrder}.
   *
   * @param wrapper the QueryWrapper to apply sorting to
   * @param allowedColumns set of allowed snake_case column names
   * @param defaultColumn the default snake_case column to sort by when no sorting is specified
   */
  public <T> void applySorting(
      QueryWrapper<T> wrapper, Set<String> allowedColumns, String defaultColumn) {
    applySorting(wrapper, allowedColumns, defaultColumn, "desc");
  }

  /**
   * Apply sorting to a QueryWrapper based on {@code sortBy}/{@code sortOrder} parameters.
   *
   * <p>Only columns in {@code allowedColumns} are accepted; invalid or disallowed columns are
   * silently ignored. If no sorting is specified, falls back to {@code defaultColumn}/{@code
   * defaultOrder}.
   *
   * @param wrapper the QueryWrapper to apply sorting to
   * @param allowedColumns set of allowed snake_case column names
   * @param defaultColumn the default snake_case column to sort by when no sorting is specified
   * @param defaultOrder the default sort order ("asc" or "desc")
   */
  public <T> void applySorting(
      QueryWrapper<T> wrapper,
      Set<String> allowedColumns,
      String defaultColumn,
      String defaultOrder) {
    if (sortBy != null && !sortBy.isBlank()) {
      String[] fields = sortBy.split(",");
      String[] orders =
          sortOrder != null && !sortOrder.isBlank() ? sortOrder.split(",") : new String[0];
      for (int i = 0; i < fields.length; i++) {
        String field = fields[i].trim();
        String column = camelToSnake(field);
        if (!allowedColumns.contains(column)) {
          continue;
        }
        String order = i < orders.length ? orders[i].trim().toLowerCase() : "asc";
        if ("desc".equals(order)) {
          wrapper.orderByDesc(column);
        } else {
          wrapper.orderByAsc(column);
        }
      }
    } else if (defaultColumn != null) {
      if ("desc".equalsIgnoreCase(defaultOrder)) {
        wrapper.orderByDesc(defaultColumn);
      } else {
        wrapper.orderByAsc(defaultColumn);
      }
    }
  }

  /** Convert camelCase to snake_case, e.g. {@code "updateAt"} → {@code "update_at"}. */
  private static String camelToSnake(String camelCase) {
    return camelCase.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }
}
