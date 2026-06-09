package com.lincsoft.controller.oa;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.controller.oa.vo.AnnualBalanceResponse;
import com.lincsoft.controller.oa.vo.LeaveApprovalRequest;
import com.lincsoft.controller.oa.vo.LeaveInfoResponse;
import com.lincsoft.controller.oa.vo.LeavePageRequest;
import com.lincsoft.controller.oa.vo.LeavePageResponseItem;
import com.lincsoft.controller.oa.vo.LeaveSubmitRequest;
import com.lincsoft.controller.oa.vo.LeaveTaskResponseItem;
import com.lincsoft.controller.oa.vo.LeaveWithdrawRequest;
import com.lincsoft.mapstruct.LeaveRequestMapper;
import com.lincsoft.services.oa.LeaveRequestService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Leave request controller.
 *
 * <p>Provides endpoints for submitting, reviewing, withdrawing, and querying leave requests. The
 * approval flow is driven by the Flowable BPMN engine.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveRequestController {

  /** Leave request service. */
  private final LeaveRequestService leaveRequestService;

  /** Leave request mapper for converting between VO and entity. */
  private final LeaveRequestMapper leaveRequestMapper;

  /**
   * Get the current user's annual-leave balance.
   *
   * @return the current user's annual-leave balance with per-batch detail
   */
  @GetMapping("/annual-balance")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPLY.roleCode)")
  public AnnualBalanceResponse getMyAnnualBalance() {
    return leaveRequestService.getMyAnnualBalance();
  }

  /**
   * Get an employee's annual-leave balance by employee ID (privileged view).
   *
   * @param employeeId Employee ID
   * @return the employee's annual-leave balance with per-batch detail
   */
  @GetMapping("/annual-balance/{employeeId}")
  @PreAuthorize(
      "hasAnyRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode, T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPLY.roleCode)")
  public AnnualBalanceResponse getAnnualBalanceByEmployeeId(@PathVariable Long employeeId) {
    return leaveRequestService.getAnnualBalanceByEmployeeId(employeeId);
  }

  /**
   * Get leave request by ID.
   *
   * @param id Leave request ID
   * @return Leave info response
   */
  @GetMapping("/{id}")
  @PreAuthorize(
      "hasAnyRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode, T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPLY.roleCode)")
  public LeaveInfoResponse getLeave(@PathVariable Long id) {
    return leaveRequestMapper.toInfoResponse(leaveRequestService.getLeaveById(id));
  }

  /**
   * Get leave request page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of leave request items
   */
  @GetMapping("/page")
  @PreAuthorize(
      "hasAnyRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_READ.roleCode, T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPLY.roleCode)")
  public IPage<LeavePageResponseItem> getLeavePage(LeavePageRequest request) {
    return leaveRequestService
        .getLeavePage(request)
        .convert(leaveRequestMapper::toPageResponseItem);
  }

  /**
   * List the pending approval tasks assigned to the current user.
   *
   * @return List of pending leave approval tasks
   */
  @GetMapping("/tasks")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPROVE.roleCode)")
  public List<LeaveTaskResponseItem> getMyPendingTasks() {
    return leaveRequestService.getMyPendingTasks();
  }

  /**
   * Submit a new leave request and start the approval process.
   *
   * @param request Leave submit request
   * @return created leave request ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPLY.roleCode)")
  public Long submitLeave(@Valid @RequestBody LeaveSubmitRequest request) {
    return leaveRequestService.submitLeave(leaveRequestMapper.toEntity(request));
  }

  /**
   * Approve or reject a leave request.
   *
   * @param request Leave approval request
   */
  @PostMapping("/review")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPROVE.roleCode)")
  public void reviewLeave(@Valid @RequestBody LeaveApprovalRequest request) {
    leaveRequestService.reviewLeave(request.id(), request.approved(), request.comment());
  }

  /**
   * Withdraw a pending leave request.
   *
   * @param request Leave withdraw request
   */
  @PostMapping("/withdraw")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).LEAVE_APPLY.roleCode)")
  public void withdrawLeave(@Valid @RequestBody LeaveWithdrawRequest request) {
    leaveRequestService.withdrawLeave(request.id());
  }
}
