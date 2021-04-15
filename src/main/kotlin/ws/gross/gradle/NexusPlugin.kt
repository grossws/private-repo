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

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Properties

class NexusPlugin : Plugin<Settings> {
  override fun apply(settings: Settings) {
    if (GradleVersion.current() < GradleVersion.version("6.8")) {
      throw PluginInstantiationException("Only Gradle 6.8+ supported")
    }

    settings.apply<NexusPluginImpl>()
  }
}

internal class NexusPluginImpl : Plugin<Settings> {
  companion object {
    const val NEXUS_REPO_NAME = "nexus"
    const val GRADLE_RELEASES_REPO_NAME = "nexusReleasesGradle"
    const val GRADLE_SNAPSHOTS_REPO_NAME = "nexusSnapshotsGradle"

    val logger = Logging.getLogger(NexusPlugin::class.java)
  }

  lateinit var baseUrl: String

  override fun apply(settings: Settings) {
    val nexusUrl: String? by settings
    baseUrl = nexusUrl ?: throw PluginInstantiationException("nexusUrl should be defined in gradle properties")

    settings.configurePluginRepos()
    settings.configureRepos()

    settings.addPrivatePluginsBootstrap()
  }

  private fun Settings.configurePluginRepos() {
    pluginManagement.repositories {
      gradlePluginPortal()

      findOrCreateNexusRepo(baseUrl, GRADLE_RELEASES_REPO_NAME, "gradle") {
        mavenContent { releasesOnly() }
      }

      findOrCreateNexusRepo(baseUrl, GRADLE_SNAPSHOTS_REPO_NAME, "gradle/dev") {
        mavenContent { snapshotsOnly() }
      }
    }
  }

  @Suppress("UnstableApiUsage")
  private fun Settings.configureRepos() {
    val nexusGroups: String? by this
    val groups = nexusGroups.parseList()

    val nexusGroupRegexes: String? by this
    val regexes = nexusGroupRegexes.parseList()

    val nexusDefaultGroupRegex: String? by this
    val defaultRegex = if (nexusDefaultGroupRegex.parseSwitch(true)) {
      val url = URI.create(baseUrl)
      val prefix = url.host.split('.').asReversed().take(2).joinToString(".")
      prefix.replace(".", "\\.") + "(\\..*)?"
    } else {
      null
    }

    dependencyResolutionManagement.repositories.mavenCentral {
      content {
        groups.forEach { excludeGroup(it) }
        regexes.forEach { excludeGroupByRegex(it) }
        if (defaultRegex != null && groups.isEmpty() && regexes.isEmpty()) {
          excludeGroupByRegex(defaultRegex)
        }
      }
    }

    val nexusTarget: String? by this
    val nexusExclusive: String? by this
    val exclusive = nexusExclusive.parseSwitch(false)
    configureNexusRepo(nexusTarget ?: "public", groups, regexes, defaultRegex, exclusive)
  }

  private fun Settings.addPrivatePluginsBootstrap() {
    val nexusPrivatePluginsBootstrap: String? by settings
    nexusPrivatePluginsBootstrap ?: return

    val components = nexusPrivatePluginsBootstrap!!.split(':')
    require(components.size == 3) { "nexusPrivatePluginsBootstrap should be in group:module:version notation" }
    val (group, module, version) = components

    val httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()

    val cached = File("$settingsDir/.gradle/${group}.${module}-$version.properties")
    if (!cached.exists()) {
      val path = "${group.replace('.', '/')}/$module/$version/$module-$version.properties"
      val req = HttpRequest.newBuilder(URI.create("$baseUrl/repository/gradle/$path")).GET().build()

      val resp = httpClient.send(req, HttpResponse.BodyHandlers.ofFile(cached.toPath()))

      if (resp.statusCode() != 200) {
        throw GradleException("Failed to download ${resp.uri()}: $resp")
      }
    }

    if (cached.exists()) {
      val props = Properties().apply { cached.reader().use { load(it) } }
      val ids = props.getProperty("ids").split(',').map { it.trim() }
      val version = props.getProperty("version")

      pluginManagement.plugins {
        ids.forEach { id(it) version (version) }
      }
    }
  }

  @Suppress("UnstableApiUsage")
  private fun Settings.configureNexusRepo(
    repoId: String,
    groups: List<String>,
    regexes: List<String>,
    defaultRegex: String?,
    exclusive: Boolean,
  ) {
    dependencyResolutionManagement.repositories {
      if (exclusive) {
        exclusiveContent {
          forRepository {
            findOrCreateNexusRepo(baseUrl, NEXUS_REPO_NAME, repoId) {
              withAuth()
            }
          }

          filter {
            groups.forEach { includeGroup(it) }
            regexes.forEach { includeGroupByRegex(it) }
            if (defaultRegex != null && groups.isEmpty() && regexes.isEmpty()) {
              includeGroupByRegex(defaultRegex)
            }
          }
        }
      } else {
        findOrCreateNexusRepo(baseUrl, NEXUS_REPO_NAME, repoId) {
          withAuth()
        }
      }
    }
  }
}
