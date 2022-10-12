/*
 * Copyright 2022 Konstantin Gribov
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

package ws.gross.gradle

import java.util.regex.Pattern

data class VersionInfo(
  val major: Int, val minor: Int, val patch: Int?,
  val significant: String, val iteration: Int?,
  val dirty: Boolean,
  val metadata: String?, val feature: String?, val hash: String?
) {
  companion object {
    val RELEASE_TYPES = listOf("final", "rc", "beta", "alpha")
  }

  val release: Boolean
    get() = significant in RELEASE_TYPES
}

fun String.parseVersionInfo(): VersionInfo? = versionRegex.matcher(this).let { if (it.matches()) it else null }
  ?.run {
    VersionInfo(
      major = group("major").toInt(),
      minor = group("minor").toInt(),
      patch = group("patch").toInt(),
      significant = group("type") ?: "final",
      iteration = group("iteration")?.toInt(),
      dirty = group("dirty") != null,
      metadata = group("metadata"),
      feature = group("feature"),
      hash = group("hash")
    )
  }

@Suppress("RegExpRepeatedSpace")
private val versionRegex: Pattern = Pattern.compile("""
  # base version
  (?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)
  (?:
    # -rc.# and similar
    -(?<type>dev|milestone|alpha|beta|rc)\.(?<iteration>\d+)
    # dirty repo marker
    (?:\.(?<dirty>uncommitted))?
    # metadata block
    (?:\+(?<metadata>
      (?:(?<feature>[\w.]+)\.)? # branch name
      (?<hash>[a-fA-F0-9]+) # commit hash
    ))?
  )?
  """, Pattern.COMMENTS)
