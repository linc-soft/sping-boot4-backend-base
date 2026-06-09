package com.lincsoft.services.oa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.LeaveTypeEnum;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.Module;
import com.lincsoft.constant.OperationType;
import com.lincsoft.constant.SubModule;
import com.lincsoft.controller.oa.vo.AnnualBalanceResponse;
import com.lincsoft.controller.oa.vo.LeavePageRequest;
import com.lincsoft.controller.oa.vo.LeaveTaskResponseItem;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.entity.oa.MstDepartment;
import com.lincsoft.entity.oa.MstEmployee;
import com.lincsoft.entity.oa.OaLeaveRequest;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.oa.OaLeaveRequestMapper;
import com.lincsoft.services.master.UserService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Leave request service.
 *
 * <p>Owns the business data of leave requests via {@link OaLeaveRequestMapper}, and delegates the
 * approval flow to the Flowable BPMN engine. The applicant employee and the approver (direct
 * manager) are resolved from the organization tables; their login usernames are passed to Flowable
 * as process variables / task assignees.
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Service
@RequiredArgsConstructor
public class LeaveRequestService {
  /** BPMN process definition key, matching processes/leave-approval.bpmn20.xml. */
  private static final String PROCESS_KEY = "leaveApproval";

  /** Leave status: pending approval. */
  private static final String STATUS_PENDING = "0";

  /** Leave status: approved. */
  private static final String STATUS_APPROVED = "1";

  /** Leave status: rejected. */
  private static final String STATUS_REJECTED = "2";

  /** Leave status: withdrawn. */
  private static final String STATUS_WITHDRAWN = "3";

  /** Days threshold at or above which department-leader (second-level) approval is required. */
  private static final BigDecimal MULTI_LEVEL_THRESHOLD = new BigDecimal("3");

  /** Smallest allowed leave-day unit; requested days must be a positive multiple of this. */
  private static final BigDecimal HALF_DAY_UNIT = new BigDecimal("0.5");

  /** Leave request mapper for database operations. */
  private final OaLeaveRequestMapper leaveRequestMapper;

  /** Employee service for resolving applicant and approver. */
  private final EmployeeService employeeService;

  /** Annual leave service for quota check / consumption / refund. */
  private final AnnualLeaveService annualLeaveService;

  /** Department service for resolving the department leader (second-level approver). */
  private final DepartmentService departmentService;

  /** User service for resolving the current login account. */
  private final UserService userService;

  /** Flowable runtime service for starting and managing process instances. */
  private final RuntimeService runtimeService;

  /** Flowable task service for querying and completing user tasks. */
  private final TaskService taskService;

