/**
 * PDF 生成模块
 *
 * <p>提供统一的 PDF 文件生成功能，支持角色和用户数据导出。
 *
 * <p>包含以下子包： - config: PDF 配置类，包含模板配置、字体配置等 - service: PDF 生成相关服务，包括核心生成器、角色 PDF 服务、用户 PDF 服务 -
 * controller: REST API 控制器，处理 PDF 生成请求 - exception: PDF 生成相关异常类 - builder: PDF 构建器，提供流畅的 API 构建 PDF
 * 文档 - dto: 数据传输对象，包含请求参数和响应结果
 *
 * @see com.lincsoft.pdf.config.PdfTemplateConfig
 * @see com.lincsoft.pdf.service.PdfGeneratorService
 */
package com.lincsoft.pdf;
