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

val versionRegex = """
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
  """.toRegex(RegexOption.COMMENTS)

fun String.parseVersionInfo(): VersionInfo? = versionRegex.matchEntire(this)?.groups?.let { groups ->
  VersionInfo(
    major = groups[1]!!.value.toInt(),
    minor = groups[2]!!.value.toInt(),
    patch = groups[3]!!.value.toInt(),
    significant = groups[4]?.value ?: "final",
    iteration = groups[5]?.value?.toInt(),
    dirty = groups[6] != null,
    metadata = groups[7]?.value,
    feature = groups[8]?.value,
    hash = groups[9]?.value
  )
}

data class VersionInfo(
  val major: Int, val minor: Int, val patch: Int?,
  val significant: String, val iteration: Int?,
  val dirty: Boolean,
  val metadata: String?, val feature: String?, val hash: String?
) {
  val release: Boolean
    get() = significant == "final"
}
