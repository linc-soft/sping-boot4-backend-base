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
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * User Service
 *
 * @author 林创科技
 * @since 2026-04-08
 */
@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
  private final MstUserMapper userMapper;
  private final RoleService roleService;

  /**
   * Load user by username
   *
   * @param username the username identifying the user whose data is required.
   * @return UserDetails object
   */
  @NullMarked
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    // username to find user
    QueryWrapper<MstUser> userQueryWrapper = new QueryWrapper<>();
    userQueryWrapper.eq("username", username);
    MstUser user = userMapper.selectOne(userQueryWrapper);

    // If user does not exist, throw exception
    if (user == null) {
      throw new BusinessException(MessageEnums.USER_NOT_FOUND);
    }

    // If user is inactive, throw exception
    if (user.getStatus() == null || CommonConstants.USER_STATUS_INACTIVE.equals(user.getStatus())) {
      throw new BusinessException(MessageEnums.USER_INACTIVE);
    }

    // Get role list by user ID
    List<MstRole> roleList = roleService.getRoleListByUserId(user.getId());
    // Construct and return the Spring Security UserDetails object.
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
}
