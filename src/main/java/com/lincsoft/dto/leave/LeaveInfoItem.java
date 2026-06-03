package com.lincsoft.dto.leave;

import com.lincsoft.constant.LeaveType;
import java.math.BigDecimal;

/**
 * Leave type usage item DTO.
 *
 * @param leaveType Leave type code
 * @param leaveDays Total days used for this type
 * @author lincsoft
 * @since 2026-06-03
 */
public record LeaveInfoItem(LeaveType leaveType, BigDecimal leaveDays) {}
