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

package ws.gross.gradle.impl

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.artifacts.ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler.GRADLE_PLUGIN_PORTAL_REPO_NAME
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.*
import ws.gross.gradle.*
import ws.gross.gradle.ProviderUtil.wrappedForUseAtConfigurationTime

@Suppress("UnstableApiUsage")
internal class PrivateRepoPluginImpl : Plugin<Settings> {
  companion object {
    val pluginVersion: String by lazy { PrivateRepoPlugin::class.java.`package`.implementationVersion }
    private val logger: Logger = Logging.getLogger(PrivateRepoPlugin::class.java)
  }

  private lateinit var conf: NexusConfiguration
  private lateinit var repo: Provider<String>

  override fun apply(settings: Settings): Unit = settings.run {
    apply<PrivateRepoBasePlugin>()

    conf = NexusConfiguration.from(providers)
    repo = providers.gradleProperty("nexusRepo")
      .wrappedForUseAtConfigurationTime()
      .orElse("public")

    conf.baseUrl.orNull ?: throw GradleException("nexusUrl should be defined in gradle properties")

    configurePluginRepos()
    configureRepos()

    // apply only after repositories are configured
    apply<BootstrapPlugin>()
  }

  private fun Settings.configurePluginRepos() {
    pluginManagement.repositories {
      removeIf { it.name == GRADLE_PLUGIN_PORTAL_REPO_NAME }

      val repoUrl = conf.repoUrl(repo)
      logger.info("Adding $NEXUS_REPO_NAME(${repoUrl.get()}) to pluginManagement")
      maven(NEXUS_REPO_NAME, repoUrl, conf.credentials)

      logger.info("Adding mavenCentral to pluginManagement")
      mavenCentral()

      logger.info("Adding gradlePluginPortal to pluginManagement")
      gradlePluginPortal()
    }
  }

  private fun Settings.configureRepos() {
    val groups = providers.gradleProperty("nexusGroups")
      .wrappedForUseAtConfigurationTime()
      .map { it.parseList() }.orElse(listOf()).get()
    val groupRegexes = providers.gradleProperty("nexusGroupRegexes")
      .wrappedForUseAtConfigurationTime()
      .map { it.parseList() }.orElse(conf.defaultGroupRegex).get()

    dependencyResolutionManagement.repositories {
      logger.info("Adding mavenCentral to dependencyResolutionManagement")
      addMavenCentral(withoutGroups = groups, withoutGroupRegexes = groupRegexes)

      val repoUrl = conf.repoUrl(repo)
      val exclusive = providers.gradleProperty("nexusExclusive")
        .wrappedForUseAtConfigurationTime()
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
