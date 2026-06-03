package com.lincsoft.constant;

/**
 * File association type constants for linking uploaded files to business entities.
 *
 * <p>Used as values for {@code sys_file_upload.associate_type} column.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
public final class FileAssociateType {

  private FileAssociateType() {}

  /** File attached to a leave request */
  public static final String LEAVE_ATTACHMENT = "LEAVE_ATTACHMENT";

  /** Regulations document */
  public static final String REGULATIONS = "REGULATIONS";
}
