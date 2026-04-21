package com.lincsoft.dto;

import java.io.Serial;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * A cacheable implementation of {@link UserDetails} for Redis serialization.
 *
 * <p>Unlike {@link org.springframework.security.core.userdetails.User}, this class provides a
 * no-arg constructor required by Jackson JSON deserialization. Only essential fields (username,
 * password, authorities) are stored; account status flags always return {@code true} since business
 * logic handles those checks separately (e.g., lock status via Redis, inactive status via
 * exception).
 *
 * @author 林创科技
 * @since 2026-04-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheableUserDetails implements UserDetails {

  @Serial private static final long serialVersionUID = 1L;

  private String username;

  private String password;

  private List<SimpleGrantedAuthority> authorities;
}
