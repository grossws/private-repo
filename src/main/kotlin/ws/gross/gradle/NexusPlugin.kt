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
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
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

    private val logger: Logger = Logging.getLogger(NexusPlugin::class.java)
  }

  private lateinit var rh: RepoHelper

  override fun apply(settings: Settings) {
    val nexusUrl: String? by settings
    rh = RepoHelper(nexusUrl ?: throw PluginInstantiationException("nexusUrl should be defined in gradle properties"))
    logger.info("Configuring private repo with nexusUrl $nexusUrl")

    settings.configurePluginRepos()
    settings.configureRepos()

    settings.addPrivatePluginsBootstrap()
    settings.addNexusPublishPlugin()
  }

  private fun Settings.configurePluginRepos() {
    pluginManagement.repositories {
      logger.info("Adding gradlePluginPortal to pluginManagement")
      gradlePluginPortal()

      logger.info("Adding $GRADLE_RELEASES_REPO_NAME(${rh.repoUrl("gradle")}) to pluginManagement")
      findOrCreateNexusRepo(rh, GRADLE_RELEASES_REPO_NAME, "gradle") {
        mavenContent { releasesOnly() }
      }

      logger.info("Adding $GRADLE_SNAPSHOTS_REPO_NAME(${rh.repoUrl("gradle/dev")}) to pluginManagement")
      findOrCreateNexusRepo(rh, GRADLE_SNAPSHOTS_REPO_NAME, "gradle/dev")
    }
  }

  @Suppress("UnstableApiUsage")
  private fun Settings.configureRepos() {
    val nexusGroups: String? by settings
    val groups = nexusGroups.parseList()

    val nexusGroupRegexes: String? by settings
    val regexes = nexusGroupRegexes.parseList()

    val nexusDefaultGroupRegex: String? by this
    val defaultRegex = if (nexusDefaultGroupRegex.parseSwitch(true)) {
      val url = URI.create(rh.baseUrl)
      val prefix = url.host.split('.').asReversed().take(2).joinToString(".")
      prefix.replace(".", "\\.") + "(\\..*)?"
    } else {
      null
    }

    logger.info("Adding mavenCentral to dependencyResolutionManagement")
    dependencyResolutionManagement.repositories.mavenCentral {
      content {
        groups.forEach { excludeGroup(it) }
        regexes.forEach { excludeGroupByRegex(it) }
        if (defaultRegex != null && groups.isEmpty() && regexes.isEmpty()) {
          excludeGroupByRegex(defaultRegex)
        }
      }
    }

    val nexusTarget: String? by settings
    val nexusExclusive: String? by settings
    val exclusive = nexusExclusive.parseSwitch(false)
    configureNexusRepo(nexusTarget ?: "public", groups, regexes, defaultRegex, exclusive)
  }

  private fun Settings.addNexusPublishPlugin() {
    val version = NexusPlugin::class.java.`package`.implementationVersion
    pluginManagement.plugins {
      id("ws.gross.private-repo-publish") version (version)
    }
  }

  private fun Settings.addPrivatePluginsBootstrap() {
    val nexusUsername: String by settings
    val nexusPassword: String by settings
    val httpClient = HttpClient.newBuilder()
      .authenticator(object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
          logger.info("requested password auth")
          return PasswordAuthentication(nexusUsername, nexusPassword.toCharArray())
        }
      })
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build()

    val nexusPrivatePluginsBootstrap: String? by settings
    nexusPrivatePluginsBootstrap ?: return
    val bootstrapPlugins = nexusPrivatePluginsBootstrap!!.split(',').map { it.trim() }
    logger.info("Add bootstrap plugins ${bootstrapPlugins.joinToString(", ")}")

    for (bootstrapPlugin in bootstrapPlugins) {
      val repoId = bootstrapPlugin.substringBeforeLast('/', "")
      val components = bootstrapPlugin.substringAfterLast('/').split(':')
      require(components.size == 3) { "bootstrap plugins should be in group:module:version notation" }
      val (group, module, version) = components

      val cached = Paths.get("$settingsDir/.gradle/${group}.${module}-$version.properties")
      val metaUrl = URI.create("${rh.repoUrl(repoId.ifEmpty { "gradle" })}/${metaMavenPath(group, module, version)}")

      val bootstrap = Bootstrap.parse(cached) ?: httpClient.fetchMeta(metaUrl, cached)
      if (bootstrap == null) {
        logger.warn("Failed to load nexusPrivatePluginsBootstrap meta from $metaUrl")
        return
      }

      pluginManagement.plugins {
        bootstrap.ids.forEach { id(it) version (bootstrap.version) }
      }
    }
  }

  private fun HttpClient.fetchMeta(metaUrl: URI, destinationPath: Path): Bootstrap? {
    val req = HttpRequest.newBuilder(metaUrl).GET().build()
    val resp = send(req, HttpResponse.BodyHandlers.ofFile(destinationPath))
    logger.debug("Fetching bootstrap meta from $metaUrl : got ${resp.statusCode()}")

    if (resp.statusCode() != 200) {
      throw GradleException("Failed to download ${resp.uri()}: $resp")
    }

    return Bootstrap.parse(destinationPath)
  }

  private fun metaMavenPath(group: String, module: String, version: String) =
    "${group.replace('.', '/')}/$module/$version/$module-$version.properties"

  data class Bootstrap(val ids: List<String>, val version: String) {
    companion object {
      private val logger: Logger = Logging.getLogger(Bootstrap::class.java)

      fun parse(path: Path?): Bootstrap? {
        path ?: return null
        if (!Files.exists(path)) return null

        val props = Properties().apply { path.toFile().reader().use { load(it) } }
        val ids = props.getProperty("ids").split(',').map { it.trim() }
        val version = props.getProperty("version")

        if (ids.isEmpty() || version.isEmpty()) {
          logger.info("nexusPrivatePluginsBootstrap cache invalid")
          return null
        }
        return Bootstrap(ids, version)
      }
    }
  }

  @Suppress("UnstableApiUsage")
  private fun Settings.configureNexusRepo(
    repoPath: String,
    groups: List<String>,
    regexes: List<String>,
    defaultRegex: String?,
    exclusive: Boolean,
  ) = dependencyResolutionManagement.repositories {
    if (exclusive) {
      logger.info("Adding exclusive $NEXUS_REPO_NAME(${rh.repoUrl(repoPath)}) to dependencyResolutionManagement")
      exclusiveContent {
        forRepository {
          findOrCreateNexusRepo(rh, NEXUS_REPO_NAME, repoPath, MavenArtifactRepository::withAuth)
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
      logger.info("Adding $NEXUS_REPO_NAME(${rh.repoUrl(repoPath)}) to dependencyResolutionManagement")
      findOrCreateNexusRepo(rh, NEXUS_REPO_NAME, repoPath, MavenArtifactRepository::withAuth)
    }
  }
}
