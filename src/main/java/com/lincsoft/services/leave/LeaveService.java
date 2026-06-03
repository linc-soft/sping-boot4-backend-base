package com.lincsoft.services.leave;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.*;
import com.lincsoft.constant.Module;
import com.lincsoft.controller.leave.vo.*;
import com.lincsoft.dto.leave.AnnualLeaveCalculation;
import com.lincsoft.dto.leave.LeaveInfoItem;
import com.lincsoft.dto.leave.LeaveWithFiles;
import com.lincsoft.entity.leave.AnnualLeave;
import com.lincsoft.entity.leave.Leave;
import com.lincsoft.entity.master.Employee;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.entity.system.SysFileUpload;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.leave.AnnualLeaveMapper;
import com.lincsoft.mapper.leave.LeaveMapper;
import com.lincsoft.mapper.master.EmployeeMapper;
import com.lincsoft.mapper.master.MstUserMapper;
import com.lincsoft.services.system.FileUploadService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leave service for managing leave requests and annual leave balances.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveService {

  private final LeaveMapper leaveMapper;
  private final AnnualLeaveMapper annualLeaveMapper;
  private final MstUserMapper userMapper;
  private final EmployeeMapper employeeMapper;
  private final FileUploadService fileUploadService;

  @Lazy private final LeaveService self;

  /**
   * Create a new leave request.
   *
   * @param leave the leave entity
   * @param fileIds file upload IDs to associate
   * @param approverId the current user ID (for self-approval check)
   * @return the created leave ID
   */
  @OperationLog(
      module = Module.LEAVE,
      subModule = SubModule.LEAVE_APPLY,
      type = OperationType.CREATE,
      description = "Leave request created")
  @Transactional(rollbackFor = Exception.class)
  public Long createLeave(Leave leave, List<Long> fileIds, Long approverId) {
    LocalDate startDate = leave.getStartDate();
    LocalDate endDate = leave.getEndDate();

    // Validate dates
    if (startDate.isAfter(endDate)) {
      throw new BusinessException(MessageEnums.LEAVE_DATE_START_AFTER_END);
    }

    if (startDate.getYear() != endDate.getYear()) {
      throw new BusinessException(MessageEnums.LEAVE_DATE_NOT_SAME_YEAR);
    }

    // Validate duration is multiple of 0.5
    if (leave.getDuration().remainder(BigDecimal.valueOf(0.5)).compareTo(BigDecimal.ZERO) != 0) {
      throw new BusinessException(MessageEnums.LEAVE_INVALID_DURATION);
    }

    // Check for overlapping dates
    LambdaQueryWrapper<Leave> overlapQuery = new LambdaQueryWrapper<>();
    overlapQuery
        .eq(Leave::getUserId, leave.getUserId())
        .ne(Leave::getStatus, LeaveStatus.REJECTED.getCode())
        .le(Leave::getStartDate, endDate)
        .ge(Leave::getEndDate, startDate);
    if (leaveMapper.selectCount(overlapQuery) > 0) {
      throw new BusinessException(MessageEnums.LEAVE_OVERLAPPING_DATE);
    }

    // If annual leave, check balance and no unapproved
    if (LeaveType.ANNUAL.getCode().equals(leave.getLeaveType())) {
      // Check no pending annual leave
      LambdaQueryWrapper<Leave> pendingQuery = new LambdaQueryWrapper<>();
      pendingQuery
          .eq(Leave::getUserId, leave.getUserId())
          .eq(Leave::getLeaveType, LeaveType.ANNUAL.getCode())
          .eq(Leave::getStatus, LeaveStatus.APPLYING.getCode());
      if (leaveMapper.selectCount(pendingQuery) > 0) {
        throw new BusinessException(MessageEnums.LEAVE_ANNUAL_HAS_UNAPPROVED);
      }

      // Check annual leave balance
      BigDecimal remainDays = getAnnualRemainDays(leave.getUserId(), startDate);
      if (leave.getDuration().compareTo(remainDays) > 0) {
        throw new BusinessException(MessageEnums.LEAVE_ANNUAL_EXCEED_BALANCE);
      }
    }

    leaveMapper.insert(leave);
    Long leaveId = leave.getId();

    // Associate files
    if (fileIds != null && !fileIds.isEmpty()) {
      for (Long fileId : fileIds) {
        fileUploadService.associateFile(fileId, FileAssociateType.LEAVE_ATTACHMENT, leaveId);
      }
    }

    return leaveId;
  }

  /**
   * Update leave request status (approve/reject).
   *
   * @param leaveId leave ID
   * @param status new status
   * @param reason approval/rejection reason
   * @param approverId approver user ID
   */
  @OperationLog(
      module = Module.LEAVE,
      subModule = SubModule.LEAVE_APPROVE,
      type = OperationType.UPDATE,
      description = "Leave status updated")
  @Transactional(rollbackFor = Exception.class)
  public void updateLeaveStatus(Long leaveId, LeaveStatus status, String reason, Long approverId) {
    Leave leave = leaveMapper.selectById(leaveId);
    if (leave == null) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_FOUND);
    }

    // Cannot approve own leave
    if (leave.getUserId().equals(approverId)) {
      throw new BusinessException(MessageEnums.LEAVE_CANNOT_APPROVE_SELF);
    }

    // Reason required for rejection
    if (status == LeaveStatus.REJECTED && (reason == null || reason.isBlank())) {
      throw new BusinessException(MessageEnums.LEAVE_REASON_REQUIRED);
    }

    LambdaUpdateWrapper<Leave> updateWrapper = new LambdaUpdateWrapper<>();
    updateWrapper
        .eq(Leave::getId, leaveId)
        .set(Leave::getStatus, status.getCode())
        .set(Leave::getApproverId, approverId)
        .set(Leave::getApproveTime, LocalDateTime.now())
        .set(Leave::getApproveReason, reason);
    leaveMapper.update(null, updateWrapper);
  }

  /**
   * Delete a leave request (cancel/revoke).
   *
   * @param leaveId leave ID
   */
  @OperationLog(
      module = Module.LEAVE,
      subModule = SubModule.LEAVE_APPLY,
      type = OperationType.DELETE,
      description = "Leave request deleted")
  @Transactional(rollbackFor = Exception.class)
  public void deleteLeave(Long leaveId) {
    Leave leave = leaveMapper.selectById(leaveId);
    if (leave == null) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_FOUND);
    }
    leaveMapper.deleteById(leaveId);
  }

  /**
   * Get paginated leave list.
   *
   * @param request page request with filters
   * @return IPage of LeaveWithFiles
   */
  public IPage<Leave> getLeavePage(LeavePageRequest request) {
    LambdaQueryWrapper<Leave> queryWrapper = new LambdaQueryWrapper<>();
    if (request.getUserId() != null) {
      queryWrapper.eq(Leave::getUserId, request.getUserId());
    }
    if (request.getYear() != null) {
      queryWrapper.apply("YEAR(start_date) = {0}", request.getYear());
    }
    if (request.getLeaveType() != null) {
      queryWrapper.eq(Leave::getLeaveType, request.getLeaveType());
    }
    if (request.getStatus() != null) {
      queryWrapper.eq(Leave::getStatus, request.getStatus());
    }
    queryWrapper.orderByDesc(Leave::getCreateAt);
    return leaveMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Get distinct years from annual_leave table.
   *
   * @param userId user ID (optional)
   * @return list of years
   */
  public List<Integer> getYearList(Long userId) {
    LambdaQueryWrapper<AnnualLeave> queryWrapper = new LambdaQueryWrapper<>();
    if (userId != null) {
      queryWrapper.eq(AnnualLeave::getUserId, userId);
    }
    queryWrapper.select(AnnualLeave::getYear).groupBy(AnnualLeave::getYear);
    List<AnnualLeave> records = annualLeaveMapper.selectList(queryWrapper);
    return records.stream()
        .map(AnnualLeave::getYear)
        .distinct()
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * Get leave info (annual leave details) for a user.
   *
   * @param userId user ID
   * @return LeaveInfoResponse
   */
  public LeaveInfoResponse getLeaveInfo(Long userId) {
    int currentYear = Year.now().getValue();
    LocalDate today = LocalDate.now();
    LocalDate hiredDate = getHiredDate(userId);

    AnnualLeave annualLeave = getAnnualLeave(userId, currentYear);

    List<LeaveInfoItem> leaveInfos = calculateLeaveTypeUsage(userId, currentYear);

    String effectiveDate1 = null;
    BigDecimal days1 = BigDecimal.ZERO;
    String effectiveDate2 = null;
    BigDecimal days2 = BigDecimal.ZERO;

    if (annualLeave != null) {
      effectiveDate1 = annualLeave.getEffectiveDate().toString();
      days1 = annualLeave.getPreEffectiveAnnualLeaveDays();
      effectiveDate2 =
          annualLeave.getEffectiveDate().isAfter(today)
              ? annualLeave.getEffectiveDate().toString()
              : null;
      days2 = annualLeave.getPostEffectiveAnnualLeaveDays();
    } else if (hiredDate != null) {
      // Calculate expected values even without a record
      AnnualLeaveCalculation calc = calculateAnnualLeave(hiredDate, currentYear);
      effectiveDate1 = calc.effectiveDate().toString();
      days1 = calc.preEffective();
      effectiveDate2 = calc.effectiveDate().toString();
      days2 = calc.postEffective();
    }

    List<LeaveInfoResponse.LeaveInfoItem> leaveInfoItems =
        leaveInfos.stream()
            .map(
                item ->
                    new LeaveInfoResponse.LeaveInfoItem(
                        item.leaveType().getCode(), item.leaveDays()))
            .collect(Collectors.toList());

    return new LeaveInfoResponse(effectiveDate1, days1, effectiveDate2, days2, leaveInfoItems);
  }

  /**
   * Get remaining annual leave days for a user as of a specific date.
   *
   * @param userId user ID
   * @param date the reference date
   * @return remaining days
   */
  public BigDecimal getAnnualRemainDays(Long userId, LocalDate date) {
    int year = date.getYear();
    AnnualLeave annualLeave = getAnnualLeave(userId, year);
    if (annualLeave == null) {
      return BigDecimal.ZERO;
    }

    LocalDate effectiveDate = annualLeave.getEffectiveDate();
    if (date.isBefore(effectiveDate)) {
      return annualLeave.getPreEffectiveAnnualLeaveDays();
    } else {
      return annualLeave.getPostEffectiveAnnualLeaveDays();
    }
  }

  /**
   * Initialize annual leave for a user and year.
   *
   * @param userId user ID (0 = initialize for all employees)
   * @param year calendar year
   * @param force force re-initialization if already exists
   */
  @OperationLog(
      module = Module.LEAVE,
      subModule = SubModule.ANNUAL_LEAVE,
      type = OperationType.CREATE,
      description = "Annual leave initialized")
  @Transactional(rollbackFor = Exception.class)
  public void initAnnualLeave(Long userId, Integer year, Boolean force) {
    if (userId == 0) {
      // Initialize for all employees
      List<Employee> employees = employeeMapper.selectList(null);
      for (Employee employee : employees) {
        self.initAnnualLeaveForUser(employee.getUserId(), year, force);
      }
    } else {
      self.initAnnualLeaveForUser(userId, year, force);
    }
  }

  /** Initialize annual leave for a single user. */
  @Transactional(rollbackFor = Exception.class)
  public void initAnnualLeaveForUser(Long userId, Integer year, Boolean force) {
    // Check if already exists
    LambdaQueryWrapper<AnnualLeave> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(AnnualLeave::getUserId, userId).eq(AnnualLeave::getYear, year);
    AnnualLeave existing = annualLeaveMapper.selectOne(queryWrapper);

    if (existing != null) {
      if (force != null && force) {
        annualLeaveMapper.deleteById(existing.getId());
      } else {
        throw new BusinessException(MessageEnums.ANNUAL_LEAVE_EXISTS);
      }
    }

    LocalDate hiredDate = getHiredDate(userId);
    if (hiredDate == null) {
      throw new BusinessException(MessageEnums.EMPLOYEE_NOT_FOUND);
    }

    AnnualLeaveCalculation calc = calculateAnnualLeave(hiredDate, year);

    // Calculate previous year remaining
    BigDecimal lastRemaining = BigDecimal.ZERO;
    AnnualLeave prevYearLeave = getAnnualLeave(userId, year - 1);
    if (prevYearLeave != null) {
      lastRemaining = remainingFromPrevYear(calc, prevYearLeave);
    }

    AnnualLeave annualLeave = new AnnualLeave();
    annualLeave.setUserId(userId);
    annualLeave.setYear(year);
    annualLeave.setLastRemainingAnnualLeaveDays(lastRemaining);
    annualLeave.setPreEffectiveAnnualLeaveDays(calc.preEffective());
    annualLeave.setEffectiveDate(calc.effectiveDate());
    annualLeave.setPostEffectiveAnnualLeaveDays(calc.postEffective());

    annualLeaveMapper.insert(annualLeave);
  }

  /** Get leave with associated files. */
  public LeaveWithFiles getLeaveWithFiles(Long leaveId) {
    Leave leave = leaveMapper.selectById(leaveId);
    if (leave == null) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_FOUND);
    }
    List<SysFileUpload> files =
        fileUploadService.findByAssociate(FileAssociateType.LEAVE_ATTACHMENT, leaveId);
    return new LeaveWithFiles(leave, files);
  }

  /**
   * Calculate annual leave entitlement based on hire date and year.
   *
   * <p>Annual leave tiers based on years of service:
   *
   * <ul>
   *   <li>&lt; 1 year: 0 days before, 5 days after anniversary
   *   <li>1-3 years: 5 days
   *   <li>4-6 years: 5 days before, 7 days after anniversary
   *   <li>7+ years: 7 days before, 9 days after anniversary
   * </ul>
   */
  AnnualLeaveCalculation calculateAnnualLeave(LocalDate hiredDate, int year) {
    LocalDate anniversary = hiredDate.withYear(year);
    int yearsOfService = year - hiredDate.getYear();

    BigDecimal preDays;
    BigDecimal postDays;

    if (yearsOfService < 1) {
      preDays = BigDecimal.ZERO;
      postDays = BigDecimal.valueOf(5);
    } else if (yearsOfService <= 3) {
      preDays = BigDecimal.valueOf(5);
      postDays = BigDecimal.valueOf(5);
    } else if (yearsOfService <= 6) {
      preDays = BigDecimal.valueOf(5);
      postDays = BigDecimal.valueOf(7);
    } else {
      preDays = BigDecimal.valueOf(7);
      postDays = BigDecimal.valueOf(9);
    }

    return new AnnualLeaveCalculation(
        null, year, null, preDays, postDays, anniversary, preDays.add(postDays));
  }

  private BigDecimal remainingFromPrevYear(AnnualLeaveCalculation calc, AnnualLeave prevYear) {
    if (prevYear == null) return BigDecimal.ZERO;
    // Remaining = post-effective days - used days
    return prevYear.getPostEffectiveAnnualLeaveDays();
  }

  private AnnualLeave getAnnualLeave(Long userId, int year) {
    LambdaQueryWrapper<AnnualLeave> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(AnnualLeave::getUserId, userId).eq(AnnualLeave::getYear, year);
    return annualLeaveMapper.selectOne(queryWrapper);
  }

  private LocalDate getHiredDate(Long userId) {
    LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.eq(Employee::getUserId, userId);
    Employee employee = employeeMapper.selectOne(queryWrapper);
    return employee != null ? employee.getHiredDate() : null;
  }

  private List<LeaveInfoItem> calculateLeaveTypeUsage(Long userId, int year) {
    LambdaQueryWrapper<Leave> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper
        .eq(Leave::getUserId, userId)
        .apply("YEAR(start_date) = {0}", year)
        .ne(Leave::getStatus, LeaveStatus.REJECTED.getCode());

    List<Leave> leaves = leaveMapper.selectList(queryWrapper);

    return leaves.stream()
        .collect(
            Collectors.groupingBy(
                Leave::getLeaveType,
                Collectors.reducing(BigDecimal.ZERO, Leave::getDuration, BigDecimal::add)))
        .entrySet()
        .stream()
        .map(
            e -> {
              LeaveType type = LeaveType.values()[e.getKey()];
              return new LeaveInfoItem(type, e.getValue().setScale(1, RoundingMode.HALF_UP));
            })
        .sorted(Comparator.comparing(LeaveInfoItem::leaveType))
        .collect(Collectors.toList());
  }

  /** Get approver nickname by user ID. */
  public String getApproverNickname(Long approverId) {
    if (approverId == null) return null;
    MstUser user = userMapper.selectById(approverId);
    if (user == null) return null;
    LambdaQueryWrapper<Employee> empQuery = new LambdaQueryWrapper<>();
    empQuery.eq(Employee::getUserId, approverId);
    Employee employee = employeeMapper.selectOne(empQuery);
    return employee != null ? employee.getNickname() : user.getUsername();
  }
}
