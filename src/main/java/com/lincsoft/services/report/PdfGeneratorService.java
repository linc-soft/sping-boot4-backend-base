package com.lincsoft.services.report;

import com.lincsoft.config.PdfConfig;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * PDF generation service using Thymeleaf templates and OpenHTMLToPDF.
 *
 * <p>Renders HTML templates via Thymeleaf with i18n-aware locale support, then converts the
 * resulting HTML to PDF using OpenHTMLToPDF. All registered fonts (including CJ fonts) are
 * automatically applied to the PDF renderer for proper cross-language character rendering.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * Map<String, Object> variables = Map.of("title", "Report", "items", itemList);
 * byte[] pdf = pdfGeneratorService.generatePdf("reports/user-list-report", variables, Locale.JAPANESE);
 * }</pre>
 *
 * @author 林创科技
 * @since 2026-05-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneratorService {

  private final TemplateEngine templateEngine;
  private final PdfConfig pdfConfig;

  /**
   * Generates a PDF document from a Thymeleaf template.
   *
   * <p>Processes the template with the given variables and locale, then converts the rendered HTML
   * to PDF. CJ fonts are automatically registered based on the configuration in {@link PdfConfig}.
   *
   * @param templateName Thymeleaf template name (e.g., {@code "reports/access-log-report"})
   * @param variables template variables accessible via Thymeleaf expressions
   * @param locale the locale for i18n message resolution in the template
   * @return PDF document as a byte array
   * @throws RuntimeException if PDF generation fails
   */
  public byte[] generatePdf(String templateName, Map<String, Object> variables, Locale locale) {
    log.debug("Generating PDF from template: {}, locale: {}", templateName, locale);

    String html = renderTemplate(templateName, variables, locale);
    byte[] pdfBytes = convertHtmlToPdf(html);

    log.debug("PDF generated successfully, size: {} bytes", pdfBytes.length);
    return pdfBytes;
  }

  /**
   * Generates a PDF document and writes it directly to an output stream.
   *
   * <p>This is more memory-efficient than {@link #generatePdf} for large PDF reports, as it avoids
   * buffering the entire PDF in memory as a byte array.
   *
   * @param templateName Thymeleaf template name
   * @param variables template variables
   * @param locale the locale for i18n message resolution
   * @param outputStream the output stream to write the PDF to
   */
  public void generatePdfToStream(
      String templateName,
      Map<String, Object> variables,
      Locale locale,
      OutputStream outputStream) {
    log.debug("Generating PDF to stream from template: {}, locale: {}", templateName, locale);

    String html = renderTemplate(templateName, variables, locale);
    convertHtmlToPdf(html, outputStream);

    log.debug("PDF stream generated successfully");
  }

  private String renderTemplate(String templateName, Map<String, Object> variables, Locale locale) {
    Context context = new Context(locale);
    context.setVariables(variables);
    return templateEngine.process(templateName, context);
  }

  private byte[] convertHtmlToPdf(String html) {
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      convertHtmlToPdf(html, outputStream);
      return outputStream.toByteArray();
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate PDF", e);
    }
  }

  private void convertHtmlToPdf(String html, OutputStream outputStream) {
    try {
      PdfRendererBuilder builder = new PdfRendererBuilder();
      builder.useFastMode();
      pdfConfig.registerFonts(builder);
      builder.withHtmlContent(html, "/");
      builder.toStream(outputStream);
      builder.run();
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert HTML to PDF", e);
    }
  }
}
