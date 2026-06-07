package com.lincsoft.services.oa;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.controller.oa.vo.AnnualBalanceBatch;
import com.lincsoft.controller.oa.vo.AnnualBalanceResponse;
import com.lincsoft.entity.oa.MstEmployee;
import com.lincsoft.entity.oa.OaAnnualLeaveConsumption;
import com.lincsoft.entity.oa.OaAnnualLeaveGrant;
import com.lincsoft.mapper.oa.OaAnnualLeaveConsumptionMapper;
import com.lincsoft.mapper.oa.OaAnnualLeaveGrantMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Annual leave quota service.
 *
 * <p>Implements the annual-leave entitlement rules:
 *
 * <ul>
 *   <li><b>Tenure tier</b> (locked at each grant date): tenure 1–3 years → 5 days, 4–6 years → 7
 *       days, 7+ years → 9 days.
 *   <li><b>Grant cadence</b>: one batch is granted on each employment anniversary, starting from
 *       the first anniversary (hireDate + 1 year).
 *   <li><b>Validity</b>: each batch is valid for 24 months from its grant date.
 *   <li><b>Consumption</b>: FIFO — the earliest-granted (soonest-to-expire) batch is consumed
 *       first.
 *   <li><b>Lazy grant</b>: batches are written on demand (on query / submit) rather than via a
 *       scheduled job; only non-expired due batches are materialized.
 * </ul>
 *
 * @author 林创科技
 * @since 2026-06-07
 */
@Service
@RequiredArgsConstructor
public class AnnualLeaveService {
  /** Validity window of a grant batch, in months. */
  private static final int VALIDITY_MONTHS = 24;

  /** Tenure (years) upper bound for the first tier. */
  private static final int TIER1_MAX_YEARS = 3;

  /** Tenure (years) upper bound for the second tier. */
  private static final int TIER2_MAX_YEARS = 6;

  /** Days granted for tenure 1–3 years. */
  private static final BigDecimal TIER1_DAYS = new BigDecimal("5");

  /** Days granted for tenure 4–6 years. */
  private static final BigDecimal TIER2_DAYS = new BigDecimal("7");

  /** Days granted for tenure 7+ years. */
  private static final BigDecimal TIER3_DAYS = new BigDecimal("9");

  /** Annual leave grant mapper for database operations. */
  private final OaAnnualLeaveGrantMapper grantMapper;

  /** Annual leave consumption ledger mapper for FIFO refund tracking. */
  private final OaAnnualLeaveConsumptionMapper consumptionMapper;

