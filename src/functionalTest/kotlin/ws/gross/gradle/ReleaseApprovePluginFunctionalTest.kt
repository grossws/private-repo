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

import assertk.Assert
import assertk.assertThat
import assertk.assertions.*
import com.github.syari.kgit.KGit
import org.eclipse.jgit.transport.URIish
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class ReleaseApprovePluginFunctionalTest {
  companion object {
    @JvmStatic
    fun approve() = parameters(approve = true, args = listOf("approveRelease"))

    @JvmStatic
    fun disallow() = parameters(approve = false, args = listOf("approveRelease"))
      .flatMap { Stream.of(it, it.copy(approve = null)) }

    @JvmStatic
    fun approveFinal() = parameters(approve = true, args = listOf("release", "final"))

    @JvmStatic
    fun disallowFinal() = parameters(approve = false, args = listOf("release", "final"))
      .flatMap { Stream.of(it, it.copy(approve = null)) }

    fun parameters(approve: Boolean?, args: List<String>, configurationCache: Boolean = false): Stream<Parameters> =
      Stream.of(
        Parameters(approve = approve, interactive = false, args = args),
        Parameters(approve = approve, interactive = true, args = args),
      ).flatMap { if (configurationCache) Stream.of(it, it.copy(configurationCache = true)) else Stream.of(it) }
  }

  private val projectDir = createProjectDir()

  @Nested
  inner class TaskPresence {
    @Test
    fun `releaseApprove task absent when nebula release not applied`() {
      projectDir.resolve("build.gradle.kts").writeText("""
        plugins { id("ws.gross.release-approve") }
      """.trimIndent())

      val parameters = Parameters(approve = null, interactive = false, args = listOf("tasks"), expectFail = false)
      val result = createRunner(parameters).build(parameters)

      assertThat(result).task(":tasks").isSuccess()
      assertThat(result).output().none { it.startsWith("approveRelease - ") }
    }

    @Test
    fun `releaseApprove task present`() {
      val parameters = Parameters(approve = null, interactive = false, args = listOf("tasks"), expectFail = false)
      val result = createRunner(parameters).build(parameters)

      assertThat(result).task(":tasks").isSuccess()
      assertThat(result).output().any { it.startsWith("approveRelease - ") }
    }
  }

  @ParameterizedTest
  @MethodSource("approve")
  fun `approve task succeeds`(parameters: Parameters) {
    val result = createRunner(parameters).build(parameters)

    assertThat(result).task(":approveRelease").isSuccess()
    if (parameters.configurationCache) assertThat(result).reusedConfigurationCache()
  }

  @ParameterizedTest
  @MethodSource("disallow")
  fun `approve task fails`(parameters: Parameters) {
    val result = createRunner(parameters).build(parameters)

    assertThat(result).task(":approveRelease").isFailed()
    assertThat(result).notApproved()
    if (parameters.configurationCache) assertThat(result).reusedConfigurationCache()
  }

  @ParameterizedTest
  @MethodSource("approveFinal")
  fun `approved release successful`(parameters: Parameters) {
    val result = createRunner(parameters).build(parameters)

    assertThat(result).task(":approveRelease").isSuccess()

    val tag = KGit.open(projectDir).use { it.describe { setTags(true) } }
    assertThat(tag).isNotNull().isEqualTo("v0.2.0")
  }

  @ParameterizedTest
  @MethodSource("disallowFinal")
  fun `not approved release fails`(parameters: Parameters) {
    val result = createRunner(parameters).build(parameters)

    assertThat(result).task(":approveRelease").isFailed()
    assertThat(result).notApproved()

    val tag = KGit.open(projectDir).use { it.describe { setTags(true) } }
    assertThat(tag).isNotNull().startsWith("v0.1.0-1-g")
  }

  data class Parameters(
    val approve: Boolean?,
    val interactive: Boolean,
    val configurationCache: Boolean = false,
    val args: List<String>,
    val expectFail: Boolean = approve == null || !approve
  ) {
    val interactiveInput: String?
      get() = approve?.let { if (approve) "yes\n" else "no\n" }

    val effectiveArgs: List<String>
      get() = args.toMutableList().apply {
        if (!interactive && approve != null) add("-Prelease.approve=${approve}")
        if (configurationCache) add("--configuration-cache")
      }
  }

  private fun createRunner(
    parameters: Parameters,
    gradleVersion: String? = null,
  ): GradleRunner = createRunner(projectDir, gradleVersion)
    .withArguments(parameters.effectiveArgs)
    .also {
      if (parameters.interactive && parameters.approve != null) it.withInteractiveInput(parameters.interactiveInput)
    }

  private fun GradleRunner.build(parameters: Parameters): BuildResult {
    if (parameters.configurationCache) {
      if (parameters.expectFail) buildAndFail() else build()
      if (parameters.approve != null) withInteractiveInput(parameters.interactiveInput)
    }
    return if (parameters.expectFail) buildAndFail() else build()
  }

  @BeforeEach
  fun baseProject() {
    val remoteRepoDir = projectDir.resolve("remote.git")
    KGit.init {
      setBare(true)
      setDirectory(remoteRepoDir)
    }.close()

    projectDir.resolve("settings.gradle.kts").writeText("""
      rootProject.name = "test"
      
    """.trimIndent())

    projectDir.resolve("build.gradle.kts").writeText("""
      plugins { 
        id("ws.gross.release-approve")
        id("nebula.release") version "17.2.0"
      }
      
    """.trimIndent())

    projectDir.resolve("gradle.properties").writeText("""
      group = org.example.test
      
    """.trimIndent())

    projectDir.resolve(".gitignore").writeText("""
      .gradle/
      remote.git/
      build/
    """.trimIndent())

    KGit.init {
      setBare(false)
      setDirectory(projectDir)
    }.use { repo ->
      repo.remoteAdd {
        setName("origin")
        setUri(URIish(remoteRepoDir.absolutePath))
      }

      repo.add {
        addFilepattern(".gitignore")
        addFilepattern("build.gradle.kts")
        addFilepattern("settings.gradle.kts")
      }
      repo.commit {
        message = "Initial commit"
        setAuthor("test", "test@localhost")
      }

      repo.tag {
        name = "v0.1.0"
        isAnnotated = false
      }

      repo.add {
        addFilepattern("gradle.properties")
      }
      repo.commit {
        message = "Add gradle properties"
        setAuthor("test", "test@localhost")
      }

      repo.push {
        setPushAll()
        setPushTags()
      }
    }
  }

  private fun Assert<BuildResult>.notApproved() = prop("output") { it.output.lines() }.any {
    it.contains("not approved", ignoreCase = true)
  }
}
