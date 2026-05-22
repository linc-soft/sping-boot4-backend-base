# PDF 字体文件目录

本目录存放 PDF 生成所需的内嵌字体文件。

## 必需字体

以下字体文件是应用启动的必要条件：

- `NotoSansSC-Regular.ttf` - 简体中文字体（支持 GB2312 字符集）

## 可选字体

以下字体文件如果不存在，系统会记录警告但不会阻止应用启动：

- `NotoSansJP-Regular.ttf` - 日文字体

## 获取字体

### 方式一：下载完整字体

从以下地址下载 Noto Sans 字体：

1. **Noto Sans SC（简体中文）**
   - 下载地址：https://fonts.google.com/noto/specimen/Noto+Sans+SC
   - 或 GitHub：https://github.com/notofonts/noto-cjk
   - 许可证：SIL Open Font License 1.1

2. **Noto Sans JP（日文）**
   - 下载地址：https://fonts.google.com/noto/specimen/Noto+Sans+JP
   - 许可证：SIL Open Font License 1.1

### 方式二：使用外部字体路径

可以通过配置文件指定外部字体路径，避免将字体文件打包到应用中：

```yaml
pdf:
  template:
    font:
      paths:
        chinese: ${PDF_CHINESE_FONT_PATH:/path/to/NotoSansSC-Regular.ttf}
        japanese: ${PDF_JAPANESE_FONT_PATH:/path/to/NotoSansJP-Regular.ttf}
```

或通过环境变量：

```bash
export PDF_CHINESE_FONT_PATH=/path/to/NotoSansSC-Regular.ttf
export PDF_JAPANESE_FONT_PATH=/path/to/NotoSansJP-Regular.ttf
```

## 字体要求

### 中文字体要求

- 必须支持 GB2312 字符集的简体中文字符
- 推荐使用 TrueType 或 OpenType 格式
- 文件大小建议控制在 10MB 以内

### 日文字体要求

- 必须支持日文假名、汉字字符
- 推荐使用 TrueType 或 OpenType 格式

## 故障排除

### 应用启动失败

如果看到以下错误消息：

```
PdfFontException: Embedded Chinese font file not found: fonts/NotoSansSC-Regular.ttf
```

请确保：

1. 字体文件已放置在本目录下
2. 或通过 `pdf.template.font.paths.chinese` 配置了外部字体路径

### PDF 中文显示为方框

如果 PDF 中的中文显示为方框，可能是：

1. 字体文件损坏或不完整
2. 字体不支持所需的字符
3. PDF 生成时字体加载失败（检查日志中的警告信息）
