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

package ws.gross.gradle

import assertk.assertThat
import assertk.assertions.any
import assertk.assertions.contains
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class GradleUtilsFunctionalTest {
  companion object {
    private const val d = "$"
  }

  private val projectDir = createProjectDir()

  @ParameterizedTest
  @CsvSource(value = [
    "6.8.3,false",
    "7.0,true",
    "7.3.2,true",
    "7.4,true"
  ])
  fun `isVersionCatalogsSupported returns correct value`(gradle: String, expected: Boolean) {
    projectDir.resolve("settings.gradle.kts").appendText("""
      logger.lifecycle("version catalogs supported: $d{isVersionCatalogsSupported()}")
    """.trimIndent())

    val result = createRunner(gradle).build()

    assertThat(result).output().any { it.contains("version catalogs supported: $expected") }
  }

  @ParameterizedTest
  @CsvSource(value = [
    "7.0,true",
    "7.3.2,true",
    "7.4,false"
  ])
  fun `isVersionCatalogExperimental returns correct value`(gradle: String, expected: Boolean) {
    projectDir.resolve("settings.gradle.kts").appendText("""
      logger.lifecycle("version catalogs experimental: $d{isVersionCatalogsExperimental()}")
    """.trimIndent())

    val result = createRunner(gradle).build()

    assertThat(result).output().any { it.contains("version catalogs experimental: $expected") }
  }

  @Test
  fun `isVersionCatalogsExperimental throws exception before gradle 7`() {
    projectDir.resolve("settings.gradle.kts").appendText("""
      val experimental = isVersionCatalogsExperimental()
    """.trimIndent())

    val result = createRunner("6.8.3").buildAndFail()

    assertThat(result).output().any { it.contains("version catalogs not supported", ignoreCase = true) }
  }

  @ParameterizedTest
  @CsvSource(value = [
    "6.8.3,false,false",
    "7.3.2,false,false",
    "7.3.2,true,true",
    "7.4,false,true"
  ])
  fun `isVersionCatalogsEnabled returns correct value`(
    gradle: String,
    enableFeature: Boolean,
    expected: Boolean
  ) {
    projectDir.resolve("settings.gradle.kts").appendText("""
      ${if (enableFeature) "enableFeaturePreview(\"VERSION_CATALOGS\")" else ""}
      logger.lifecycle("version catalogs enabled: $d{isVersionCatalogsEnabled()}")
    """.trimIndent())

    val result = createRunner(gradle).build()

    assertThat(result).output().any { it.contains("version catalogs enabled: $expected") }
  }

  private fun createRunner(gradleVersion: String? = null) = createRunner(projectDir, gradleVersion)
    .withArguments("clean")

  @BeforeEach
  fun baseProject() {
    projectDir.resolve("settings.gradle.kts").writeText("""
      import ws.gross.gradle.*
      
      rootProject.name = "test"
      
      plugins { id("ws.gross.private-repo") apply false }
      
    """.trimIndent())

    projectDir.resolve("build.gradle.kts").writeText("""
      plugins { base }
      
    """.trimIndent())
  }
}
