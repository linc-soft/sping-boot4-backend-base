package com.lincsoft.controller.leave;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.constant.*;
import com.lincsoft.controller.leave.vo.*;
import com.lincsoft.entity.leave.Leave;
import com.lincsoft.entity.system.SysFileUpload;
import com.lincsoft.mapstruct.LeaveMapper;
import com.lincsoft.services.leave.LeaveService;
import com.lincsoft.services.system.FileUploadService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Leave management controller.
 *
 * @author lincsoft
 * @since 2026-06-03
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

  private final LeaveService leaveService;
  private final LeaveMapper leaveMapper;
  private final FileUploadService fileUploadService;

  /**
   * Get paginated leave list.
   *
   * @param request page request with filters
   * @return IPage of leave responses
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode)")
  @GetMapping("/page")
  public IPage<LeavePageResponse> getLeavePage(LeavePageRequest request) {
    IPage<Leave> leavePage = leaveService.getLeavePage(request);
    return leavePage.convert(
        leave -> {
          String nickname = leaveService.getApproverNickname(leave.getUserId());
          String approverNickname =
              leave.getApproverId() != null
                  ? leaveService.getApproverNickname(leave.getApproverId())
                  : null;
          List<SysFileUpload> files =
              fileUploadService.findByAssociate(FileAssociateType.LEAVE_ATTACHMENT, leave.getId());

          return new LeavePageResponse(
              leave.getId(),
              leave.getUserId(),
              nickname,
              leave.getStartDate(),
              leave.getEndDate(),
              leave.getLeaveType(),
              leave.getDuration(),
              leave.getReason(),
              leave.getStatus(),
              approverNickname,
              leave.getApproveTime(),
              leave.getApproveReason(),
              leaveMapper.toFileMetadataResponseList(files));
        });
  }

  /**
   * Get available year list for leave filtering.
   *
   * @return list of years
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode)")
  @GetMapping("/year-list")
  public List<Integer> getYearList() {
    return leaveService.getYearList(null);
  }

  /**
   * Get annual leave info for the current user.
   *
   * @return leave info response
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode)")
  @GetMapping("/info")
  public LeaveInfoResponse getLeaveInfo() {
    Long userId = getCurrentUserId();
    return leaveService.getLeaveInfo(userId);
  }

  /**
   * Get remaining annual leave days as of a specific date.
   *
   * @param date the reference date (yyyy-MM-dd)
   * @return remaining days
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode)")
  @GetMapping("/annual-remain")
  public AnnualRemainResponse getAnnualRemain(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    Long userId = getCurrentUserId();
    BigDecimal remainDays = leaveService.getAnnualRemainDays(userId, date);
    return new AnnualRemainResponse(remainDays);
  }

  /**
   * Create a new leave request.
   *
   * @param request leave request data
   * @return created leave ID
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_WRITE.roleCode)")
  @PostMapping
  public Long createLeave(@Valid @RequestBody SaveLeaveRequest request) {
    Leave leave = new Leave();
    leave.setUserId(request.userId());
    leave.setStartDate(LocalDate.parse(request.startDate()));
    leave.setEndDate(LocalDate.parse(request.endDate()));
    leave.setLeaveType(request.leaveType().getCode());
    leave.setDuration(request.duration());
    leave.setReason(request.reason());
    leave.setStatus(LeaveStatus.APPLYING.getCode());
    return leaveService.createLeave(leave, request.fileIds(), getCurrentUserId());
  }

  /**
   * Approve or reject a leave request.
   *
   * @param request status update request
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPROVE.roleCode)")
  @PutMapping("/status")
  public void updateLeaveStatus(@Valid @RequestBody UpdateLeaveStatusRequest request) {
    leaveService.updateLeaveStatus(
        request.id(), request.status(), request.reason(), getCurrentUserId());
  }

  /**
   * Delete (cancel/revoke) a leave request.
   *
   * @param id leave ID
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_DELETE.roleCode)")
  @DeleteMapping
  public void deleteLeave(@RequestParam Long id) {
    leaveService.deleteLeave(id);
  }

  /**
   * Initialize annual leave for a user or all users.
   *
   * @param request initialization request
   */
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPROVE.roleCode)")
  @PostMapping("/init-annual-leave")
  public void initAnnualLeave(@Valid @RequestBody InitAnnualLeaveRequest request) {
    leaveService.initAnnualLeave(request.userId(), request.year(), request.force());
  }

  private Long getCurrentUserId() {
    // Get from security context - will be injected via Spring Security
    return 1L; // TODO: inject from SecurityContext
  }
}
