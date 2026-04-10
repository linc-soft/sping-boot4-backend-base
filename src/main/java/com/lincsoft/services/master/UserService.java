package com.lincsoft.services.master;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.MessageEnums;
import com.lincsoft.entity.master.MstRole;
import com.lincsoft.entity.master.MstUser;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstUserMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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

  /**
   * Role service for retrieving user roles.
   *
   * <p>Used to fetch role information associated with a user for authority mapping.
   */
  private final RoleService roleService;

  /**
   * Loads user details by username for Spring Security authentication.
   *
   * <p>This method is called by Spring Security during the authentication process (via {@link
   * org.springframework.security.authentication.dao.DaoAuthenticationProvider}) and by {@link
   * com.lincsoft.filter.JwtAuthorizationFilter} for JWT token validation. It performs the following
   * steps:
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
   * @param username the username to look up
   * @return a fully populated UserDetails object with username, password, and authorities
   * @throws UsernameNotFoundException if the user is not found in the database
   * @throws BusinessException if the user exists but is inactive
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
      throw new BusinessException(MessageEnums.USER_NOT_FOUND);
    }

    // Validate user status - must be active
    if (user.getStatus() == null || CommonConstants.USER_STATUS_INACTIVE.equals(user.getStatus())) {
      throw new BusinessException(MessageEnums.USER_INACTIVE);
    }

    // Retrieve user's roles from the database
    List<MstRole> roleList = roleService.getRoleListByUserId(user.getId());
    // Build and return Spring Security UserDetails object with mapped authorities
    return User.builder()
        .username(user.getUsername())
        .password(user.getPassword())
        .authorities(
            roleList.stream()
                .filter(role -> role.getRoleCode() != null)
                .map(
                    role ->
                        new SimpleGrantedAuthority(
                            role.getRoleCode().startsWith("ROLE_")
                                ? role.getRoleCode()
                                : "ROLE_" + role.getRoleCode()))
                .toList())
        .build();
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
}
