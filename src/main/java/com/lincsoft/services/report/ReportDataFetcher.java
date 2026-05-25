package com.lincsoft.services.report;

import java.util.Locale;
import java.util.Map;

/**
 * Strategy interface for fetching and assembling report data.
 *
 * <p>Each report type implements this interface to encapsulate its own data querying, grouping, and
 * template variable assembly logic. This keeps report-specific concerns out of domain services and
 * prevents {@link ReportService} from accumulating unrelated report logic.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * @Component
 * public class UserListReportDataFetcher
 *     implements ReportDataFetcher<UserListReportRequest> {
 *
 *   @Override
 *   public String getTemplateName() {
 *     return "reports/user-list-report";
 *   }
 *
 *   @Override
 *   public Map<String, Object> fetchData(UserListReportRequest request, Locale locale) {
 *     // query data, apply grouping, build template variables
 *   }
 * }
 * }</pre>
 *
 * @param <R> the request type for this report
 * @author 林创科技
 * @since 2026-05-25
 */
public interface ReportDataFetcher<R> {

  /**
   * Returns the Thymeleaf template name for this report.
   *
   * @return template name, e.g. {@code "reports/user-list-report"}
   */
  String getTemplateName();

  /**
   * Fetches and assembles the data needed to render this report.
   *
   * <p>The returned map is passed directly to the Thymeleaf template as variables.
   *
   * @param request the report request containing filters and grouping options
   * @param locale the locale for i18n label resolution
   * @return template variables map
   */
  Map<String, Object> fetchData(R request, Locale locale);
}
