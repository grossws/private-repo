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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.artifacts.ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.internal.FeaturePreviews.Feature
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
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
  private lateinit var repo: Provider<String>

  override fun apply(settings: Settings) {
    conf = NexusConfiguration.from(settings.providers)
    repo = settings.providers.gradleProperty("nexusRepo")
      .forUseAtConfigurationTime()
      .orElse("public")

    conf.baseUrl.orNull ?: throw GradleException("nexusUrl should be defined in gradle properties")

    settings.configurePluginRepos()
    settings.configureRepos()

    settings.bootstrap()
  }

  private fun Settings.configurePluginRepos() {
    pluginManagement.repositories {
      logger.info("Adding gradlePluginPortal to pluginManagement")
      gradlePluginPortal()

      val repoUrl = conf.repoUrl(repo)
      logger.info("Adding $NEXUS_REPO_NAME(${repoUrl.get()}) to pluginManagement")
      maven(NEXUS_REPO_NAME, repoUrl, conf.credentials)
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
      logger.info("Adding mavenCentral to dependencyResolutionManagement")
      addMavenCentral(withoutGroups = groups, withoutGroupRegexes = groupRegexes)

      val repoUrl = conf.repoUrl(repo)
      val exclusive = providers.gradleProperty("nexusExclusive")
        .forUseAtConfigurationTime()
        .map { it.toBoolean() }.orElse(false).get()

      if (exclusive) {
        logger.info("Adding exclusive $NEXUS_REPO_NAME(${repoUrl.get()} to dependencyResolutionManagement")
        exclusiveContent {
          forRepository {
            maven(NEXUS_REPO_NAME, repoUrl, conf.credentials)
          }

          filter {
            groups.forEach { includeGroup(it) }
            groupRegexes.forEach { includeGroupByRegex(it) }
          }
        }
      } else {
        logger.info("Adding $NEXUS_REPO_NAME(${repoUrl.get()} to dependencyResolutionManagement")
        maven(NEXUS_REPO_NAME, repoUrl, conf.credentials)
      }
    }
  }

  private fun Settings.bootstrap() {
    val bootstrapManifests = providers.gradleProperty("nexusBootstrap")
      .forUseAtConfigurationTime()
      .orElse("").map { it.parseList() }.get()
    if (bootstrapManifests.isEmpty()) {
      return
    }

    val bootstrapCatalogs = providers.gradleProperty("nexusBootstrapCatalogs")
      .forUseAtConfigurationTime()
      .map { it.toBoolean() }.orElse(false).get()
    if (bootstrapCatalogs) {
      logger.warn("""
        nexusBootstrapCatalogs property is deprecated:
          use enableFeaturePreview(\"VERSION_CATALOGS\") in settings.gradle.kts
          to add catalogs from manifests automatically.
      """.trimIndent())
    }

    buildscript.repositories {
      val repoUrl = conf.repoUrl(repo)
      logger.info("Adding $NEXUS_REPO_NAME(${repoUrl.get()}) to buildscript resolve bootstrap manifests")
      maven(BOOTSTRAP_NEXUS_NAME, repoUrl, conf.credentials)
    }

    gradle.rootProject {
      extensions.create("bootstrapManifests", ManifestsExtension::class)
    }

    bootstrapManifests.forEach { manifest ->
      val bootstrap = Bootstrap.from(settings, manifest)

      pluginManagement.plugins {
        bootstrap.pluginIds.forEach { id(it) version bootstrap.version }
      }

      gradle.settingsEvaluated(AutoBootstrapCatalogsAction(bootstrap))

      gradle.rootProject {
        logger.info("Adding bootstrap manifest $manifest to bootstrapManifests extension in rootProject")
        the<ManifestsExtension>().manifests.put(manifest.substringBeforeLast(':'), bootstrap)
      }
    }
  }

  private fun RepositoryHandler.addMavenCentral(
    withoutGroups: List<String> = listOf(),
    withoutGroupRegexes: List<String> = listOf()
  ) {
    findByName(DEFAULT_MAVEN_CENTRAL_REPO_NAME) ?: mavenCentral {
      content {
        withoutGroups.forEach { excludeGroup(it) }
        withoutGroupRegexes.forEach { excludeGroupByRegex(it) }
      }
    }
  }
}

abstract class ManifestsExtension {
  abstract val manifests: MapProperty<String, Bootstrap>
}

@Suppress("UnstableApiUsage")
internal class AutoBootstrapCatalogsAction(private val bootstrap: Bootstrap) : Action<Settings> {
  override fun execute(settings: Settings): Unit = settings.run {
    val features = serviceOf<FeaturePreviews>()
    if (Feature.VERSION_CATALOGS.isActive && !features.isFeatureEnabled(Feature.VERSION_CATALOGS)) {
      // VERSION_CATALOGS is still feature preview but not enabled
      return@run
    }

    dependencyResolutionManagement.versionCatalogs {
      bootstrap.catalogs.forEach { (alias, dependencyNotation) ->
        create(alias) { from("$dependencyNotation:${bootstrap.version}") }
      }
    }
  }
}
