package com.lincsoft.dto.leave;

import com.lincsoft.entity.leave.Leave;
import com.lincsoft.entity.system.SysFileUpload;
import java.util.List;

/**
 * DTO combining a Leave entity with its associated file uploads.
 *
 * @param leave The leave entity
 * @param files Associated file uploads
 * @author lincsoft
 * @since 2026-06-03
 */
public record LeaveWithFiles(Leave leave, List<SysFileUpload> files) {}
