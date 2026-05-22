package com.lincsoft.pdf.dto;

/**
 * PDF 生成结果
 *
 * <p>封装 PDF 生成完成后的字节数组和文件名。
 *
 * <p><b>Validates: Requirements 1.8, 1.9, 2.5, 2.8, 3.8, 3.9, 4.5, 4.9</b>
 *
 * @param content PDF 字节数组
 * @param filename 文件名
 * @author 林创科技
 * @since 2026-04-29
 */
public record PdfResult(byte[] content, String filename) {}
