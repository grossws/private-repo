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

plugins {
  id("plugin-conventions")
  kotlin("jvm") version embeddedKotlinVersion
}

@Suppress("UnstableApiUsage")
gradlePlugin {
  website.set("https://github.com/grossws/private-repo")
  vcsUrl.set("https://github.com/grossws/private-repo.git")

  plugins.create("privateRepo") {
    id = "ws.gross.private-repo"
    displayName = "Settings plugin for private repository configuration and bootstrapping"
    description = """
      Gradle Settings plugin to configure dependencyResolutionManagement and pluginManagement
      to use private Nexus/Artifactory repository with auth and convenient defaults.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.PrivateRepoPlugin"
    tags.set(listOf("repository", "private-repository", "nexus", "artifactory"))
  }

  plugins.create("privateRepoBase") {
    id = "ws.gross.private-repo.base"
    displayName = "Settings helper plugin to register privateRepo extension"
    description = """
      Gradle Settings plugin to register privateRepo extension. 
      Used by other settings plugins.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.PrivateRepoBasePlugin"
    tags.set(listOf("repository", "private-repository", "nexus", "artifactory"))
  }

  plugins.create("privateRepoBootstrap") {
    id = "ws.gross.private-repo.bootstrap"
    displayName = "Settings plugin to apply bootstrap manifests"
    description = """
      Gradle Settings plugin to apply bootstrap manifests for this build.
      Resolves and parses manifest, adds plugin declaration to pluginManagement
      and version catalogs to dependencyResolutionManagement.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.BootstrapPlugin"
    tags.set(listOf("repository", "private-repository", "bootstrap"))
  }

  plugins.create("privateRepoPublish") {
    id = "ws.gross.private-repo-publish"
    displayName = "Plugin for private repository publication configuration"
    description = """
      Gradle plugin to configure `maven-publish` plugin with sane default repositories
      to use with private Nexus/Artifactory authenticated repo.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.PrivateRepoPublishPlugin"
    tags.set(listOf("repository", "private-repository", "nexus", "artifactory", "publish", "maven-publish"))
  }

  plugins.create("bootstrapManifest") {
    id = "ws.gross.bootstrap-manifest"
    displayName = "Plugin to generate and publish bootstrap manifest to configure plugins and version catalogs"
    description = """
      Gradle plugin to generate bootstrap manifest files (properties file with pluginIds, catalogIds 
      and version fields) to use with nexusBootstrap feature in private-repo plugin.
      Also configures manifest publication.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.BootstrapManifestPlugin"
    tags.set(listOf("bootstrap", "version-catalogs", "plugins", "private-repository"))
  }

  plugins.create("releaseApprove") {
    id = "ws.gross.release-approve"
    displayName = "Plugin to add approve task for `nebula.release` plugin rc/final release tasks"
    description = """
      Gradle plugin which adds approve for rc/final tasks when `nebula.release` plugin present.
      Use `-Prelease.approve=true` in non-interactive context.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.ReleaseApprovePlugin"
    tags.set(listOf("release", "git", "nebula-release"))
  }
}

@Suppress("UnstableApiUsage")
testing {
  suites {
    named<JvmTestSuite>("test") {
      dependencies {
        implementation(project.dependencies.platform(project.dependencies.kotlin("bom", embeddedKotlinVersion)))
        implementation(project.dependencies.create(project.dependencies.kotlin("stdlib-jdk8", embeddedKotlinVersion)))
        implementation(project.dependencies.plugin(libs.plugins.nebula.release))
      }
    }

    withType<JvmTestSuite> {
      useJUnitJupiter(libs.versions.junit.get())

      dependencies {
        implementation(project.dependencies.platform(project.dependencies.kotlin("bom", embeddedKotlinVersion)))
        implementation(project.dependencies.create(project.dependencies.kotlin("stdlib-jdk8", embeddedKotlinVersion)))
        implementation(libs.assertk.jvm)
        implementation(libs.kgit)
        implementation(project.dependencies.platform(libs.jackson.bom))
        implementation(libs.jackson.databind)
        implementation(libs.jackson.kotlin)
        implementation(libs.jackson.guava)
      }
    }
  }
}
