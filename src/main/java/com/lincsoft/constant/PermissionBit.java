package com.lincsoft.constant;

import lombok.Getter;

/**
 * Permission bit enumeration for data permission control.
 *
 * <p>Each permission is represented as a power-of-two bitmask so that multiple permissions can be
 * combined via bitwise OR and tested via bitwise AND.
 *
 * <p>Bitmask layout:
 *
 * <pre>
 *   READ   = 0001 (1)
 *   WRITE  = 0010 (2)
 *   DELETE = 0100 (4)
 *   EXPORT = 1000 (8)
 * </pre>
 *
 * @author 林创科技
 * @since 2026-04-15
 */
@Getter
public enum PermissionBit {
  READ(1),
  WRITE(2),
  DELETE(4),
  EXPORT(8);

  /** The integer bitmask value for this permission. */
  private final int bit;

  PermissionBit(int bit) {
    this.bit = bit;
  }

  /**
   * Check whether the given bitmask includes this permission.
   *
   * @param permBits the combined permission bitmask to test
   * @return {@code true} if this permission bit is set in {@code permBits}
   */
  public boolean isGranted(int permBits) {
    return (permBits & this.bit) != 0;
  }
}
