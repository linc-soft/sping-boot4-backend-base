package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lincsoft.annotation.OperationLog;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.constant.OperationType;
import com.lincsoft.controller.master.vo.UserPageRequest;
import com.lincsoft.dto.CacheableUserDetails;
import com.lincsoft.dto.master.UserWithRoles;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.entity.master.MstUserRole;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.filter.JwtAuthorizationFilter;
import com.lincsoft.filter.PreAuthenticationChecks;
import com.lincsoft.mapper.master.MstUserMapper;
import com.lincsoft.mapper.master.MstUserRoleMapper;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * User Service Implementation.
 *
 * <p>This service handles user-related business logic and implements Spring Security's {@link
 * UserDetailsService} interface to provide user authentication support. It is responsible for
 * loading user details from the database and constructing Spring Security {@link UserDetails}
 * objects with appropriate roles and permissions.
 *
 * <p>Key responsibilities:
 *
 * <ul>
 *   <li>Load user information by user ID for authentication
 *   <li>Validate user status (active/inactive)
 *   <li>Retrieve and map user roles to Spring Security authorities
 *   <li>Handle user not found and inactive user scenarios
 *   <li>Cache UserDetails via Spring Cache ({@code @Cacheable}) to reduce database queries
 *   <li>Evict cached UserDetails via {@code @CacheEvict} when user state changes
 *   <li>CRUD operations for user management
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
  /**
   * User mapper for database operations.
   *
   * <p>Provides access to user-related database queries and operations.
   */
  private final MstUserMapper userMapper;

  /** User role mapper for managing user-role associations. */
  private final MstUserRoleMapper userRoleMapper;

  /**
   * Role service for retrieving user roles.
   *
   * <p>Used to fetch role information associated with a user for authority mapping.
   */
  private final RoleService roleService;

  /** Password encoder for encrypting user passwords. */
  private final PasswordEncoder passwordEncoder;

  /** Self reference for lazy initialization. */
  @Lazy private final UserService self;

  /**
   * Loads user details by username for Spring Security authentication.
   *
   * <p>This method is called by Spring Security during the authentication process (via {@link
   * DaoAuthenticationProvider}) and by {@link JwtAuthorizationFilter} for JWT token validation. It
   * performs the following steps:
   *
   * <ol>
   *   <li>Checks Spring Cache for previously loaded UserDetails (Redis-backed)
   *   <li>On cache miss, queries the database to find the user by username
   *   <li>Validates that the user exists
   *   <li>Checks if the user account is active
   *   <li>Retrieves the user's roles from the database
   *   <li>Converts roles to Spring Security authorities (with ROLE_ prefix if needed)
   *   <li>Constructs a {@link UserDetails} object (automatically cached by Spring Cache)
   * </ol>
   *
   * <p><b>Note on caching and account lock:</b> This method only caches static user data (username,
   * password hash, authorities). Account lock status is intentionally excluded from the cached
   * {@link UserDetails} and is instead checked in real-time by {@link PreAuthenticationChecks},
   * which is registered as a {@link UserDetailsChecker} on {@link DaoAuthenticationProvider}. This
   * ensures lock/unlock changes take effect immediately without being affected by cache TTL.
   *
   * @param username the username to look up
   * @return a fully populated UserDetails object with username, password, and authorities
   * @throws UsernameNotFoundException if the user is not found in the database
   * @throws DisabledException if the user exists but is inactive (disabled)
   */
  @NullMarked
  @Override
  @Cacheable(cacheNames = CommonConstants.REDIS_USER_DETAILS_PREFIX, key = "#username")
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // Build query to find user by username
    QueryWrapper<MstUser> userQueryWrapper = new QueryWrapper<>();
    userQueryWrapper.eq("username", username);
    MstUser user = userMapper.selectOne(userQueryWrapper);

    // Validate user existence
    if (user == null) {
      throw new UsernameNotFoundException("User not found: " + username);
    }

    // Validate user status - must be active
    if (user.getStatus() == null || CommonConstants.USER_STATUS_INACTIVE.equals(user.getStatus())) {
      throw new DisabledException("User is inactive");
    }

    // Retrieve user's direct roles from the database
    List<MstRole> directRoles = roleService.getRoleListByUserId(user.getId());

    // Resolve all effective role codes (direct + inherited via role hierarchy)
    List<Long> directRoleIds = directRoles.stream().map(MstRole::getId).toList();
    Set<String> allRoleCodes = roleService.resolveAllRoleCodes(directRoleIds);

    // Build authorities from all effective roles (with ROLE_ prefix)
    List<SimpleGrantedAuthority> authorities =
        allRoleCodes.stream()
            .map(
                code ->
                    new SimpleGrantedAuthority(code.startsWith("ROLE_") ? code : "ROLE_" + code))
            .toList();
    return new CacheableUserDetails(user.getUsername(), user.getPassword(), authorities);
  }

  /**
   * Evicts the cached UserDetails for the specified username.
   *
   * <p>Should be called when user state changes (e.g., role update, password change, account
   * deactivation) to ensure the next authentication loads fresh data from the database.
   *
   * @param username the username whose cache entry should be evicted
   */
  @CacheEvict(cacheNames = CommonConstants.REDIS_USER_DETAILS_PREFIX, key = "#username")
  public void evictUserDetailsCache(String username) {
    log.debug("Evicted UserDetails cache for user: {}", username);
  }

  /**
   * Get user list by query conditions.
   *
   * @param username Username (partial match)
   * @param status User status
   * @return List of users
   */
  public List<MstUser> getUserList(String username, String status) {
    QueryWrapper<MstUser> queryWrapper = new QueryWrapper<>();

    // Partial match for username
    if (username != null && !username.isBlank()) {
      queryWrapper.like("username", username);
    }

    // Exact match for status
    if (status != null && !status.isBlank()) {
      queryWrapper.eq("status", status);
    }

    // Order by update time descending
    queryWrapper.orderByDesc("update_at");

    return userMapper.selectList(queryWrapper);
  }

  /**
   * Get user page by query conditions with pagination.
   *
   * @param request Page request with pagination parameters and query conditions
   * @return IPage of users
   */
  public IPage<MstUser> getUserPage(UserPageRequest request) {
    // Build query conditions
    QueryWrapper<MstUser> queryWrapper = new QueryWrapper<>();

    // Partial match for username
    if (request.getUsername() != null && !request.getUsername().isBlank()) {
      queryWrapper.like("username", request.getUsername());
    }

    // Exact match for status
    if (request.getStatus() != null && !request.getStatus().isBlank()) {
      queryWrapper.eq("status", request.getStatus());
    }

    // Order by update time descending
    queryWrapper.orderByDesc("update_at");

    return userMapper.selectPage(request.toPage(), queryWrapper);
  }

  /**
   * Get user by ID.
   *
   * @param id User ID
   * @return MstUser entity
   * @throws BusinessException if the user is not found
   */
  public MstUser getUserById(Long id) {
    MstUser user = userMapper.selectById(id);
    if (user == null) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "user");
    }
    return user;
  }

  /**
   * Get user with its directly assigned role IDs by ID.
   *
   * <p>Loads the user entity together with the IDs of all roles directly assigned to the user (not
   * including inherited roles).
   *
   * @param id User ID
   * @return User paired with its directly assigned role IDs
   * @throws BusinessException if the user is not found
   */
  public UserWithRoles getUserWithRolesById(Long id) {
    MstUser user = getUserById(id);
    QueryWrapper<MstUserRole> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("user_id", id);
    List<Long> roleIds =
        userRoleMapper.selectList(queryWrapper).stream().map(MstUserRole::getRoleId).toList();
    return new UserWithRoles(user, roleIds);
  }

  /**
   * Create a new user.
   *
   * <p>Checks for username uniqueness before inserting. Password is encrypted before storage.
   * Throws an exception if the username already exists.
   *
   * @param user MstUser entity
   * @return The created user ID
   */
  @OperationLog(
      module = "Master",
      subModule = "User Manager",
      type = OperationType.CREATE,
      description = "User created: #{#user.username}")
  @Transactional(rollbackFor = Exception.class)
  public Long createUser(MstUser user, List<Integer> roleIds) {
    // Validate username uniqueness
    validateUsernameUnique(user.getUsername());

    // Encrypt password if provided
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    }

    // Set default status if not provided
    if (user.getStatus() == null || user.getStatus().isBlank()) {
      user.setStatus(CommonConstants.USER_STATUS_ACTIVE);
    }

    // Insert user
    int inserted = userMapper.insert(user);
    if (inserted == 0) {
      throw new BusinessException(MessageEnums.INSERT_FAILED, "user");
    }

    // Assign roles to user if roleIds is provided
    if (roleIds != null && !roleIds.isEmpty()) {
      List<MstRole> roles = roleService.getRolesByIds(roleIds);
      for (MstRole role : roles) {
        self.assignRoleToUser(user, role);
      }
    }

    return user.getId();
  }

  /**
   * Update an existing user.
   *
   * <p>Checks for username uniqueness (excluding the current user) before updating. Password is
   * encrypted if provided. Uses optimistic locking via version field. Throws an exception if the
   * username already exists or if the user was modified by another transaction.
   *
   * @param user MstUser entity with updated values
   * @param roleIds Role IDs to assign to the user (optional). When non-null, synchronizes the
   *     user's role assignments to match exactly this list (adds missing, removes extra). When
   *     null, role assignments remain unchanged.
   * @throws BusinessException if the username is duplicate or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "User Manager",
      type = OperationType.UPDATE,
      description = "User updated: #{#user.username}")
  @Transactional(rollbackFor = Exception.class)
  public void updateUser(MstUser user, List<Integer> roleIds) {
    // Get existing user for cache eviction
    MstUser existingUser = getUserById(user.getId());

    // username can't be updated
    if (!existingUser.getUsername().equals(user.getUsername())) {
      throw new BusinessException(MessageEnums.USERNAME_CANNOT_BE_UPDATED);
    }

    // Encrypt password if provided
    if (user.getPassword() != null && !user.getPassword().isBlank()) {
      user.setPassword(passwordEncoder.encode(user.getPassword()));
    } else {
      // Keep existing password if not provided
      user.setPassword(null);
    }

    // Update user (optimistic locking handled by @Version annotation)
    int updated = userMapper.updateById(user);
    if (updated == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "user");
    }

    // Synchronize user-role assignments if roleIds is provided
    if (roleIds != null) {
      syncUserRoles(existingUser, roleIds);
    }

    // Evict UserDetails cache
    evictUserDetailsCache(existingUser.getUsername());
  }

  /**
   * Synchronize the user's role assignments with the given role ID list.
   *
   * <p>Compares the user's current role assignments against the target list. Roles present in the
   * target but not currently assigned are added; roles currently assigned but absent from the
   * target are revoked. Roles present in both sets are left untouched.
   *
   * @param user The user whose role assignments will be synchronized
   * @param roleIds Target list of role IDs (an empty list will revoke all existing roles)
   */
  private void syncUserRoles(MstUser user, List<Integer> roleIds) {
    // Load current role IDs from mst_user_role
    QueryWrapper<MstUserRole> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("user_id", user.getId());
    Set<Long> currentRoleIds =
        userRoleMapper.selectList(queryWrapper).stream()
            .map(MstUserRole::getRoleId)
            .collect(Collectors.toSet());

    // Normalize target role IDs to Long for comparison
    Set<Long> targetRoleIds = roleIds.stream().map(Integer::longValue).collect(Collectors.toSet());

    // Compute diff: roles to add / roles to revoke
    Set<Long> toAdd = new HashSet<>(targetRoleIds);
    toAdd.removeAll(currentRoleIds);
    Set<Long> toRemove = new HashSet<>(currentRoleIds);
    toRemove.removeAll(targetRoleIds);

    // Assign newly added roles
    if (!toAdd.isEmpty()) {
      List<Integer> addRoleIds = toAdd.stream().map(Long::intValue).toList();
      List<MstRole> addRoles = roleService.getRolesByIds(addRoleIds);
      for (MstRole role : addRoles) {
        self.assignRoleToUser(user, role);
      }
    }

    // Revoke removed roles
    if (!toRemove.isEmpty()) {
      List<Integer> removeRoleIds = toRemove.stream().map(Long::intValue).toList();
      List<MstRole> removeRoles = roleService.getRolesByIds(removeRoleIds);
      for (MstRole role : removeRoles) {
        self.revokeRoleFromUser(user, role);
      }
    }
  }

  /**
   * Delete a user.
   *
   * <p>Uses optimistic locking via version field. Throws an exception if the user was modified by
   * another transaction.
   *
   * @param user MstUser entity to be deleted
   * @param version Version for optimistic locking
   * @throws BusinessException if the user is not found or optimistic lock fails
   */
  @OperationLog(
      module = "Master",
      subModule = "User Manager",
      type = OperationType.DELETE,
      description = "User deleted: #{#user.username}")
  @Transactional(rollbackFor = Exception.class)
  public void deleteUser(MstUser user, Integer version) {
    // Check if user not found
    if (user == null) {
      throw new BusinessException(MessageEnums.NOT_FOUND, "user");
    }

    // Delete user with optimistic locking via explicit version condition
    // Note: deleteById does NOT apply @Version check for logical delete,
    // so we use delete(wrapper) with an explicit version condition.
    LambdaUpdateWrapper<MstUser> deleteWrapper = new LambdaUpdateWrapper<>();
    deleteWrapper.eq(MstUser::getId, user.getId()).eq(MstUser::getVersion, version);
    int deleted = userMapper.delete(deleteWrapper);
    if (deleted == 0) {
      throw new BusinessException(MessageEnums.OPTIMISTIC_LOCK_FAILED, "user");
    }

    // Evict UserDetails cache
    evictUserDetailsCache(user.getUsername());
  }

  /**
   * Assign a role to a user.
   *
   * <p>Creates a user-role association record. Each invocation generates one operation log entry.
   *
   * @param user The user to assign the role to
   * @param role The role to assign
   */
  @OperationLog(
      module = "Master",
      subModule = "User Manager",
      type = OperationType.CREATE,
      description = "Assigned role #{#role.roleName} to user: #{#user.username}")
  public void assignRoleToUser(MstUser user, MstRole role) {
    MstUserRole userRole = new MstUserRole();
    userRole.setUserId(user.getId());
    userRole.setRoleId(role.getId());
    userRoleMapper.insert(userRole);
  }

  /**
   * Revoke a role from a user.
   *
   * <p>Deletes the user-role association record identified by user ID and role ID. Each invocation
   * generates one operation log entry.
   *
   * @param user The user to revoke the role from
   * @param role The role to revoke
   */
  @OperationLog(
      module = "Master",
      subModule = "User Manager",
      type = OperationType.DELETE,
      description = "Revoked role #{#role.roleName} from user: #{#user.username}")
  public void revokeRoleFromUser(MstUser user, MstRole role) {
    QueryWrapper<MstUserRole> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("user_id", user.getId()).eq("role_id", role.getId());
    userRoleMapper.delete(queryWrapper);
  }

  /**
   * Validate that the username is unique.
   *
   * @param username Username to check
   * @throws BusinessException if the username already exists
   */
  private void validateUsernameUnique(String username) {
    QueryWrapper<MstUser> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("username", username);
    if (userMapper.selectCount(queryWrapper) > 0) {
      throw new BusinessException(MessageEnums.UNIQUE_CONSTRAINT_VIOLATION, "username");
    }
  }
}
