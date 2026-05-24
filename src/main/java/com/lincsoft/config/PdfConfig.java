package com.lincsoft.config;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * PDF font configuration and registry.
 *
 * <p>Auto-discovers and registers font files from the configured font directory at application
 * startup. Supports both classpath resources (e.g., {@code classpath:/fonts/}) and filesystem paths
 * (e.g., {@code /opt/fonts/}).
 *
 * <p>Registered fonts are applied to {@link PdfRendererBuilder} instances during PDF generation,
 * enabling CJ (Chinese/Japanese) character rendering in PDF reports.
 *
 * @author 林创科技
 * @since 2026-05-23
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfConfig {

  private final AppProperties appProperties;
  private final ResourceLoader resourceLoader;

  /** List of discovered font metadata entries. */
  private final List<FontEntry> fontEntries = new ArrayList<>();

  /** Whether CJ fonts are available. */
  private boolean cjFontsAvailable = false;

  /**
   * Discover and register fonts at application startup.
   *
   * <p>Scans the configured font directory for {@code .ttf} and {@code .otf} files. Derives font
   * family names and weights from file names using a naming convention:
   *
   * <ul>
   *   <li>Files containing "Bold" (case-insensitive) are registered as bold weight (700)
   *   <li>Files containing "Light" are registered as light weight (300)
   *   <li>All other files are registered as normal weight (400)
   *   <li>Files containing "Italic" are registered as italic style
   * </ul>
   *
   * <p>Font family names are extracted from the file name by removing weight/style suffixes and
   * suffixes like "SC" (Simplified Chinese) or "JP" (Japanese).
   */
  @PostConstruct
  public void init() {
    String fontPath = normalizeFontPath(appProperties.getReport().getFontPath());
    log.info("Scanning font directory for PDF generation: {}", fontPath);

    try {
      if (fontPath.startsWith("classpath:")) {
        // Spring's ClassPathResource.exists() is unreliable for directories,
        // so probe individual font files directly.
        scanClasspathFonts(fontPath);
      } else {
        Resource resource = resourceLoader.getResource(fontPath);
        if (!resource.exists()) {
          log.warn(
              "Font directory not found: {}. PDF reports will use fallback fonts."
                  + " CJ characters may not render correctly.",
              fontPath);
          return;
        }
        scanFilesystemFonts(resource);
      }

      if (fontEntries.isEmpty()) {
        log.warn(
            "No font files found in: {}. PDF reports will use fallback fonts."
                + " CJ characters may not render correctly."
                + " Download Noto Sans fonts from https://fonts.google.com/noto"
                + " and place them in the font directory.",
            fontPath);
      } else {
        log.info("Registered {} font(s) for PDF generation", fontEntries.size());
        checkCjFonts();
      }

    } catch (Exception e) {
      log.error("Failed to scan font directory: {}", fontPath, e);
    }
  }

  /**
   * Register all discovered fonts with the given PDF renderer builder.
   *
   * <p>This method must be called for each new {@link PdfRendererBuilder} instance before
   * generating a PDF. It configures all discovered fonts so that the renderer can use them when
   * processing HTML/CSS {@code font-family} declarations.
   *
   * @param builder the PDF renderer builder to register fonts with
   */
  public void registerFonts(PdfRendererBuilder builder) {
    for (FontEntry entry : fontEntries) {
      try {
        builder.useFont(
            entry::openStream,
            entry.getFamily(),
            entry.getWeight(),
            entry.isItalic() ? FontStyle.ITALIC : FontStyle.NORMAL,
            true);
        log.trace(
            "Registered font: {} (weight={}, italic={})",
            entry.getFamily(),
            entry.getWeight(),
            entry.isItalic());
      } catch (Exception e) {
        log.warn("Failed to register font {}: {}", entry.getFileName(), e.getMessage());
      }
    }
  }

  /**
   * Returns whether CJ (Chinese/Japanese) fonts are available.
   *
   * <p>When CJ fonts are not available, PDF reports may render Chinese/Japanese characters as blank
   * squares or question marks. Callers should check this flag and warn users if CJ content is
   * expected.
   *
   * @return true if at least one CJ font is registered
   */
  public boolean isCjFontsAvailable() {
    return cjFontsAvailable;
  }

  /**
   * Normalizes the font path to ensure it ends with a slash.
   *
   * @param fontPath raw font path from configuration
   * @return normalized font path with trailing slash
   */
  private String normalizeFontPath(String fontPath) {
    if (fontPath == null || fontPath.isBlank()) {
      return "classpath:/fonts/";
    }
    return fontPath.endsWith("/") ? fontPath : fontPath + "/";
  }

  /**
   * Scans for known Noto font files in the classpath.
   *
   * <p>Since classpath scanning for resources inside JARs is non-trivial, this method probes for a
   * fixed list of known font file names. To support custom fonts, add them to the {@code
   * commonFonts} array or use a filesystem-based font path.
   *
   * @param fontPath the classpath font directory (e.g., {@code classpath:/fonts/})
   */
  private void scanClasspathFonts(String fontPath) {
    String[] commonFonts = {
      "NotoSansSC-Regular.ttf", "NotoSansSC-Bold.ttf",
      "NotoSansJP-Regular.ttf", "NotoSansJP-Bold.ttf",
      "NotoSans-Regular.ttf", "NotoSans-Bold.ttf"
    };

    for (String fileName : commonFonts) {
      Resource resource = resourceLoader.getResource(fontPath + fileName);
      if (resource.exists()) {
        addFontEntry(fileName, resource);
      }
    }
  }

  private void scanFilesystemFonts(Resource resource) throws IOException {
    java.io.File fontDir = resource.getFile();
    if (!fontDir.isDirectory()) {
      log.warn("Font path is not a directory: {}", fontDir.getAbsolutePath());
      return;
    }

    java.io.File[] files =
        fontDir.listFiles(
            (dir, name) -> {
              String lower = name.toLowerCase();
              return lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc");
            });

    if (files != null) {
      for (java.io.File file : files) {
        addFontEntry(file.getName(), new org.springframework.core.io.FileSystemResource(file));
      }
    }
  }

  private void addFontEntry(String fileName, Resource resource) {
    FontEntry entry = parseFontFileName(fileName);
    if (entry != null) {
      entry.setFileName(fileName);
      entry.setResource(resource);
      fontEntries.add(entry);
      log.info(
          "Discovered font: {} -> family='{}', weight={}",
          fileName,
          entry.getFamily(),
          entry.getWeight());
    }
  }

  private FontEntry parseFontFileName(String fileName) {
    String name = fileName.replaceAll("\\.(ttf|otf|ttc)$", "");

    boolean italic = name.toLowerCase().contains("italic");
    int weight = 400;

    String nameLower = name.toLowerCase();
    if (nameLower.contains("bold") || nameLower.contains("heavy")) {
      weight = 700;
    } else if (nameLower.contains("medium")) {
      weight = 500;
    } else if (nameLower.contains("light") || nameLower.contains("thin")) {
      weight = 300;
    }

    String family = name;
    family = family.replaceAll("(?i)(-|\\s)?(Bold|Medium|Light|Thin|Heavy|Italic|Regular)$", "");
    family = family.replaceAll("([a-z])([A-Z])", "$1 $2");

    if (family.isBlank()) {
      return null;
    }

    FontEntry entry = new FontEntry();
    entry.setFamily(family);
    entry.setWeight(weight);
    entry.setItalic(italic);
    return entry;
  }

  private void checkCjFonts() {
    cjFontsAvailable =
        fontEntries.stream()
            .anyMatch(
                e -> {
                  String familyLower = e.getFamily().toLowerCase();
                  String fileLower = e.getFileName().toLowerCase();
                  return familyLower.contains("noto")
                      && (fileLower.contains("sc") || fileLower.contains("jp"));
                });

    if (cjFontsAvailable) {
      log.info("CJ fonts available for PDF generation");
    } else {
      log.warn(
          "No CJ fonts found. Chinese/Japanese characters in PDF reports may not render"
              + " correctly. Install Noto Sans SC/JP fonts in the font directory.");
    }
  }

  /**
   * Font metadata entry for PDF font registration.
   *
   * <p>Stores the derived font family name, weight, style, file name, and Spring Resource for each
   * discovered font file. The Resource reference allows the font stream to be opened lazily by the
   * PDF renderer without requiring access to the outer class's dependencies.
   */
  @Data
  public static class FontEntry {
    /** Font family name derived from the file name. */
    private String family;

    /** Font weight (e.g., 300=Light, 400=Regular, 700=Bold). */
    private int weight = 400;

    /** Whether this is an italic font variant. */
    private boolean italic = false;

    /** Original font file name. */
    private String fileName;

    /** Spring Resource pointing to the font file (classpath or filesystem). */
    private Resource resource;

    /**
     * Opens an InputStream for the font file.
     *
     * <p>Wraps the checked {@link IOException} thrown by {@link Resource#getInputStream()} as an
     * {@link UncheckedIOException} so this method can be used as a method reference for {@code
     * FSSupplier<InputStream>}, which does not declare checked exceptions.
     *
     * @return InputStream for the font file
     * @throws UncheckedIOException if the font file cannot be read
     */
    public InputStream openStream() {
      try {
        return resource.getInputStream();
      } catch (IOException e) {
        throw new UncheckedIOException("Failed to open font file: " + fileName, e);
      }
    }
  }
}
