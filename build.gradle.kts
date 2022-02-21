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
}

gradlePlugin {
  plugins.create("private-repo") {
    id = "ws.gross.private-repo"
    displayName = "Settings plugin for private repository configuration and bootstrapping"
    description = """
      Gradle Settings plugin to configure dependencyResolutionManagement and pluginManagement
      to use private Nexus/Artifactory repository with auth and convenient defaults.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.PrivateRepoPlugin"
  }

  plugins.create("private-repo-base") {
    id = "ws.gross.private-repo.base"
    displayName = "Settings helper plugin to register privateRepo extension"
    description = """
      Gradle Settings plugin to register privateRepo extension. 
      Used by other settings plugins.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.PrivateRepoBasePlugin"
  }

  plugins.create("private-repo-bootstrap") {
    id = "ws.gross.private-repo.bootstrap"
    displayName = "Settings plugin to apply bootstrap manifests"
    description = """
      Gradle Settings plugin to apply bootstrap manifests for this build.
      Resolves and parses manifest, adds plugin declaration to pluginManagement
      and version catalogs to dependencyResolutionManagement.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.BootstrapPlugin"
  }

  plugins.create("private-repo-publish") {
    id = "ws.gross.private-repo-publish"
    displayName = "Plugin for private repository publication configuration"
    description = """
      Gradle plugin to configure `maven-publish` plugin with sane default repositories
      to use with private Nexus/Artifactory authenticated repo.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.PrivateRepoPublishPlugin"
  }

  plugins.create("bootstrap-manifest") {
    id = "ws.gross.bootstrap-manifest"
    displayName = "Plugin to generate and publish bootstrap manifest to configure plugins and version catalogs"
    description = """
      Gradle plugin to generate bootstrap manifest files (properties file with pluginIds, catalogIds 
      and version fields) to use with nexusBootstrap feature in private-repo plugin.
      Also configures manifest publication.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.BootstrapManifestPlugin"
  }

  plugins.create("release-approve") {
    id = "ws.gross.release-approve"
    displayName = "Plugin to add approve task for `nebula.release` plugin rc/final release tasks"
    description = """
      Gradle plugin which adds approve for rc/final tasks when `nebula.release` plugin present.
      Use `-Prelease.approve=true` in non-interactive context.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.ReleaseApprovePlugin"
  }
}

pluginBundle {
  website = "https://github.com/grossws/private-repo"
  vcsUrl = "https://github.com/grossws/private-repo.git"
  tags = listOf("repository", "private-repository", "nexus", "artifactory")
}

dependencies {
  testImplementation(platform("com.fasterxml.jackson:jackson-bom:2.12.2"))
  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
  testImplementation("com.github.sya-ri:kgit:1.0.5")
  testImplementation("nebula.release:nebula.release.gradle.plugin:16.0.0")
}
