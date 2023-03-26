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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import static ws.gross.gradle.utils.StringUtils.isNullOrEmpty;

public class VersionInfo {
  public static final List<String> RELEASE_TYPES = Stream.of("final", "rc", "beta", "alpha")
      .collect(Collectors.toList());

  @SuppressWarnings("RegExpRepeatedSpace")
  private static final Pattern VERSION_REGEX = Pattern.compile(
      "#base version\n" +
      "(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?\n" +
      "(?:\n" +
      "  # -rc.# and similar\n" +
      "  -(?<type>dev|milestone|alpha|beta|rc)\\.(?<iteration>\\d+)\n" +
      "  # dirty repo merker\n" +
      "  (?:[.-](?<dirty>uncommitted|dirty))?\n" +
      "  # metadata block\n" +
      "  (?:\\+(?<metadata>\n" +
      "    (?:(?<feature>[\\w.]+)\\.)? # branch name\n" +
      "    (?<hash>[a-fA-F0-9]+) # commit hash\n" +
      "  ))?\n" +
      ")?",
      Pattern.COMMENTS
  );

  private final int major;
  private final int minor;
  private final Integer patch;
  private final String significant;
  private final Integer iteration;
  private final boolean dirty;
  private final String metadata;
  private final String feature;
  private final String hash;

  private VersionInfo(
      int major,
      int minor,
      @Nullable Integer patch,
      String significant,
      @Nullable Integer iteration,
      boolean dirty,
      @Nullable String metadata,
      @Nullable String feature,
      @Nullable String hash) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.significant = significant;
    this.iteration = iteration;
    this.dirty = dirty;
    this.metadata = metadata;
    this.feature = feature;
    this.hash = hash;
  }

  public static Optional<VersionInfo> of(String version) {
    Matcher matcher = VERSION_REGEX.matcher(version);
    if (matcher.matches()) {
      return Optional.of(new VersionInfo(
          getIntGroup(matcher, "major"),
          getIntGroup(matcher, "minor"),
          getIntGroupOpt(matcher, "patch").orElse(null),
          isNullOrEmpty(matcher.group("type")) ? "final" : matcher.group("type"),
          getIntGroupOpt(matcher, "iteration").orElse(null),
          !isNullOrEmpty(matcher.group("dirty")),
          matcher.group("metadata"),
          matcher.group("feature"),
          matcher.group("hash")
      ));
    } else {
      return Optional.empty();
    }
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  @Nullable
  public Integer getPatch() {
    return patch;
  }

  public String getSignificant() {
    return significant;
  }

  @Nullable
  public Integer getIteration() {
    return iteration;
  }

  public boolean isDirty() {
    return dirty;
  }

  @Nullable
  public String getMetadata() {
    return metadata;
  }

  @Nullable
  public String getFeature() {
    return feature;
  }

  @Nullable
  public String getHash() {
    return hash;
  }

  public boolean isRelease() {
    return RELEASE_TYPES.contains(significant);
  }

  private static int getIntGroup(Matcher matcher, String group) {
    return getIntGroupOpt(matcher, group)
        .orElseThrow(() -> new NoSuchElementException(group + " version part not found"));
  }

  private static Optional<Integer> getIntGroupOpt(Matcher matcher, String group) {
    return Optional.ofNullable(matcher.group(group))
        .filter(s -> !s.isEmpty())
        .map(Integer::parseInt);
  }
}
