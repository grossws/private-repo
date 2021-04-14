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

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.*
import java.net.URI

fun RepositoryHandler.findOrCreateNexusRepo(
  baseUrl: String?,
  repoName: String,
  repoPath: String,
  configuration: MavenArtifactRepository.() -> Unit
): ArtifactRepository = findByName(repoName) ?: maven {
  name = repoName
  url = URI.create("$baseUrl/repository/$repoPath")
  configuration()
}

fun MavenArtifactRepository.withAuth() {
  credentials(PasswordCredentials::class.java)
  authentication.create<BasicAuthentication>("basic")
}

internal fun String?.parseList(): List<String> =
  (this ?: "").split(',').map { it.trim() }.filterNot { it.isEmpty() }

internal fun String?.parseSwitch(defaultValue: Boolean): Boolean =
  when ((this ?: defaultValue.toString()).trim().toLowerCase()) {
    "true" -> true
    "on" -> true
    "false" -> false
    "off" -> false
    else -> {
      throw IllegalArgumentException("can't parse $this to boolean: use true/false or on/off")
    }
  }

