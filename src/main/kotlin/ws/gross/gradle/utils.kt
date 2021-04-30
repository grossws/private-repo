/*
 * Copyright 2021 Konstantin Gribov
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

import org.gradle.api.Project
import java.util.regex.Pattern

inline fun Project.ifNotDslAccessors(block: () -> Unit) {
  if (project.name != "gradle-kotlin-dsl-accessors") block.invoke()
}

internal fun String?.parseList(): List<String> =
  (this ?: "").split(',').map { it.trim() }.filterNot { it.isEmpty() }

internal fun String.parsePair(): Pair<String, String> =
  split('=', limit = 2).map { it.trim() }.let { it[0] to it[1] }

internal fun String?.parseMap(): Map<String, String> =
  parseList().associate { it.parsePair() }

val publishTaskNameRegex = """publish([A-Z][\w.]*)PublicationTo([A-Z]\w*)Repository""".toRegex()

fun String.parsePublishTaskInfo(): PublishTaskInfo? =
  publishTaskNameRegex.matchEntire(this)?.destructured?.let { (p, r) ->
    PublishTaskInfo(publication = p.decapitalize(), repository = r.decapitalize())
  }

data class PublishTaskInfo(val publication: String, val repository: String)

@Suppress("RegExpRepeatedSpace")
val versionRegex: Pattern = Pattern.compile("""
  # base version
  (?<major>\d+)\.(?<minor>\d+)\.(?<patch>\d+)
  (?:
    # -rc.# and similar
    -(?<type>dev|milestone|rc)\.(?<iteration>\d+)
    # dirty repo marker
    (?:\.(?<dirty>uncommitted))?
    # metadata block
    (?:\+(?<metadata>
      (?:(?<feature>[\w.]+)\.)? # branch name
      (?<hash>[a-fA-F0-9]+) # commit hash
    ))?
  )?
  """, Pattern.COMMENTS)

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

data class VersionInfo(
  val major: Int, val minor: Int, val patch: Int?,
  val significant: String, val iteration: Int?,
  val dirty: Boolean,
  val metadata: String?, val feature: String?, val hash: String?
) {
  val release: Boolean
    get() = significant in listOf("final", "rc")
}
