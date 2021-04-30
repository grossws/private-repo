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
import assertk.assertions.*
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.gradle.api.artifacts.ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME
import org.gradle.api.artifacts.ArtifactRepositoryContainer.MAVEN_CENTRAL_URL
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File

class NexusPluginFunctionalTest {
  lateinit var projectDir: File

  @BeforeEach
  fun init() {
    projectDir = baseProject()
  }

  @Nested
  inner class Prerequisites {
    @Test
    fun `fails if old gradle`() {
      val result = createRunner(projectDir, "6.7").buildAndFail()

      assertThat(result.output).contains("Only Gradle 6.8+ supported")
    }

    @Test
    fun `fails if nexusUrl property absent`() {
      projectDir.resolve("gradle.properties").writeText("")

      val result = createRunner(projectDir).buildAndFail()

      assertThat(result.output).contains("nexusUrl should be defined")
    }
  }

  @Nested
  inner class NormalUse {
    @ParameterizedTest
    @ValueSource(strings = ["6.8.3", "7.0"])
    fun `sane defaults`(gradleVersion: String) {
      val result = createRunner(projectDir, gradleVersion).build()

      assertThat(parseOutput(result.output)).repos {
        each {
          it.includeMatchers().isEmpty()
        }
        mavenCentral().excludeMatchers().groupRegexes("com\\.example(\\..*)?")
        nexus().excludeMatchers().isEmpty()
      }
    }

    @Test
    fun `without default regexes`() {
      projectDir.resolve("gradle.properties").appendText("""
      nexusDefaultGroupRegex = false
      """.trimIndent())

      val result = createRunner(projectDir).build()

      assertThat(parseOutput(result.output)).repos {
        each {
          it.includeMatchers().isEmpty()
          it.excludeMatchers().isEmpty()
        }
      }
    }

    @Test
    fun `with explicit regexes`() {
      projectDir.resolve("gradle.properties").appendText("""
      nexusGroupRegexes = org\\.example(\\..*)?, dev\\..*
      """.trimIndent())

      val result = createRunner(projectDir).build()

      assertThat(parseOutput(result.output)).repos {
        each { it.includeMatchers().isEmpty() }
        mavenCentral().excludeMatchers().groupRegexes("org\\.example(\\..*)?", "dev\\..*")
        nexus().excludeMatchers().isEmpty()
      }
    }

    @Test
    fun `with explicit groups`() {
      projectDir.resolve("gradle.properties").appendText("""
      nexusGroups = org.example, org.example.gradle
      """.trimIndent())

      val result = createRunner(projectDir).build()

      assertThat(parseOutput(result.output)).repos {
        each { it.includeMatchers().isEmpty() }
        mavenCentral().excludeMatchers().groups("org.example", "org.example.gradle")
        nexus().excludeMatchers().isEmpty()
      }
    }
  }

  @Nested
  inner class ExclusiveContent {

  }

  private fun createRunner(projectDir: File, gradleVersion: String? = null) = GradleRunner.create()
    .forwardOutput()
    .withPluginClasspath()
    .withArguments("clean")
    .withProjectDir(projectDir)
    .apply { if (gradleVersion != null) withGradleVersion(gradleVersion) }

  private fun baseProject(): File {
    val projectDir = File("build/functionalTest/private-repo")
    projectDir.mkdirs()

    val d = "$"

    projectDir.resolve("settings.gradle.kts").writeText("""
      import com.fasterxml.jackson.annotation.JsonAutoDetect
      import com.fasterxml.jackson.databind.ObjectMapper
      import org.gradle.api.internal.artifacts.repositories.RepositoryContentDescriptorInternal

      buildscript {
        repositories.mavenCentral()
        dependencies.classpath("com.fasterxml.jackson.core:jackson-databind:2.12.2")
      }

      plugins { id("ws.gross.private-repo") }

      val mapper = ObjectMapper().setDefaultVisibility(
        JsonAutoDetect.Value.defaultVisibility().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
      )
      dependencyResolutionManagement.repositories.forEach {
        val r = it as MavenArtifactRepository
        var f: Any? = null
        r.content { f = (this as RepositoryContentDescriptorInternal).toContentFilter() }
        println("REPO|$d{r.name}|$d{r.url}|$d{mapper.writeValueAsString(f)}")
      }
    """.trimIndent())

    projectDir.resolve("build.gradle.kts").writeText("""
      plugins { base }
    """.trimIndent())

    projectDir.resolve("gradle.properties").writeText("""
      nexusUrl = https://nexus.example.com
      nexusUsername = gradle
      nexusPassword = secret

    """.trimIndent())

    return projectDir
  }
}

data class Repo(val name: String, val url: String, val filters: RepoFilters)

data class RepoFilters(
  val includeMatchers: List<RepoFilter> = listOf(),
  val excludeMatchers: List<RepoFilter> = listOf(),
) {
  companion object {
    @JsonCreator
    @JvmStatic
    fun of(includeMatchers: List<RepoFilter>?, excludeMatchers: List<RepoFilter>?) =
      RepoFilters(includeMatchers ?: listOf(), excludeMatchers ?: listOf())
  }
}

data class RepoFilter(
  val inclusive: Boolean,
  val groupPattern: String? = null,
  val modulePattern: String? = null,
  val versionPattern: String? = null,
  val group: String? = null,
  val module: String? = null,
  val version: String? = null,
) {
  fun isValid() = when {
    groupPattern != null -> {
      group == null && module == null && version == null
    }
    group != null -> {
      modulePattern == null && versionPattern == null
    }
    else -> {
      false
    }
  }
}

fun Assert<List<Repo>>.mavenCentral() = index(0)
fun Assert<List<Repo>>.nexus() = index(1)

fun Assert<List<Repo>>.repos(alsoCheck: Assert<List<Repo>>.() -> Unit) = all {
  hasSize(2)
  mavenCentral().repo(DEFAULT_MAVEN_CENTRAL_REPO_NAME, MAVEN_CENTRAL_URL)
  nexus().repo("nexus", "https://nexus.example.com/repository/public")
  alsoCheck()
}

fun Assert<Repo>.repo(name: String, url: String): Assert<Repo> = transform { repo ->
  prop("name") { it.name }.isEqualTo(name)
  prop("url") { it.url }.isEqualTo(url)
  includeMatchers().allValid()
  excludeMatchers().allValid()
  repo
}

fun Assert<Repo>.filters() = prop("filters") { it.filters }

fun Assert<Repo>.includeMatchers() = filters().prop("include") { it.includeMatchers }

fun Assert<Repo>.excludeMatchers() = filters().prop("exclude") { it.excludeMatchers }

fun Assert<List<RepoFilter>>.allValid() = transform { filters ->
  each { filter -> filter.prop("valid") { it.isValid() }.isTrue() }
  filters
}

fun Assert<List<RepoFilter>>.groupRegexes(vararg regex: String) = prop("groupPattern") { filters ->
  filters.map { it.groupPattern }.filterNotNull().toSet()
}.containsOnly(*regex)

fun Assert<List<RepoFilter>>.groups(vararg regex: String) = prop("group") { filters ->
  filters.map { it.group }.filterNotNull().toSet()
}.containsOnly(*regex)

private val mapper = jacksonObjectMapper()
  .findAndRegisterModules()
  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

private fun parseOutput(data: String): List<Repo> = data.lineSequence()
  .filter { it.startsWith("REPO|") }
  .map { it.split('|') }
  .map { Repo(it[1], it[2], mapper.readValue(it[3])) }
  .toList()
  .sortedBy { it.name.toLowerCase() }
