package ws.gross.gradle

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import java.net.URI

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
  }

  lateinit var baseUrl: String

  override fun apply(settings: Settings) {
    val nexusUrl: String? by settings
    baseUrl = nexusUrl ?: throw PluginInstantiationException("nexusUrl should be defined in gradle properties")

    settings.configurePluginRepos()
    settings.configureRepos()
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
