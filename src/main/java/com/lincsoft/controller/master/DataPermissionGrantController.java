package com.lincsoft.controller.master;

import com.lincsoft.entity.master.MstDataPermissionGrant;
import com.lincsoft.services.master.DataPermissionGrantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Data permission grant controller.
 *
 * <p>Provides endpoints for managing row-level permission grants.
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@RestController
@RequestMapping("/api/data-permission-grants")
@RequiredArgsConstructor
public class DataPermissionGrantController {

  private final DataPermissionGrantService dataPermissionGrantService;

  /**
   * Get all grants for a specific resource.
   *
   * @param resourceType resource type enum name
   * @param resourceId resource instance ID
   * @return list of grants
   */
  @GetMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public List<MstDataPermissionGrant> getGrantList(
      @RequestParam @NotBlank String resourceType, @RequestParam @NotNull Long resourceId) {
    return dataPermissionGrantService.getGrantList(resourceType, resourceId);
  }

  /**
   * Create a new row-level permission grant.
   *
   * @param grant grant entity
   * @return created grant ID
   */
  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public Long createGrant(@Valid @RequestBody MstDataPermissionGrant grant) {
    return dataPermissionGrantService.createGrant(grant);
  }

  /**
   * Update an existing permission grant.
   *
   * @param grant grant entity with updated values
   */
  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void updateGrant(@Valid @RequestBody MstDataPermissionGrant grant) {
    dataPermissionGrantService.updateGrant(grant);
  }

  /**
   * Delete a permission grant.
   *
   * @param id grant ID
   * @param version version for optimistic locking
   */
  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).ADMIN.roleCode)")
  public void deleteGrant(@PathVariable Long id, @RequestParam @NotNull Integer version) {
    dataPermissionGrantService.deleteGrant(id, version);
  }
}
