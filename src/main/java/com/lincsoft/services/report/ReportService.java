package com.lincsoft.services.report;

import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Report coordination service.
 *
 * <p>Delegates data fetching to a {@link ReportDataFetcher} implementation and PDF rendering to
 * {@link PdfGeneratorService}. Each report type has its own Fetcher, keeping this service thin and
 * free of report-specific logic.
 *
 * @author 林创科技
 * @since 2026-05-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

  private final PdfGeneratorService pdfGeneratorService;

  /**
   * Generates a PDF report using the given fetcher and request.
   *
   * <p>The fetcher provides the template name and assembles the data; this service coordinates the
   * overall generation flow.
   *
   * @param <R> the request type
   * @param fetcher the report-specific data fetcher
   * @param request the report request containing filters and options
   * @param locale the locale for i18n label resolution
   * @return PDF document as a byte array
   */
  public <R> byte[] generateReport(ReportDataFetcher<R> fetcher, R request, Locale locale) {
    String templateName = fetcher.getTemplateName();
    log.debug("Generating report from template: {}, locale: {}", templateName, locale);

    Map<String, Object> variables = fetcher.fetchData(request, locale);
    byte[] pdfBytes = pdfGeneratorService.generatePdf(templateName, variables, locale);

    log.debug("Report generated successfully, size: {} bytes", pdfBytes.length);
    return pdfBytes;
  }
}
