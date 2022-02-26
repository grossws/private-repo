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

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.*
import java.io.File

class BootstrapManifestPluginFunctionalTest {
  private val projectDir = createProjectDir()

  private val manifest: File
    get() = projectDir.resolve("build/manifest.properties")

  @Test
  fun `simple manifest`() {
    val result = createRunner().build()

    assertThat(result)
      .task(":generateBootstrapManifest")
      .isSuccess()

    assertThat(manifest).all {
      exists()
      text().asProperties().all {
        key("version").isEqualTo("1.2")
        key("description").isEqualTo("some description")
        key("pluginIds").isEqualTo("some.plugin")
        key("catalogIds").isEqualTo("catAlias=some.group:module")
      }
    }
  }

  @Test
  fun `caches result`() {
    val first = createRunner().build()
    val second = createRunner().build()

    assertThat(first).task(":generateBootstrapManifest").isSuccess()
    assertThat(second).task(":generateBootstrapManifest").isUpToDate()
  }

  @Test
  fun `use configuration cache`() {
    val runner = createRunner().withConfigurationCache()
    runner.build()
    val result = runner.build()

    assertThat(result).task(":generateBootstrapManifest").isUpToDate()
    assertThat(result).reusedConfigurationCache()
  }

  @Test
  fun `is run on assemble`() {
    val result = createRunner().withArguments("assemble").build()

    assertThat(result).task(":generateBootstrapManifest").isSuccess()
  }

  @BeforeEach
  fun baseProject() {
    projectDir.resolve("settings.gradle.kts").writeText("""
      rootProject.name = "functional-test"

    """.trimIndent())

    projectDir.resolve("build.gradle.kts").writeText("""
      plugins { id("ws.gross.bootstrap-manifest") }

      manifest {
        bootstrapManifest {
          description.set("some description")
          version("1.2")
          plugin("some.plugin")
          catalog("catAlias", "some.group:module")
        }
      }

    """.trimIndent())

    projectDir.resolve("gradle.properties").writeText("""
      group = test.group

    """.trimIndent())
  }

  private fun createRunner(gradleVersion: String? = null) = createRunner(projectDir, gradleVersion)
    .withArguments("generateBootstrapManifest")
}