  /**
   * Compute the available annual-leave days for an employee as of today.
   *
   * <p>Ensures all due (non-expired) grant batches are materialized, then sums the remaining days
   * of the non-expired batches.
   *
   * @param employee The employee whose available annual leave is computed
   * @return total available annual-leave days (0 if hireDate is null)
   */
  @Transactional(rollbackFor = Exception.class)
  public BigDecimal getAvailableDays(MstEmployee employee) {
    if (employee.getHireDate() == null) {
      return BigDecimal.ZERO;
    }
    ensureGrants(employee, LocalDate.now());
    return loadActiveGrants(employee.getId(), LocalDate.now()).stream()
        .map(g -> g.getGrantedDays().subtract(g.getUsedDays()))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /**
   * Compute the annual-leave balance for an employee as of today, with per-batch detail.
   *
   * <p>Materializes due grant batches, then returns the total available days plus each active
   * (non-expired) batch's grant date, expiry date, granted / used / remaining days. Employees with
   * a null hire date have a zero total and no batches.
   *
   * @param employee The employee whose balance is computed
   * @return the aggregated annual-leave balance
   */
  @Transactional(rollbackFor = Exception.class)
  public AnnualBalanceResponse getBalance(MstEmployee employee) {
    if (employee.getHireDate() == null) {
      return new AnnualBalanceResponse(employee.getId(), BigDecimal.ZERO, List.of());
    }
    ensureGrants(employee, LocalDate.now());
    List<OaAnnualLeaveGrant> grants = loadActiveGrants(employee.getId(), LocalDate.now());

    List<AnnualBalanceBatch> batches =
        grants.stream()
            .map(
                g ->
                    new AnnualBalanceBatch(
                        g.getGrantDate(),
                        g.getExpireDate(),
                        g.getGrantedDays(),
                        g.getUsedDays(),
                        g.getGrantedDays().subtract(g.getUsedDays())))
            .toList();

    BigDecimal total =
        batches.stream()
            .map(AnnualBalanceBatch::remainingDays)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new AnnualBalanceResponse(employee.getId(), total, batches);
  }

  /**
   * Consume annual-leave days FIFO across an employee's active grant batches.
   *
   * <p>Ensures grants are materialized, validates sufficient balance, then deducts {@code days}
   * starting from the earliest-granted batch. For each affected batch, writes a consumption ledger
   * row keyed by {@code leaveRequestId} so the exact batches can be refunded later. Caller is
   * responsible for invoking this within the submission transaction.
   *
   * @param employee The employee consuming annual leave
   * @param days Days to consume (must be {@code > 0})
   * @param leaveRequestId The leave request driving this consumption (for refund tracking)
   * @return true if the deduction succeeded; false if the balance is insufficient
   */
  @Transactional(rollbackFor = Exception.class)
  public boolean consume(MstEmployee employee, BigDecimal days, Long leaveRequestId) {
    if (employee.getHireDate() == null || days == null || days.signum() <= 0) {
      return false;
    }
    ensureGrants(employee, LocalDate.now());
    List<OaAnnualLeaveGrant> grants = loadActiveGrants(employee.getId(), LocalDate.now());

    BigDecimal total =
        grants.stream()
            .map(g -> g.getGrantedDays().subtract(g.getUsedDays()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (total.compareTo(days) < 0) {
      return false;
    }

    BigDecimal remaining = days;
    for (OaAnnualLeaveGrant grant : grants) {
      if (remaining.signum() <= 0) {
        break;
      }
      BigDecimal batchRemaining = grant.getGrantedDays().subtract(grant.getUsedDays());
      if (batchRemaining.signum() <= 0) {
        continue;
      }
      BigDecimal take = batchRemaining.min(remaining);
      grant.setUsedDays(grant.getUsedDays().add(take));
      grantMapper.updateById(grant);

      OaAnnualLeaveConsumption ledger = new OaAnnualLeaveConsumption();
      ledger.setLeaveRequestId(leaveRequestId);
      ledger.setGrantId(grant.getId());
      ledger.setDays(take);
      consumptionMapper.insert(ledger);

      remaining = remaining.subtract(take);
    }
    return true;
  }

  /**
   * Refund the annual leave consumed by a leave request, restoring the exact original batches.
   *
   * <p>Reads the consumption ledger for {@code leaveRequestId}, adds each recorded {@code days}
   * back to its original grant batch, then deletes the ledger rows. If a batch has since been
   * hard-deleted it is skipped (the days are simply not restored). Caller invokes this within the
   * reject/withdraw transaction.
   *
   * @param leaveRequestId The leave request whose consumption should be refunded
   */
  @Transactional(rollbackFor = Exception.class)
  public void refund(Long leaveRequestId) {
    QueryWrapper<OaAnnualLeaveConsumption> ledgerQuery = new QueryWrapper<>();
    ledgerQuery.eq("leave_request_id", leaveRequestId);
    List<OaAnnualLeaveConsumption> ledgers = consumptionMapper.selectList(ledgerQuery);

    for (OaAnnualLeaveConsumption ledger : ledgers) {
      OaAnnualLeaveGrant grant = grantMapper.selectById(ledger.getGrantId());
      if (grant != null) {
        grant.setUsedDays(grant.getUsedDays().subtract(ledger.getDays()));
        grantMapper.updateById(grant);
      }
      consumptionMapper.deleteById(ledger.getId());
    }
  }

  /**
   * Materialize all due, non-expired grant batches for an employee up to the reference date.
   *
   * <p>Walks each employment anniversary from {@code hireDate + 1 year} up to {@code asOf}. For
   * each anniversary whose 24-month validity window has not closed, inserts a grant batch if one
   * does not already exist. Expired anniversaries are skipped (never materialized).
   *
   * @param employee The employee to grant for
   * @param asOf Reference date (typically today)
   */
  private void ensureGrants(MstEmployee employee, LocalDate asOf) {
    LocalDate hireDate = employee.getHireDate();
    LocalDate anniversary = hireDate.plusYears(1);
    while (!anniversary.isAfter(asOf)) {
      LocalDate expireDate = anniversary.plusMonths(VALIDITY_MONTHS);
      boolean expired = !expireDate.isAfter(asOf);
      if (!expired && !grantExists(employee.getId(), anniversary)) {
        OaAnnualLeaveGrant grant = new OaAnnualLeaveGrant();
        grant.setEmployeeId(employee.getId());
        grant.setGrantDate(anniversary);
        grant.setExpireDate(expireDate);
        grant.setGrantedDays(tierDaysFor(hireDate, anniversary));
        grant.setUsedDays(BigDecimal.ZERO);
        grantMapper.insert(grant);
      }
      anniversary = anniversary.plusYears(1);
    }
  }

  /**
   * Resolve the granted days for a batch based on tenure at the grant date.
   *
   * @param hireDate Employee hire date
   * @param grantDate The anniversary grant date
   * @return granted days for the tenure tier (5 / 7 / 9)
   */
  private BigDecimal tierDaysFor(LocalDate hireDate, LocalDate grantDate) {
    int tenureYears = (int) java.time.temporal.ChronoUnit.YEARS.between(hireDate, grantDate);
    if (tenureYears <= TIER1_MAX_YEARS) {
      return TIER1_DAYS;
    }
    if (tenureYears <= TIER2_MAX_YEARS) {
      return TIER2_DAYS;
    }
    return TIER3_DAYS;
  }

  /**
   * Check whether a grant batch already exists for the given employee and grant date.
   *
   * @param employeeId Employee ID
   * @param grantDate Grant date
   * @return true if a batch already exists
   */
  private boolean grantExists(Long employeeId, LocalDate grantDate) {
    QueryWrapper<OaAnnualLeaveGrant> qw = new QueryWrapper<>();
    qw.eq("employee_id", employeeId).eq("grant_date", grantDate);
    return grantMapper.selectCount(qw) > 0;
  }

  /**
   * Load an employee's active (non-expired) grant batches ordered FIFO by grant date.
   *
   * @param employeeId Employee ID
   * @param asOf Reference date
   * @return active grant batches, earliest grant date first
   */
  private List<OaAnnualLeaveGrant> loadActiveGrants(Long employeeId, LocalDate asOf) {
    QueryWrapper<OaAnnualLeaveGrant> qw = new QueryWrapper<>();
    qw.eq("employee_id", employeeId).gt("expire_date", asOf).orderByAsc("grant_date");
    return grantMapper.selectList(qw);
  }
}
