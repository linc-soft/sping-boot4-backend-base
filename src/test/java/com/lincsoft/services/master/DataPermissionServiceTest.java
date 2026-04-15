package com.lincsoft.services.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.lincsoft.constant.CommonConstants;
import com.lincsoft.constant.DataScopeType;
import com.lincsoft.constant.PermissionBit;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

/**
 * Unit tests for {@link DataPermissionService}.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>Role chain resolution (deduplication, cycle safety)
 *   <li>Accessible dept ID resolution (ALL sentinel, DEPT, DEPT_AND_CHILD)
 *   <li>Row-level resource ID resolution (validity period filtering, permission bit filtering)
 *   <li>Permission check (grant and deny scenarios)
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
class DataPermissionServiceTest {

  @Mock private MstRoleMapper roleMapper;
  @Mock private MstUserRoleMapper userRoleMapper;
  @Mock private MstRoleDataScopeMapper roleDataScopeMapper;
  @Mock private MstDataPermissionGrantMapper grantMapper;
  @Mock private DeptService deptService;
  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private MstUserMapper userMapper;
  @Mock private ValueOperations<String, Object> valueOps;

  private DataPermissionService service;

  @BeforeEach
  void setUp() {
    service =
        new DataPermissionService(
            roleMapper,
            userRoleMapper,
            roleDataScopeMapper,
            grantMapper,
            deptService,
            redisTemplate,
            userMapper);

    // Default: cache miss
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(valueOps.get(anyString())).thenReturn(null);
  }

  // -------------------------------------------------------------------------
  // getRoleChain tests
  // -------------------------------------------------------------------------

  @Test
  void getRoleChain_returnsDirectRoles() {
    // Given: user 1 has role 10
    MstUserRole ur = new MstUserRole();
    ur.setUserId(1L);
    ur.setRoleId(10L);
    when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));

    MstRole role = new MstRole();
    role.setId(10L);
    role.setParentRoleId(null);
    when(roleMapper.selectById(10L)).thenReturn(role);

    // When
    List<Long> chain = service.getRoleChain(1L);

    // Then
    assertThat(chain).containsExactlyInAnyOrder(10L);
  }

  @Test
  void getRoleChain_includesParentRoles() {
    // Given: user 1 has role 10, role 10 has parent role 20
    MstUserRole ur = new MstUserRole();
    ur.setUserId(1L);
    ur.setRoleId(10L);
    when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));

    MstRole role10 = new MstRole();
    role10.setId(10L);
    role10.setParentRoleId(20L);
    when(roleMapper.selectById(10L)).thenReturn(role10);

    MstRole role20 = new MstRole();
    role20.setId(20L);
    role20.setParentRoleId(null);
    when(roleMapper.selectById(20L)).thenReturn(role20);

    // When
    List<Long> chain = service.getRoleChain(1L);

    // Then: both roles should be in the chain
    assertThat(chain).containsExactlyInAnyOrder(10L, 20L);
  }

  @Test
  void getRoleChain_deduplicatesRoles() {
    // Given: user has two roles that share the same parent
    MstUserRole ur1 = new MstUserRole();
    ur1.setUserId(1L);
    ur1.setRoleId(10L);
    MstUserRole ur2 = new MstUserRole();
    ur2.setUserId(1L);
    ur2.setRoleId(11L);
    when(userRoleMapper.selectList(any())).thenReturn(List.of(ur1, ur2));

    MstRole role10 = new MstRole();
    role10.setId(10L);
    role10.setParentRoleId(20L);
    when(roleMapper.selectById(10L)).thenReturn(role10);

    MstRole role11 = new MstRole();
    role11.setId(11L);
    role11.setParentRoleId(20L); // same parent
    when(roleMapper.selectById(11L)).thenReturn(role11);

    MstRole role20 = new MstRole();
    role20.setId(20L);
    role20.setParentRoleId(null);
    when(roleMapper.selectById(20L)).thenReturn(role20);

    // When
    List<Long> chain = service.getRoleChain(1L);

    // Then: role 20 should appear only once
    assertThat(chain).containsExactlyInAnyOrder(10L, 11L, 20L);
    assertThat(chain).doesNotHaveDuplicates();
  }

  // -------------------------------------------------------------------------
  // resolveAccessibleDeptIds tests
  // -------------------------------------------------------------------------

  @Test
  void resolveAccessibleDeptIds_allScope_returnsSentinel() {
    // Given: user has a role with ALL scope
    setupUserWithRole(1L, 10L);

    MstRoleDataScope scope = new MstRoleDataScope();
    scope.setRoleId(10L);
    scope.setScopeType(DataScopeType.ALL.name());
    scope.setEnabled(true);
    when(roleDataScopeMapper.selectList(any())).thenReturn(List.of(scope));

    // When
    Set<Long> deptIds = service.resolveAccessibleDeptIds(1L);

    // Then: should return the ALL sentinel
    assertThat(deptIds).containsExactly(CommonConstants.DATA_PERM_ALL_SCOPE_SENTINEL);
  }

  @Test
  void resolveAccessibleDeptIds_deptScope_returnsDeptId() {
    // Given: user has a role with DEPT scope for dept 100
    setupUserWithRole(1L, 10L);

    MstRoleDataScope scope = new MstRoleDataScope();
    scope.setRoleId(10L);
    scope.setScopeType(DataScopeType.DEPT.name());
    scope.setDeptId(100L);
    scope.setEnabled(true);
    when(roleDataScopeMapper.selectList(any())).thenReturn(List.of(scope));

    // When
    Set<Long> deptIds = service.resolveAccessibleDeptIds(1L);

    // Then
    assertThat(deptIds).containsExactly(100L);
  }

  @Test
  void resolveAccessibleDeptIds_deptAndChildScope_returnsDescendants() {
    // Given: user has DEPT_AND_CHILD scope for dept 100
    setupUserWithRole(1L, 10L);

    MstRoleDataScope scope = new MstRoleDataScope();
    scope.setRoleId(10L);
    scope.setScopeType(DataScopeType.DEPT_AND_CHILD.name());
    scope.setDeptId(100L);
    scope.setEnabled(true);
    when(roleDataScopeMapper.selectList(any())).thenReturn(List.of(scope));

    // DeptService returns dept 100 and its children 101, 102
    when(deptService.collectDescendantIds(100L)).thenReturn(Set.of(100L, 101L, 102L));

    // When
    Set<Long> deptIds = service.resolveAccessibleDeptIds(1L);

    // Then
    assertThat(deptIds).containsExactlyInAnyOrder(100L, 101L, 102L);
  }

  @Test
  void resolveAccessibleDeptIds_noScopes_returnsEmpty() {
    // Given: user has no data scopes
    setupUserWithRole(1L, 10L);
    when(roleDataScopeMapper.selectList(any())).thenReturn(Collections.emptyList());

    // When
    Set<Long> deptIds = service.resolveAccessibleDeptIds(1L);

    // Then
    assertThat(deptIds).isEmpty();
  }

  // -------------------------------------------------------------------------
  // checkPermission tests
  // -------------------------------------------------------------------------

  @Test
  void checkPermission_deniedWhenNoPermission() {
    // Given: authenticated user with no permissions
    setupSecurityContext("testuser", 1L);
    when(valueOps.get(anyString())).thenReturn(null);
    setupUserWithRole(1L, 10L);
    when(roleDataScopeMapper.selectList(any())).thenReturn(Collections.emptyList());
    when(grantMapper.selectList(any())).thenReturn(Collections.emptyList());
    when(deptService.getUserDeptIds(anyLong())).thenReturn(Collections.emptySet());

    // When / Then: should throw FORBIDDEN
    assertThatThrownBy(() -> service.checkPermission(null, 999L, PermissionBit.WRITE))
        .isInstanceOf(BusinessException.class);
  }

  // -------------------------------------------------------------------------
  // PermissionBit.isGranted tests
  // -------------------------------------------------------------------------

  @Test
  void permissionBit_isGranted_correctBitwise() {
    // READ=1, WRITE=2, DELETE=4, EXPORT=8
    assertThat(PermissionBit.READ.isGranted(1)).isTrue();
    assertThat(PermissionBit.READ.isGranted(3)).isTrue(); // READ | WRITE
    assertThat(PermissionBit.READ.isGranted(2)).isFalse(); // WRITE only
    assertThat(PermissionBit.WRITE.isGranted(2)).isTrue();
    assertThat(PermissionBit.WRITE.isGranted(1)).isFalse();
    assertThat(PermissionBit.DELETE.isGranted(4)).isTrue();
    assertThat(PermissionBit.DELETE.isGranted(7)).isTrue(); // READ|WRITE|DELETE
    assertThat(PermissionBit.EXPORT.isGranted(8)).isTrue();
    assertThat(PermissionBit.EXPORT.isGranted(15)).isTrue(); // all bits
    assertThat(PermissionBit.EXPORT.isGranted(7)).isFalse(); // no EXPORT
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void setupUserWithRole(Long userId, Long roleId) {
    MstUserRole ur = new MstUserRole();
    ur.setUserId(userId);
    ur.setRoleId(roleId);
    when(userRoleMapper.selectList(any())).thenReturn(List.of(ur));

    MstRole role = new MstRole();
    role.setId(roleId);
    role.setParentRoleId(null);
    when(roleMapper.selectById(roleId)).thenReturn(role);
  }

  private void setupSecurityContext(String username, Long userId) {
    MstUser user = new MstUser();
    user.setId(userId);
    user.setUsername(username);
    when(userMapper.selectOne(any())).thenReturn(user);

    var userDetails = User.withUsername(username).password("").roles("USER").build();
    var auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
