/*
 * Copyright 2023 Konstantin Gribov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ws.gross.gradle.utils;

import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class StringUtils {
  private static final Pattern TO_CAMEL_CASE_PATTERN = Pattern.compile("[._-]");

  public static String toUpperCamelCase(String value) {
    return toCamelCase(value, true);
  }

  public static String toLowerCamelCase(String value) {
    return toCamelCase(value, false);
  }

  public static String capitalize(@Nullable String value) {
    if (isNullOrEmpty(value)) {
      return "";
    }
    return Character.isUpperCase(value.charAt(0)) ? value
        : Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }

  public static String decapitalize(@Nullable String value) {
    if (isNullOrEmpty(value)) {
      return "";
    }
    return Character.isLowerCase(value.charAt(0)) ? value
        : Character.toLowerCase(value.charAt(0)) + value.substring(1);
  }

  public static boolean isNullOrEmpty(@Nullable String value) {
    return value == null || value.isEmpty();
  }

  public static String nullToEmpty(@Nullable String value) {
    return value == null ? "" : value;
  }

  @Nullable
  public static String emptyToNull(@Nullable String value) {
    return value != null && value.isEmpty() ? null : value;
  }

  private static String toCamelCase(String value, boolean firstUpper) {
    String result = TO_CAMEL_CASE_PATTERN.splitAsStream(value)
        .map(p -> capitalize(p.toLowerCase(Locale.ROOT)))
        .collect(Collectors.joining(""));
    return firstUpper ? result : capitalize(result);
  }
}
