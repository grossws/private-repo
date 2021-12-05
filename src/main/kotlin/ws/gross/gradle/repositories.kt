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

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.credentials.Credentials
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.internal.artifacts.repositories.AuthenticationSupportedInternal
import org.gradle.kotlin.dsl.*
import java.net.URI

const val NEXUS_REPO_NAME = "nexus"
const val BOOTSTRAP_NEXUS_NAME = "bootstrapNexus"
const val RELEASES_REPO_NAME = "nexusReleases"
const val SNAPSHOTS_REPO_NAME = "nexusSnapshots"

data class NexusConfiguration(
  val baseUrl: Provider<String>,
  val credentials: Provider<out Credentials>,
  val defaultGroupRegex: Provider<List<String>>,
  private val providers: ProviderFactory,
) {
  companion object {
    @Suppress("UnstableApiUsage")
    fun from(providers: ProviderFactory): NexusConfiguration {
      val baseUrl = providers.gradleProperty("nexusUrl")
        .forUseAtConfigurationTime()

      val enabled = providers.gradleProperty("nexusDefaultGroupRegex")
        .forUseAtConfigurationTime()
        .map { it.toBoolean() }.orElse(true).get()
      val regex = baseUrl.map {
        URI.create(it).host
          .split('.').asReversed().take(2)
          .joinToString("\\.", postfix = "(\\..*)?")
      }
      val defaultGroupRegex: Provider<List<String>> = regex.map { if (enabled) listOf(it) else listOf() }

      return NexusConfiguration(
        baseUrl = baseUrl,
        credentials = providers.credentials(PasswordCredentials::class, "nexus"),
        defaultGroupRegex = defaultGroupRegex,
        providers = providers,
      )
    }
  }

  fun repoUrl(repoPath: Provider<String>): Provider<String> =
    baseUrl.flatMap { url -> repoPath.map { "$url/repository/$it" } }

  fun repoUrl(repoPath: String): Provider<String> = repoUrl(providers.provider { repoPath })
}

fun RepositoryHandler.maven(
  repoName: String,
  repoUrl: Provider<String>,
  credentials: Provider<out Credentials>? = null,
  configuration: Action<in MavenArtifactRepository> = Action {}
): ArtifactRepository = findByName(repoName) ?: maven {
  name = repoName
  setUrl(repoUrl)
  credentials?.let {
    authentication.create<BasicAuthentication>("basic")
    (this as AuthenticationSupportedInternal).configuredCredentials.set(credentials)
  }
  configuration.execute(this)
}
