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

import org.gradle.api.Plugin
import org.gradle.api.artifacts.ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion

class NexusPlugin : Plugin<Settings> {
  companion object {
    // decoupling to not fail with obscure message on gradle
    val pluginVersion: String
      get() = NexusPluginImpl.pluginVersion
  }

  override fun apply(settings: Settings) {
    if (GradleVersion.current() < GradleVersion.version("6.8")) {
      throw PluginInstantiationException("Only Gradle 6.8+ supported")
    }

    settings.apply<NexusPluginImpl>()
  }
}

@Suppress("UnstableApiUsage")
internal class NexusPluginImpl : Plugin<Settings> {
  companion object {
    val pluginVersion: String by lazy { NexusPlugin::class.java.`package`.implementationVersion }
    private val logger: Logger = Logging.getLogger(NexusPlugin::class.java)
  }

  private lateinit var conf: NexusConfiguration
  private lateinit var target: Provider<String>

  override fun apply(settings: Settings) {
    conf = NexusConfiguration.from(settings.providers)
    target = settings.providers.gradleProperty("nexusTarget")
      .forUseAtConfigurationTime()
      .orElse("public")

    settings.configurePluginRepos()
    settings.configureRepos()

    settings.bootstrap()
  }

  private fun Settings.configurePluginRepos() {
    pluginManagement.repositories {
      logger.info("Adding gradlePluginPortal to pluginManagement")
      gradlePluginPortal()
      addPrivateGradleRepos()
    }
  }

  private fun Settings.configureRepos() {
    val groups = providers.gradleProperty("nexusGroups")
      .forUseAtConfigurationTime()
      .map { it.parseList() }.orElse(listOf()).get()
    val groupRegexes = providers.gradleProperty("nexusGroupRegexes")
      .forUseAtConfigurationTime()
      .map { it.parseList() }.orElse(conf.defaultGroupRegex).get()

    dependencyResolutionManagement.repositories {
      addMavenCentral(withoutGroups = groups, withoutGroupRegexes = groupRegexes)

      val repo = conf.repoUrl(target)
      val exclusive = providers.gradleProperty("nexusExclusive")
        .forUseAtConfigurationTime()
        .map { it.toBoolean() }.orElse(false).get()

      if (exclusive) {
        logger.info("Adding exclusive $NEXUS_REPO_NAME(${repo.get()} to dependencyResolutionManagement")
        exclusiveContent {
          forRepository {
            maven(NEXUS_REPO_NAME, repo, conf.credentials)
          }

          filter {
            groups.forEach { includeGroup(it) }
            groupRegexes.forEach { includeGroupByRegex(it) }
          }
        }
      } else {
        logger.info("Adding $NEXUS_REPO_NAME(${repo.get()} to dependencyResolutionManagement")
        maven(NEXUS_REPO_NAME, repo, conf.credentials)
      }
    }
  }

  private fun Settings.bootstrap() {
    val bootstrapManifests = providers.gradleProperty("nexusBootstrap")
      .forUseAtConfigurationTime()
      .orElse("").map { it.parseList() }.get()
    val bootstrapCatalogs = providers.gradleProperty("nexusBootstrapCatalogs")
      .forUseAtConfigurationTime()
      .map { it.toBoolean() }.orElse(false).get()

    if (bootstrapManifests.isEmpty()) {
      return
    }

    buildscript.repositories {
      addPrivateGradleRepos()
      maven(BOOTSTRAP_NEXUS_NAME, conf.repoUrl(target), conf.credentials)
    }

    bootstrapManifests.forEach { manifest ->
      val bootstrap = Bootstrap.from(settings, manifest)
      pluginManagement.plugins {
        bootstrap.pluginIds.forEach { id(it) version bootstrap.version }
      }
      if (bootstrapCatalogs) {
        enableFeaturePreview("VERSION_CATALOGS")
        dependencyResolutionManagement.versionCatalogs {
          bootstrap.catalogs.forEach { (alias, dependencyNotation) ->
            create(alias) { from("$dependencyNotation:${bootstrap.version}") }
          }
        }
      }
    }
  }

  private fun RepositoryHandler.addPrivateGradleRepos() {
    val releasesRepo = conf.repoUrl("gradle")
    logger.info("Adding $GRADLE_RELEASES_REPO_NAME(${releasesRepo.get()}) to pluginManagement")
    maven(GRADLE_RELEASES_REPO_NAME, releasesRepo) {
      mavenContent { releasesOnly() }
    }

    val snapshotsRepo = conf.repoUrl("gradle/dev")
    logger.info("Adding $GRADLE_SNAPSHOTS_REPO_NAME(${snapshotsRepo.get()}) to pluginManagement")
    maven(GRADLE_SNAPSHOTS_REPO_NAME, snapshotsRepo)
  }

  private fun RepositoryHandler.addMavenCentral(
    withoutGroups: List<String> = listOf(),
    withoutGroupRegexes: List<String> = listOf()
  ) {
    logger.info("Adding mavenCentral to dependencyResolutionManagement")
    findByName(DEFAULT_MAVEN_CENTRAL_REPO_NAME) ?: mavenCentral {
      content {
        withoutGroups.forEach { excludeGroup(it) }
        withoutGroupRegexes.forEach { excludeGroupByRegex(it) }
      }
    }
  }
}
