package com.lincsoft.util;

import com.lincsoft.constant.CommonConstants;

/**
 * Common utility class for log processing.
 *
 * <p>Functions provided
 *
 * <ul>
 *   <li>truncate: Truncates the given string to the specified maximum length.
 * </ul>
 *
 * @author 林创科技
 * @since 2026-04-08
 */
public final class LogUtil {
  /**
   * Truncates the given string to the specified maximum length.
   *
   * @param str the string to truncate
   * @param maxLen the maximum length
   * @return the truncated string
   */
  public static String truncate(String str, int maxLen) {
    if (str == null) {
      return null;
    }
    if (str.length() <= maxLen) {
      return str;
    }
    return str.substring(0, maxLen) + CommonConstants.TRUNCATE_SUFFIX;
  }
}