  /**
   * Get leave request by ID.
   *
   * @param id Leave request ID
   * @return OaLeaveRequest entity
   * @throws BusinessException if the leave request is not found
   */
  public OaLeaveRequest getLeaveById(Long id) {
    OaLeaveRequest leave = leaveRequestMapper.selectById(id);
    if (leave == null) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_FOUND);
    }
    return leave;
  }

  /**
   * Get the current authenticated user's annual-leave balance.
   *
   * @return the current user's annual-leave balance with per-batch detail
   */
  public AnnualBalanceResponse getMyAnnualBalance() {
    return annualLeaveService.getBalance(resolveCurrentEmployee());
  }

  /**
   * Get an employee's annual-leave balance by employee ID (privileged view).
   *
   * @param employeeId Employee ID
   * @return the employee's annual-leave balance with per-batch detail
   * @throws BusinessException if the employee is not found
   */
  public AnnualBalanceResponse getAnnualBalanceByEmployeeId(Long employeeId) {
    return annualLeaveService.getBalance(employeeService.getEmployeeById(employeeId));
  }

  /**
   * Get leave request page by query conditions.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of leave requests
   */
  public IPage<OaLeaveRequest> getLeavePage(LeavePageRequest request) {
    QueryWrapper<OaLeaveRequest> queryWrapper = new QueryWrapper<>();
    if (request.getEmployeeId() != null) {
      queryWrapper.eq("employee_id", request.getEmployeeId());
    }
    if (request.getLeaveType() != null && !request.getLeaveType().isBlank()) {
      queryWrapper.eq("leave_type", request.getLeaveType());
    }
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      queryWrapper.eq("status", request.getStatus());
    }
    request.applySorting(
        queryWrapper,
        Set.of("id", "employee_id", "leave_type", "status", "create_at", "update_at"),
        "create_at");
    return leaveRequestMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Submit a new leave request and start the approval process.
   *
   * <p>Resolves the applicant from the authenticated user, resolves the direct manager from {@link
   * MstEmployee#getManagerId()}, persists the business record, then starts the Flowable process
   * with the manager's username as the task assignee. The whole operation is transactional.
   *
   * @param leave Leave request entity (business fields populated by the caller)
   * @return The created leave request ID
   * @throws BusinessException if the applicant has no employee profile or no direct manager
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.LEAVE,
      type = OperationType.CREATE,
      description = "Leave request submitted: #{#result}")
  @Transactional(rollbackFor = Exception.class)
  public Long submitLeave(OaLeaveRequest leave) {
    validateLeaveType(leave.getLeaveType());
    validateHalfDayUnit(leave.getDays());

    MstEmployee applicant = resolveCurrentEmployee();
    if (applicant.getManagerId() == null) {
      throw new BusinessException(MessageEnums.LEAVE_NO_MANAGER);
    }
    MstEmployee manager = employeeService.getEmployeeById(applicant.getManagerId());
    String managerUsername = resolveUsername(manager);

    boolean multiLevel = isMultiLevel(leave.getDays());
    String deptLeaderUsername = multiLevel ? resolveDeptLeaderUsername(applicant) : null;

    leave.setEmployeeId(applicant.getId());
    leave.setApproverId(manager.getId());
    leave.setStatus(STATUS_PENDING);
    leaveRequestMapper.insert(leave);

    // Annual leave consumes quota at submission time (FIFO). Insufficient balance rolls back the
    // whole transaction, so no leave record or process is left behind.
    if (LeaveTypeEnum.ANNUAL.getCode().equals(leave.getLeaveType())
        && !annualLeaveService.consume(applicant, leave.getDays(), leave.getId())) {
      throw new BusinessException(MessageEnums.LEAVE_INSUFFICIENT_ANNUAL);
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("leaveId", leave.getId());
    variables.put("applicantName", applicant.getRealName());
    variables.put("leaveDays", leave.getDays());
    variables.put("managerId", managerUsername);
    if (deptLeaderUsername != null) {
      variables.put("deptLeaderId", deptLeaderUsername);
    }
    ProcessInstance instance =
        runtimeService.startProcessInstanceByKey(PROCESS_KEY, leave.getId().toString(), variables);

    LambdaUpdateWrapper<OaLeaveRequest> uw = new LambdaUpdateWrapper<>();
    uw.eq(OaLeaveRequest::getId, leave.getId())
        .set(OaLeaveRequest::getProcessInstanceId, instance.getId());
    leaveRequestMapper.update(null, uw);

    return leave.getId();
  }

  /**
   * Approve or reject a leave request.
   *
   * <p>The current user must be the assignee of the pending task. Completes the Flowable task with
   * the {@code approved} variable (driving the gateway), and synchronizes the business status.
   *
   * @param id Leave request ID
   * @param approved Approval decision
   * @param comment Approver comment
   * @throws BusinessException if the leave is not pending or the task is not found
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.LEAVE,
      type = OperationType.UPDATE,
      description = "Leave request reviewed: #{#id}")
  @Transactional(rollbackFor = Exception.class)
  public void reviewLeave(Long id, boolean approved, String comment) {
    OaLeaveRequest leave = getLeaveById(id);
    if (!STATUS_PENDING.equals(leave.getStatus())) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_PENDING);
    }

    Task task =
        taskService
            .createTaskQuery()
            .processInstanceId(leave.getProcessInstanceId())
            .singleResult();
    if (task == null) {
      throw new BusinessException(MessageEnums.LEAVE_TASK_NOT_FOUND);
    }

    Map<String, Object> variables = new HashMap<>();
    variables.put("approved", approved);
    taskService.complete(task.getId(), variables);

    // After completing the task, approving may advance the process to the next approval level
    // (department leader). The business record stays PENDING until the process actually ends.
    boolean processEnded = isProcessEnded(leave.getProcessInstanceId());
    LambdaUpdateWrapper<OaLeaveRequest> uw = new LambdaUpdateWrapper<>();
    uw.eq(OaLeaveRequest::getId, id);
    if (!approved) {
      refundAnnualLeaveIfNeeded(leave);
      uw.set(OaLeaveRequest::getStatus, STATUS_REJECTED)
          .set(OaLeaveRequest::getApprovalComment, comment);
    } else if (processEnded) {
      uw.set(OaLeaveRequest::getStatus, STATUS_APPROVED)
          .set(OaLeaveRequest::getApprovalComment, comment);
    } else {
      uw.set(OaLeaveRequest::getApprovalComment, comment);
    }
    leaveRequestMapper.update(null, uw);
  }

  /**
   * Check whether a process instance has ended (no active runtime instance remains).
   *
   * @param processInstanceId Flowable process instance ID
   * @return true if the process instance is no longer active
   */
  private boolean isProcessEnded(String processInstanceId) {
    return runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count()
        == 0;
  }

  /**
   * Withdraw a pending leave request.
   *
   * <p>Only the applicant may withdraw their own request, and only while it is still pending.
   * Deletes the Flowable process instance and marks the business record as withdrawn.
   *
   * @param id Leave request ID
   * @throws BusinessException if the caller is not the owner or the leave is not pending
   */
  @OperationLog(
      module = Module.OA,
      subModule = SubModule.LEAVE,
      type = OperationType.UPDATE,
      description = "Leave request withdrawn: #{#id}")
  @Transactional(rollbackFor = Exception.class)
  public void withdrawLeave(Long id) {
    OaLeaveRequest leave = getLeaveById(id);
    MstEmployee applicant = resolveCurrentEmployee();
    if (!applicant.getId().equals(leave.getEmployeeId())) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_OWNER);
    }
    if (!STATUS_PENDING.equals(leave.getStatus())) {
      throw new BusinessException(MessageEnums.LEAVE_NOT_PENDING);
    }

    if (leave.getProcessInstanceId() != null) {
      runtimeService.deleteProcessInstance(leave.getProcessInstanceId(), "Withdrawn by applicant");
    }

    refundAnnualLeaveIfNeeded(leave);

    LambdaUpdateWrapper<OaLeaveRequest> uw = new LambdaUpdateWrapper<>();
    uw.eq(OaLeaveRequest::getId, id).set(OaLeaveRequest::getStatus, STATUS_WITHDRAWN);
    leaveRequestMapper.update(null, uw);
  }

  /**
   * List the pending approval tasks assigned to the current user.
   *
   * <p>Queries Flowable for tasks assigned to the current user's username, then loads the matching
   * leave request business data for each.
   *
   * @return List of pending leave approval tasks
   */
  public List<LeaveTaskResponseItem> getMyPendingTasks() {
    MstEmployee approver = resolveCurrentEmployee();
    String username = resolveUsername(approver);

    List<Task> tasks = taskService.createTaskQuery().taskAssignee(username).list();
    return tasks.stream().map(this::toTaskItem).toList();
  }

  /**
   * Build a task item by loading the leave business data behind a Flowable task.
   *
   * @param task Flowable task
   * @return Combined task + leave business view
   */
  private LeaveTaskResponseItem toTaskItem(Task task) {
    Object leaveIdVar = runtimeService.getVariable(task.getProcessInstanceId(), "leaveId");
    Long leaveId = Long.valueOf(String.valueOf(leaveIdVar));
    OaLeaveRequest leave = leaveRequestMapper.selectById(leaveId);
    return new LeaveTaskResponseItem(
        leave.getId(),
        leave.getEmployeeId(),
        leave.getLeaveType(),
        leave.getStartDate(),
        leave.getStartPeriod(),
        leave.getEndDate(),
        leave.getEndPeriod(),
        leave.getDays(),
        leave.getReason(),
        leave.getCreateAt());
  }

  /**
   * Resolve the employee profile of the currently authenticated user.
   *
   * @return the current user's employee
   * @throws BusinessException if the user has no linked employee profile
   */
  private MstEmployee resolveCurrentEmployee() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    MstUser user = userService.findByUsername(username);
    if (user == null) {
      throw new BusinessException(MessageEnums.USER_NOT_FOUND);
    }
    MstEmployee employee = employeeService.findByUserId(user.getId());
    if (employee == null) {
      throw new BusinessException(MessageEnums.EMPLOYEE_NOT_FOUND);
    }
    return employee;
  }

  /**
   * Resolve the login username of an employee.
   *
   * @param employee Employee whose login username is needed
   * @return the linked login username
   * @throws BusinessException if the employee has no linked login account
   */
  private String resolveUsername(MstEmployee employee) {
    if (employee.getUserId() == null) {
      throw new BusinessException(MessageEnums.LEAVE_NO_MANAGER);
    }
    MstUser user = userService.getUserById(employee.getUserId());
    return user.getUsername();
  }

  /**
   * Determine whether the leave duration requires multi-level (department-leader) approval.
   *
   * @param days Number of leave days
   * @return true if days is at or above the multi-level threshold
   */
  private boolean isMultiLevel(BigDecimal days) {
    return days != null && days.compareTo(MULTI_LEVEL_THRESHOLD) >= 0;
  }

  /**
   * Resolve the login username of the applicant's department leader (second-level approver).
   *
   * @param applicant The applicant employee
   * @return the department leader's login username
   * @throws BusinessException if the applicant has no department or the department has no leader
   */
  private String resolveDeptLeaderUsername(MstEmployee applicant) {
    if (applicant.getDeptId() == null) {
      throw new BusinessException(MessageEnums.LEAVE_NO_DEPT_LEADER);
    }
    MstDepartment department = departmentService.getDepartmentById(applicant.getDeptId());
    if (department.getLeaderEmployeeId() == null) {
      throw new BusinessException(MessageEnums.LEAVE_NO_DEPT_LEADER);
    }
    MstEmployee leader = employeeService.getEmployeeById(department.getLeaderEmployeeId());
    return resolveUsername(leader);
  }

  /**
   * Validate that the leave type is one of the defined types.
   *
   * @param leaveType Leave type code
   * @throws BusinessException if the type is not defined
   */
  private void validateLeaveType(String leaveType) {
    if (!LeaveTypeEnum.isValid(leaveType)) {
      throw new BusinessException(MessageEnums.LEAVE_INVALID_TYPE);
    }
  }

  /**
   * Validate that the requested days is a positive multiple of the half-day unit.
   *
   * @param days Number of leave days
   * @throws BusinessException if days is null, non-positive, or not a multiple of 0.5
   */
  private void validateHalfDayUnit(BigDecimal days) {
    if (days == null
        || days.signum() <= 0
        || days.remainder(HALF_DAY_UNIT).compareTo(BigDecimal.ZERO) != 0) {
      throw new BusinessException(MessageEnums.LEAVE_DAYS_NOT_HALF_UNIT);
    }
  }

  /**
   * Refund consumed annual leave for the given request, if it is an annual-leave request.
   *
   * @param leave The leave request being rejected or withdrawn
   */
  private void refundAnnualLeaveIfNeeded(OaLeaveRequest leave) {
    if (LeaveTypeEnum.ANNUAL.getCode().equals(leave.getLeaveType())) {
      annualLeaveService.refund(leave.getId());
    }
  }
}
