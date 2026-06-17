package com.lincsoft.controller.system;

import com.lincsoft.controller.system.vo.ResourceCreateRequest;
import com.lincsoft.controller.system.vo.ResourceDeleteRequest;
import com.lincsoft.controller.system.vo.ResourceTreeNode;
import com.lincsoft.controller.system.vo.ResourceUpdateRequest;
import com.lincsoft.entity.system.SysResource;
import com.lincsoft.services.system.ResourceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Resource controller.
 *
 * <p>Provides endpoints for frontend permission resource management. The {@code /mine} endpoint
 * returns the visible resource tree for the current authenticated user and requires no special role
 * — only authentication. CRUD endpoints require {@code RESOURCE_READ} / {@code RESOURCE_WRITE}.
 *
 * @author 林创科技
 * @since 2026-06-17
 */
@RestController
@RequestMapping("/api/system/resources")
@RequiredArgsConstructor
public class ResourceController {

  private final ResourceService resourceService;

  @GetMapping("/mine")
  public List<ResourceTreeNode> getMyResources() {
    Set<String> roleCodes = extractRoleCodes();
    return resourceService.getVisibleResourceTree(roleCodes);
  }

  @GetMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).RESOURCE_READ.roleCode)")
  public List<ResourceTreeNode> getResourceTree() {
    return resourceService.getResourceTree();
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).RESOURCE_READ.roleCode)")
  public SysResource getResource(@PathVariable Long id) {
    return resourceService.getResourceById(id);
  }

  @PostMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).RESOURCE_WRITE.roleCode)")
  public Long createResource(@Valid @RequestBody ResourceCreateRequest request) {
    return resourceService.createResource(toEntity(request));
  }

  @PutMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).RESOURCE_WRITE.roleCode)")
  public void updateResource(@Valid @RequestBody ResourceUpdateRequest request) {
    resourceService.updateResource(toEntity(request));
  }

  @DeleteMapping
  @PreAuthorize("hasRole(T(com.lincsoft.constant.RoleCodeEnums).RESOURCE_WRITE.roleCode)")
  public void deleteResource(@Valid @RequestBody ResourceDeleteRequest request) {
    resourceService.deleteResource(
        resourceService.getResourceById(request.id()), request.version());
  }

  private Set<String> extractRoleCodes() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return Set.of();
    }
    return auth.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring(5))
        .collect(Collectors.toSet());
  }

  private SysResource toEntity(ResourceCreateRequest r) {
    SysResource e = new SysResource();
    e.setResourceCode(r.resourceCode());
    e.setResourceName(r.resourceName());
    e.setType(r.type());
    e.setParentId(r.parentId());
    e.setRoutePath(r.routePath());
    e.setIcon(r.icon());
    e.setSortOrder(r.sortOrder());
    e.setRoleCode(r.roleCode());
    e.setStatus(r.status());
    return e;
  }

  private SysResource toEntity(ResourceUpdateRequest r) {
    SysResource e = new SysResource();
    e.setId(r.id());
    e.setResourceCode(r.resourceCode());
    e.setResourceName(r.resourceName());
    e.setType(r.type());
    e.setParentId(r.parentId());
    e.setRoutePath(r.routePath());
    e.setIcon(r.icon());
    e.setSortOrder(r.sortOrder());
    e.setRoleCode(r.roleCode());
    e.setStatus(r.status());
    e.setVersion(r.version());
    return e;
  }
}
