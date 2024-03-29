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

import org.gradle.internal.SystemProperties
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.internal.DefaultGradleRunner
import java.io.ByteArrayInputStream
import java.io.File

fun createProjectDir(): File {
  val baseDir = File(SystemProperties.getInstance().workerTmpDir ?: "build/tmp/functionalTest")
  baseDir.mkdirs()
  val projectDir = File.createTempFile("gradle-", "-projectDir", baseDir)
  if (projectDir.exists()) projectDir.deleteRecursively()
  projectDir.mkdirs()
  projectDir.deleteOnExit()
  return projectDir
}

fun createRunner(projectDir: File, gradleVersion: String? = null): GradleRunner = GradleRunner.create()
  .forwardOutput()
  .withPluginClasspath()
  .withProjectDir(projectDir).also {
    if (gradleVersion != null) it.withGradleVersion(gradleVersion)
  }

fun GradleRunner.withConfigurationCache(enable: Boolean = true): GradleRunner =
  withArguments(if (enable) arguments.toMutableList().apply { add("--configuration-cache") } else arguments)

fun GradleRunner.withInteractiveInput(input: String?): GradleRunner = apply {
  input ?: return@apply
  (this as DefaultGradleRunner).withStandardInput(ByteArrayInputStream(input.toByteArray()))
}

fun publishCatalogAndManifest(baseDir: File) {
  baseDir.mkdirs()

  baseDir.resolve("settings.gradle.kts").writeText("""
    rootProject.name = "dummy"

    dependencyResolutionManagement.repositories {
      mavenCentral()
    }
  """.trimIndent())

  baseDir.resolve("gradle.properties").writeText("""
    group = org.example
    version = 1.0
  """.trimIndent())

  baseDir.resolve("build.gradle.kts").writeText("""
    import org.gradle.api.publish.maven.MavenPublication

    plugins {
      `kotlin-dsl`
      `version-catalog`
      id("ws.gross.bootstrap-manifest")
      `maven-publish`
    }

    manifest {
      bootstrapManifest {
        description.set("dummy manifest")
        plugin("org.example.dummy")
        catalog("dummy", "org.example:catalog")
      }
    }

    catalog {
      versionCatalog {
        library("guava", "com.google.guava", "guava").version("31.0.1-jre")
      }
    }

    publishing {
      repositories {
        maven {
          name = "DummyRepo"
          url = uri("../repo")
        }
      }

      publications {
        create<MavenPublication>("catalogMaven") {
          from(components["versionCatalog"])
          artifactId = "catalog"
        }

        named<MavenPublication>("manifestMaven") {
          artifactId = "manifest"
        }
      }
    }
  """.trimIndent())

  baseDir.resolve("src/main/kotlin").apply {
    mkdirs()
    resolve("org.example.dummy.gradle.kts").writeText("""
      plugins { base }
      logger.lifecycle("DUMMY|dummy plugin applied")
    """.trimIndent())
  }

  GradleRunner.create()
    .withProjectDir(baseDir)
    .withArguments("publish")
    .withPluginClasspath()
    .build()
}
