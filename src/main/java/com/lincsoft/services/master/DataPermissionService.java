package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.DataScopeType;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.PermissionBit;
import com.lincsoft.constant.ResourceType;
import com.lincsoft.constant.SubjectType;
import com.lincsoft.entity.master.MstDataPermissionGrant;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstRoleDataScope;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstDataPermissionGrantMapper;
import com.lincsoft.mapper.master.MstRoleDataScopeMapper;
import com.lincsoft.mapper.master.MstRoleMapper;
import com.lincsoft.mapper.master.MstUserMapper;
import com.lincsoft.mapper.master.MstUserRoleMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Core data permission service.
 *
 * <p>Provides three main capabilities:
 *
 * <ol>
 *   <li>Resolve the set of department IDs accessible to the current user via role data scopes (used
 *       by the SQL interceptor for query filtering).
 *   <li>Resolve the set of resource IDs accessible to the current user via row-level grants (used
 *       by the SQL interceptor for query filtering).
 *   <li>Check whether the current user has a specific permission on a specific resource (used by
 *       the AOP aspect for write/delete/export operations).
 * </ol>
 *
 * <p>Results are cached in Redis with a 5-minute TTL. Callers must evict the cache when permission
 * data changes (role assignment, dept assignment, grant changes).
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Service
@RequiredArgsConstructor
public class DataPermissionService {

  /** Redis TTL for permission cache entries (5 minutes). */
  private static final long CACHE_TTL_MINUTES = 5L;

  private final MstRoleMapper roleMapper;
  private final MstUserRoleMapper userRoleMapper;
  private final MstRoleDataScopeMapper roleDataScopeMapper;
  private final MstDataPermissionGrantMapper grantMapper;
  private final DeptService deptService;
  private final RedisTemplate<String, Object> redisTemplate;
  private final MstUserMapper userMapper;

  // -------------------------------------------------------------------------
  // Public API
  // -------------------------------------------------------------------------

  /**
   * Resolve all department IDs accessible to the given user via role data scopes.
   *
   * <p>Returns a set containing {@link CommonConstants#DATA_PERM_ALL_SCOPE_SENTINEL} as the sole
   * element when the user has an ALL-scope role, signalling the interceptor to skip dept filtering.
   *
   * <p>Results are cached in Redis under key {@code dataperm:dept:{userId}}.
   *
   * @param userId user ID
   * @return set of accessible dept IDs, or singleton {@code {-1L}} for ALL-scope users
   */
  @SuppressWarnings("unchecked")
  public Set<Long> resolveAccessibleDeptIds(Long userId) {
    String cacheKey = CommonConstants.REDIS_DATA_PERM_DEPT_IDS_PREFIX + userId;

    // Try cache first
    Object cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached instanceof Set<?> cachedSet) {
      return (Set<Long>) cachedSet;
    }

    Set<Long> result = computeAccessibleDeptIds(userId);

