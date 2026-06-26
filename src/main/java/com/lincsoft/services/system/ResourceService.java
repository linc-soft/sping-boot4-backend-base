package com.lincsoft.services.system;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.ModuleEnums;
import com.lincsoft.constant.OperationEnums;
import com.lincsoft.constant.RoleCodeEnums;
import com.lincsoft.constant.SubModuleEnums;
import com.lincsoft.controller.system.vo.ResourceTreeNode;
import com.lincsoft.entity.system.SysResource;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.system.SysResourceMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resource service.
 *
 * <p>Provides business logic for frontend permission resource management: tree construction,
 * visibility filtering, and CRUD with validation (role_code membership, parent existence, code
 * uniqueness, directory role_code constraint).
 *
 * <p>The full resource tree is cached in Redis and evicted on any write operation.
 *
 * @author 林创科技
 * @since 2026-06-17
 */
@Service
@RequiredArgsConstructor
public class ResourceService {

  private final SysResourceMapper resourceMapper;

  private static final int TYPE_DIRECTORY = 0;
  private static final int TYPE_PAGE = 1;
  private static final int TYPE_BUTTON = 2;

  @Cacheable(cacheNames = CommonConstants.REDIS_RESOURCE_TREE_PREFIX, key = "'all'")
  public List<ResourceTreeNode> getResourceTree() {
    List<SysResource> all = listAll();
    return buildTree(all);
  }

  /**
   * Returns the visible resource tree for the current user.
   *
   * <p>Visibility rules:
   *
   * <ul>
   *   <li>ADMIN: full tree
   *   <li>Others: directories always visible (as shells); pages/buttons visible only if their
   *       role_code is in the user's resolved role codes; directories with no visible descendants
   *       are pruned
   * </ul>
   *
   * @param roleCodes the current user's resolved role codes (with ROLE_ prefix stripped)
   * @return filtered resource tree
   */
  public List<ResourceTreeNode> getVisibleResourceTree(Set<String> roleCodes) {
    List<SysResource> all = listAll();
    if (roleCodes.contains(RoleCodeEnums.ADMIN.getRoleCode())) {
      return buildTree(all);
    }
    return buildVisibleTree(all, roleCodes);
  }

  public SysResource getResourceById(Long id) {
    SysResource resource = resourceMapper.selectById(id);
    if (resource == null) {
      throw new BusinessException(MessageEnums.RESOURCE_NOT_FOUND);
    }
    return resource;
  }

