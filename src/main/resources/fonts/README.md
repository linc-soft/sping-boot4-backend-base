# PDF Report Font Setup

This directory contains font files used for PDF report generation.

## Required Fonts for CJ Support

To properly render Chinese and Japanese characters in PDF reports, download the following Noto Sans fonts and place them in this directory:

### Download Links

Download from [Google Noto Fonts](https://fonts.google.com/noto):

| Font | File Name | Download URL |
|------|-----------|--------------|
| Noto Sans SC Regular | `NotoSansSC-Regular.ttf` | https://fonts.google.com/noto/specimen/Noto+Sans+SC |
| Noto Sans SC Bold | `NotoSansSC-Bold.ttf` | https://fonts.google.com/noto/specimen/Noto+Sans+SC |
| Noto Sans JP Regular | `NotoSansJP-Regular.ttf` | https://fonts.google.com/noto/specimen/Noto+Sans_JP |
| Noto Sans JP Bold | `NotoSansJP-Bold.ttf` | https://fonts.google.com/noto/specimen/Noto+Sans_JP |

### Recommended (Optional)

| Font | File Name | Purpose |
|------|-----------|---------|
| Noto Sans Regular | `NotoSans-Regular.ttf` | Latin character fallback |
| Noto Sans Bold | `NotoSans-Bold.ttf` | Latin bold fallback |

## Font Naming Convention

The font auto-discovery system derives font family names and weights from file names:

- Files containing **Bold** → registered as bold weight (700)
- Files containing **Light** → registered as light weight (300)
- Others → registered as normal weight (400)
- Suffixes like **SC** (Simplified Chinese), **JP** (Japanese) are stripped from the family name

Examples:
- `NotoSansSC-Regular.ttf` → family: `Noto Sans SC`, weight: 400
- `NotoSansSC-Bold.ttf` → family: `Noto Sans SC`, weight: 700
- `NotoSansJP-Regular.ttf` → family: `Noto Sans JP`, weight: 400
- `NotoSans-Bold.ttf` → family: `Noto Sans`, weight: 700

## Configuration

Font directory is configured in `application.yml`:

```yaml
app:
  report:
    font-path: classpath:/fonts/    # For embedded fonts (JAR)
    # font-path: file:/opt/fonts/   # For external filesystem fonts
```

## License

Noto Sans fonts are released under the [SIL Open Font License (OFL)](https://scripts.sil.org/OFL), which permits free commercial use.