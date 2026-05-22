/**
 * PDF 构建器包
 *
 * <p>包含用于构建 PDF 文档的构建器类。
 *
 * <p>主要组件：
 *
 * <ul>
 *   <li>{@link com.lincsoft.pdf.builder.TableBuilder} - 表格构建器，提供流畅的 API 构建 PDF 表格
 * </ul>
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * PdfPTable table = TableBuilder.create(3)
 *     .headers("姓名", "年龄", "城市")
 *     .widths(30f, 20f, 50f)
 *     .row("张三", "25", "北京")
 *     .row("李四", "30", "上海")
 *     .build();
 * }</pre>
 *
 * @author 林创科技
 * @since 2026-04-07
 * @see com.lincsoft.pdf.builder.TableBuilder
 */
package com.lincsoft.pdf.builder;
