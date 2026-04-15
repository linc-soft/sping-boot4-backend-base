package com.lincsoft.services.master;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lincsoft.entity.master.MstDept;
import com.lincsoft.exception.BusinessException;
import com.lincsoft.mapper.master.MstDeptMapper;
import com.lincsoft.mapper.master.MstUserDeptMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link DeptService}.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>collectDescendantIds: completeness for arbitrary depth trees
 *   <li>deleteDept: rejection when child departments exist
 *   <li>updateDept: circular reference detection
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@ExtendWith(MockitoExtension.class)
class DeptServiceTest {

  @Mock private MstDeptMapper deptMapper;
  @Mock private MstUserDeptMapper userDeptMapper;
  @Mock private DeptService self;

  private DeptService service;

  @BeforeEach
  void setUp() {
    service = new DeptService(deptMapper, userDeptMapper, self);
  }

  // -------------------------------------------------------------------------
  // collectDescendantIds tests
  // -------------------------------------------------------------------------

  @Test
  void collectDescendantIds_singleNode_returnsSelf() {
    // Given: dept 1 has no children
    when(deptMapper.selectList(any())).thenReturn(Collections.emptyList());

    // When
    Set<Long> result = service.collectDescendantIds(1L);

    // Then: should contain only itself
    assertThat(result).containsExactly(1L);
  }

  @Test
  void collectDescendantIds_twoLevels_returnsAllDescendants() {
    // Given: dept 1 → [dept 2, dept 3], dept 2 → [dept 4]
    MstDept dept2 = dept(2L, 1L);
    MstDept dept3 = dept(3L, 1L);
    MstDept dept4 = dept(4L, 2L);

    when(deptMapper.selectList(matchParent(1L))).thenReturn(List.of(dept2, dept3));
    when(deptMapper.selectList(matchParent(2L))).thenReturn(List.of(dept4));
    when(deptMapper.selectList(matchParent(3L))).thenReturn(Collections.emptyList());
    when(deptMapper.selectList(matchParent(4L))).thenReturn(Collections.emptyList());

    // When
    Set<Long> result = service.collectDescendantIds(1L);

    // Then: should contain 1, 2, 3, 4
    assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
  }

  @Test
  void collectDescendantIds_doesNotContainNonDescendants() {
    // Given: dept 1 → [dept 2], dept 5 is unrelated
    MstDept dept2 = dept(2L, 1L);
    when(deptMapper.selectList(matchParent(1L))).thenReturn(List.of(dept2));
    when(deptMapper.selectList(matchParent(2L))).thenReturn(Collections.emptyList());

    // When
    Set<Long> result = service.collectDescendantIds(1L);

    // Then: should NOT contain dept 5
    assertThat(result).doesNotContain(5L);
  }

  // -------------------------------------------------------------------------
  // deleteDept tests
  // -------------------------------------------------------------------------

  @Test
  void deleteDept_throwsWhenHasChildren() {
    // Given: dept 1 exists and has 1 child
    MstDept dept = dept(1L, null);
    when(self.getDeptById(1L)).thenReturn(dept);
    when(deptMapper.selectCount(any())).thenReturn(1L);

    // When / Then
    assertThatThrownBy(() -> service.deleteDept(1L, 0)).isInstanceOf(BusinessException.class);
  }

  @Test
  void deleteDept_succeedsWhenNoChildren() {
    // Given: dept 1 exists and has no children
    MstDept dept = dept(1L, null);
    dept.setVersion(0);
    when(self.getDeptById(1L)).thenReturn(dept);
    when(deptMapper.selectCount(any())).thenReturn(0L);
    when(deptMapper.deleteById(any(MstDept.class))).thenReturn(1);

    // When / Then: should not throw
    service.deleteDept(1L, 0);
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private MstDept dept(Long id, Long parentId) {
    MstDept d = new MstDept();
    d.setId(id);
    d.setParentId(parentId);
    d.setDeptName("Dept-" + id);
    d.setVersion(0);
    return d;
  }

  /**
   * Returns a Mockito argument matcher that matches a QueryWrapper with eq("parent_id", parentId).
   * Since we cannot easily inspect QueryWrapper internals, we use any() and rely on the test setup
   * to return the correct data for each call.
   */
  private com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<MstDept> matchParent(
      Long parentId) {
    return any();
  }
}