  @CacheEvict(cacheNames = CommonConstants.REDIS_RESOURCE_TREE_PREFIX, key = "'all'")
  @OperationLog(
      module = ModuleEnums.SYSTEM,
      subModule = SubModuleEnums.PERMISSION,
      type = OperationEnums.UPDATE,
      description = "Resource updated: #{#resource.resourceCode}")
  @Transactional(rollbackFor = Exception.class)
  public void updateResource(SysResource resource) {
    getResourceById(resource.getId());
    validateResourceCodeUnique(resource.getResourceCode(), resource.getId());
    validateParent(resource.getParentId(), resource.getType());
    validateRoleCode(resource.getType(), resource.getRoleCode());

    int updated = resourceMapper.updateById(resource);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.RESOURCE_OPTIMISTIC_LOCK_FAILED);
    }
  }

  private List<SysResource> listAll() {
    QueryWrapper<SysResource> qw = new QueryWrapper<>();
    qw.orderByAsc("sort_order").orderByAsc("id");
    return resourceMapper.selectList(qw);
  }

  private List<ResourceTreeNode> buildTree(List<SysResource> resources) {
    Map<Long, List<ResourceTreeNode>> childrenMap = new LinkedHashMap<>();
    List<ResourceTreeNode> roots = new ArrayList<>();
    for (SysResource r : resources) {
      ResourceTreeNode node = toNode(r, new ArrayList<>());
      if (r.getParentId() == null || r.getParentId() == 0L) {
        roots.add(node);
      } else {
        childrenMap.computeIfAbsent(r.getParentId(), k -> new ArrayList<>()).add(node);
      }
    }
    attachChildren(roots, childrenMap);
    sortNodes(roots);
    return roots;
  }

  private List<ResourceTreeNode> buildVisibleTree(List<SysResource> all, Set<String> roleCodes) {
    Map<Long, List<ResourceTreeNode>> childrenMap = new LinkedHashMap<>();
    List<ResourceTreeNode> roots = new ArrayList<>();
    for (SysResource r : all) {
      if (!isVisible(r, roleCodes)) {
        continue;
      }
      ResourceTreeNode node = toNode(r, new ArrayList<>());
      if (r.getParentId() == null || r.getParentId() == 0L) {
        roots.add(node);
      } else {
        childrenMap.computeIfAbsent(r.getParentId(), k -> new ArrayList<>()).add(node);
      }
    }
    attachChildren(roots, childrenMap);
    sortNodes(roots);
    roots = pruneEmptyDirectories(roots);
    return roots;
  }

  private boolean isVisible(SysResource r, Set<String> roleCodes) {
    if (r.getType() == TYPE_DIRECTORY) {
      return true;
    }
    if (r.getRoleCode() == null || r.getRoleCode().isBlank()) {
      return true;
    }
    return roleCodes.contains(r.getRoleCode());
  }

  private List<ResourceTreeNode> pruneEmptyDirectories(List<ResourceTreeNode> nodes) {
    List<ResourceTreeNode> result = new ArrayList<>();
    for (ResourceTreeNode node : nodes) {
      List<ResourceTreeNode> prunedChildren = pruneEmptyDirectories(node.children());
      if (node.type() == TYPE_DIRECTORY && prunedChildren.isEmpty()) {
        continue;
      }
      result.add(
          new ResourceTreeNode(
              node.id(),
              node.resourceCode(),
              node.resourceName(),
              node.type(),
              node.parentId(),
              node.routePath(),
              node.icon(),
              node.sortOrder(),
              node.roleCode(),
              node.status(),
              prunedChildren));
    }
    return result;
  }

  private void attachChildren(List<ResourceTreeNode> nodes, Map<Long, List<ResourceTreeNode>> map) {
    for (ResourceTreeNode node : nodes) {
      List<ResourceTreeNode> children = map.get(node.id());
      if (children != null) {
        node.children().addAll(children);
        attachChildren(children, map);
      }
    }
  }

  private void sortNodes(List<ResourceTreeNode> nodes) {
    nodes.sort(
        Comparator.comparing(
                (ResourceTreeNode n) -> n.sortOrder() == null ? Integer.MAX_VALUE : n.sortOrder())
            .thenComparing(ResourceTreeNode::id));
    for (ResourceTreeNode node : nodes) {
      sortNodes(node.children());
    }
  }

  private ResourceTreeNode toNode(SysResource r, List<ResourceTreeNode> children) {
    return new ResourceTreeNode(
        r.getId(),
        r.getResourceCode(),
        r.getResourceName(),
        r.getType(),
        r.getParentId(),
        r.getRoutePath(),
        r.getIcon(),
        r.getSortOrder(),
        r.getRoleCode(),
        r.getStatus(),
        children);
  }

  private void validateResourceCodeUnique(String code, Long excludeId) {
    if (code == null || code.isBlank()) {
      return;
    }
    QueryWrapper<SysResource> qw = new QueryWrapper<>();
    qw.eq("resource_code", code);
    if (excludeId != null) {
      qw.ne("id", excludeId);
    }
    if (resourceMapper.selectCount(qw) > 0) {
      throw new BusinessException(MessageEnums.RESOURCE_CODE_EXISTS);
    }
  }

  private void validateParent(Long parentId, Integer type) {
    if (parentId == null || parentId == 0L) {
      return;
    }
    SysResource parent = resourceMapper.selectById(parentId);
    if (parent == null) {
      throw new BusinessException(MessageEnums.RESOURCE_INVALID_PARENT);
    }
    if (type != null && type == TYPE_PAGE && parent.getType() != TYPE_DIRECTORY) {
      throw new BusinessException(MessageEnums.RESOURCE_INVALID_PARENT);
    }
  }

  private void validateRoleCode(Integer type, String roleCode) {
    if (type != null && type == TYPE_DIRECTORY) {
      if (roleCode != null && !roleCode.isBlank()) {
        throw new BusinessException(MessageEnums.RESOURCE_DIRECTORY_ROLE_CODE_MUST_BE_NULL);
      }
      return;
    }
    if (roleCode == null || roleCode.isBlank()) {
      return;
    }
    if (!RoleCodeEnums.getValidCodes().contains(roleCode)) {
      throw new BusinessException(MessageEnums.RESOURCE_INVALID_ROLE_CODE);
    }
  }
}