    // Store in Redis with TTL
    redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    return result;
  }

  /**
   * Resolve all resource IDs accessible to the given user via row-level grants for the specified
   * resource type and permission bit.
   *
   * <p>Results are cached in Redis under key {@code
   * dataperm:resource:{userId}:{resourceType}:{permBit}}.
   *
   * @param userId user ID
   * @param resourceType resource type enum value
   * @param permission required permission bit
   * @return set of accessible resource IDs
   */
  @SuppressWarnings("unchecked")
  public Set<Long> resolveGrantedResourceIds(
      Long userId, ResourceType resourceType, PermissionBit permission) {
    String cacheKey =
        CommonConstants.REDIS_DATA_PERM_RESOURCE_IDS_PREFIX
            + userId
            + ":"
            + resourceType.value()
            + ":"
            + permission.name();

    // Try cache first
    Object cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached instanceof Set<?> cachedSet) {
      return (Set<Long>) cachedSet;
    }

    Set<Long> result = computeGrantedResourceIds(userId, resourceType, permission);

    // Store in Redis with TTL
    redisTemplate.opsForValue().set(cacheKey, result, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
    return result;
  }

  /**
   * Check whether the current authenticated user has the specified permission on the given
   * resource.
   *
   * <p>Permission is granted if:
   *
   * <ul>
   *   <li>The user has an ALL-scope role data scope with the required permission bit, OR
   *   <li>The user's accessible dept IDs include the resource's dept, OR
   *   <li>The user has a row-level grant for the resource with the required permission bit
   * </ul>
   *
   * @param resourceType resource type
   * @param resourceId resource instance ID
   * @param permission required permission bit
   * @throws BusinessException with {@link MessageEnums#FORBIDDEN} if the check fails
   */
  public void checkPermission(
      ResourceType resourceType, Long resourceId, PermissionBit permission) {
    Long userId = getCurrentUserId();

    // Check via row-level grants
    Set<Long> grantedIds = resolveGrantedResourceIds(userId, resourceType, permission);
    if (grantedIds.contains(resourceId)) {
      return;
    }

    // Check via dept scope (ALL sentinel means full access)
    Set<Long> deptIds = resolveAccessibleDeptIds(userId);
    if (deptIds.contains(CommonConstants.DATA_PERM_ALL_SCOPE_SENTINEL)) {
      return;
    }

    // No permission found
    throw new BusinessException(MessageEnums.FORBIDDEN);
  }

  /**
   * Evict all data permission cache entries for the given user.
   *
   * <p>Should be called whenever the user's role assignments, dept assignments, or grant records
   * change.
   *
   * @param userId user ID whose cache should be invalidated
   */
  public void evictUserPermissionCache(Long userId) {
    // Delete dept IDs cache
    redisTemplate.delete(CommonConstants.REDIS_DATA_PERM_DEPT_IDS_PREFIX + userId);

    // Delete all resource ID caches for this user (pattern-based delete)
    String pattern = CommonConstants.REDIS_DATA_PERM_RESOURCE_IDS_PREFIX + userId + ":*";
    Set<String> keys = redisTemplate.keys(pattern);
    if (keys != null && !keys.isEmpty()) {
      redisTemplate.delete(keys);
    }
  }

  // -------------------------------------------------------------------------
  // Role chain resolution
  // -------------------------------------------------------------------------

  /**
   * Resolve the complete role chain for the given user, including all inherited parent roles.
   *
   * <p>Uses BFS traversal to follow {@code parentRoleId} links. The result is deduplicated and
   * cycle-safe (visited set prevents infinite loops).
   *
   * @param userId user ID
   * @return list of all role IDs accessible to the user (direct + inherited)
   */
  public List<Long> getRoleChain(Long userId) {
    // Collect direct role assignments
    List<MstUserRole> userRoles =
        userRoleMapper.selectList(new QueryWrapper<MstUserRole>().eq("user_id", userId));

    Set<Long> visited = new HashSet<>();
    Queue<Long> queue = new LinkedList<>();

    for (MstUserRole ur : userRoles) {
      if (visited.add(ur.getRoleId())) {
        queue.offer(ur.getRoleId());
      }
    }

    // BFS: follow parentRoleId links upward
    while (!queue.isEmpty()) {
      Long roleId = queue.poll();
      MstRole role = roleMapper.selectById(roleId);
      if (role != null && role.getParentRoleId() != null) {
        Long parentId = role.getParentRoleId();
        if (visited.add(parentId)) {
          queue.offer(parentId);
        }
      }
    }

    return new ArrayList<>(visited);
  }

  // -------------------------------------------------------------------------
  // Internal computation (no caching)
  // -------------------------------------------------------------------------

  /**
   * Compute accessible dept IDs without cache.
   *
   * @param userId user ID
   * @return computed set of dept IDs (or ALL sentinel)
   */
  private Set<Long> computeAccessibleDeptIds(Long userId) {
    List<Long> roleIds = getRoleChain(userId);

    // Load all enabled, non-deleted data scopes for the role chain
    List<MstRoleDataScope> scopes =
        roleDataScopeMapper.selectList(
            new QueryWrapper<MstRoleDataScope>().in("role_id", roleIds).eq("enabled", true));

    Set<Long> deptIds = new HashSet<>();

    for (MstRoleDataScope scope : scopes) {
      DataScopeType type = DataScopeType.valueOf(scope.getScopeType());
      if (type == DataScopeType.ALL) {
        // ALL scope: return sentinel immediately
        Set<Long> allSentinel = new HashSet<>();
        allSentinel.add(CommonConstants.DATA_PERM_ALL_SCOPE_SENTINEL);
        return allSentinel;
      } else if (type == DataScopeType.DEPT) {
        if (scope.getDeptId() != null) {
          deptIds.add(scope.getDeptId());
        }
      } else if (type == DataScopeType.DEPT_AND_CHILD) {
        if (scope.getDeptId() != null) {
          deptIds.addAll(deptService.collectDescendantIds(scope.getDeptId()));
        }
      }
    }

    return deptIds;
  }

  /**
   * Compute granted resource IDs without cache.
   *
   * @param userId user ID
   * @param resourceType resource type
   * @param permission required permission bit
   * @return computed set of resource IDs
   */
  private Set<Long> computeGrantedResourceIds(
      Long userId, ResourceType resourceType, PermissionBit permission) {
    LocalDateTime now = LocalDateTime.now();

    // Build the full set of subjects for this user
    List<Object[]> subjects = new ArrayList<>();
    subjects.add(new Object[] {SubjectType.USER.name(), userId});

    // Add all roles in the chain
    for (Long roleId : getRoleChain(userId)) {
      subjects.add(new Object[] {SubjectType.ROLE.name(), roleId});
    }

    // Add user's departments and their ancestors
    Set<Long> userDeptIds = deptService.getUserDeptIds(userId);
    for (Long deptId : userDeptIds) {
      subjects.add(new Object[] {SubjectType.DEPT.name(), deptId});
      for (Long ancestorId : deptService.getAncestorDeptIds(deptId)) {
        subjects.add(new Object[] {SubjectType.DEPT.name(), ancestorId});
      }
    }

    Set<Long> resourceIds = new HashSet<>();

    // Query grants for each subject type group
    for (Object[] subject : subjects) {
      String subjectType = (String) subject[0];
      Long subjectId = (Long) subject[1];

      List<MstDataPermissionGrant> grants =
          grantMapper.selectList(
              new QueryWrapper<MstDataPermissionGrant>()
                  .eq("resource_type", resourceType.value())
                  .eq("subject_type", subjectType)
                  .eq("subject_id", subjectId)
                  // valid_from is null or <= now
                  .and(w -> w.isNull("valid_from").or().le("valid_from", now))
                  // valid_until is null or > now
                  .and(w -> w.isNull("valid_until").or().gt("valid_until", now)));

      for (MstDataPermissionGrant grant : grants) {
        if (permission.isGranted(grant.getPermBits())) {
          resourceIds.add(grant.getResourceId());
        }
      }
    }

    return resourceIds;
  }

  /**
   * Get the current authenticated user's ID from the Spring Security context.
   *
   * <p>Looks up the user record by username to retrieve the numeric ID.
   *
   * @return current user ID
   * @throws BusinessException if no authenticated user is found
   */
  private Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      throw new BusinessException(MessageEnums.UNAUTHORIZED);
    }
    Object principal = auth.getPrincipal();
    if (principal instanceof UserDetails userDetails) {
      // Username is the login name; look up the user ID from the database
      MstUser user =
          userMapper.selectOne(
              new QueryWrapper<MstUser>().eq("username", userDetails.getUsername()));
      if (user == null) {
        throw new BusinessException(MessageEnums.UNAUTHORIZED);
      }
      return user.getId();
    }
    throw new BusinessException(MessageEnums.UNAUTHORIZED);
  }
}
