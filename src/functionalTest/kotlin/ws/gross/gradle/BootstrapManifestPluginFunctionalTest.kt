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

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.support.appendName
import assertk.assertions.support.expected
import assertk.assertions.support.show
import assertk.assertions.text
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.*
import java.io.File
import java.io.StringReader
import java.util.Properties

class BootstrapManifestPluginFunctionalTest {
  private val projectDir = createProjectDir()

  private val manifest: File
    get() = projectDir.resolve("build/manifest.properties")

  @BeforeEach
  fun init() {
    baseProject()
  }

  @Test
  fun `simple manifest`() {
    val result = createRunner(projectDir).build()

    assertThat(result)
      .task(":generateBootstrapManifest")
      .isSuccess()

    assertThat(manifest).all {
      exists()
      text().asProperties().all {
        key("version").isEqualTo("{strictly [1.2,2.0); reject 1.3}")
        key("description").isEqualTo("some description")
        key("pluginIds").isEqualTo("some.plugin")
        key("catalogIds").isEqualTo("catAlias=some.group:module")
      }
    }
  }

  @Test
  fun `caches result`() {
    val first = createRunner(projectDir).build()
    val second = createRunner(projectDir).build()

    assertThat(first).task(":generateBootstrapManifest").isSuccess()
    assertThat(second).task(":generateBootstrapManifest").isUpToDate()
  }

  @Test
  fun `is run on assemble`() {
    val result = createRunner(projectDir).withArguments("assemble").build()

    assertThat(result).task(":generateBootstrapManifest").isSuccess()
  }

  private fun baseProject() {
    projectDir.resolve("settings.gradle.kts").writeText("""
      rootProject.name = "functional-test"
    """.trimIndent())

    projectDir.resolve("build.gradle.kts").writeText("""
      plugins { id("ws.gross.bootstrap-manifest") }

      manifest {
        bootstrapManifest {
          description.set("some description")
          version {
            strictly("[1.2,2.0)")
            reject("1.3")
          }
          plugin("some.plugin")
          catalog("catAlias", "some.group:module")
        }
      }
    """.trimIndent())

    projectDir.resolve("gradle.properties").writeText("""
      group = test.group
    """.trimIndent())
  }

  private fun createRunner(projectDir: File, gradleVersion: String? = null) = GradleRunner.create()
    .withPluginClasspath()
    .withProjectDir(projectDir)
    .withArguments("generateBootstrapManifest")
    .apply { if (gradleVersion != null) withGradleVersion(gradleVersion) }
}

fun Assert<String>.asProperties() = transform { text ->
  Properties().apply { load(StringReader(text)) }
}

fun Assert<Properties>.key(key: String) = transform(appendName(show(key, "[]"))) {
  if (it.containsKey(key)) {
    it.getProperty(key)
  } else {
    expected("to have key:${show(key)}")
  }
}
