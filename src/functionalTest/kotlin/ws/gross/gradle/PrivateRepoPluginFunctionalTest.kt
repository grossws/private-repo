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
import org.gradle.api.internal.artifacts.BaseRepositoryFactory.PLUGIN_PORTAL_DEFAULT_URL
import org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler.GRADLE_PLUGIN_PORTAL_REPO_NAME
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PrivateRepoPluginFunctionalTest {
  companion object {
    private val projectDir = createProjectDir()

    @BeforeAll
    @JvmStatic
    fun initRepo() {
      publishCatalogAndManifest(projectDir.resolve("repo"))
    }
  }

  @Nested
  inner class Prerequisites {
    @Test
    fun `fails if old gradle`() {
      val result = createRunner("7.3").buildAndFail()

      assertThat(result).output().any { it.contains("Only Gradle 7.4+ supported", ignoreCase = true) }
    }

    @Test
    fun `fails if nexusUrl property absent`() {
      projectDir.resolve("gradle.properties").writeText("")

      val result = createRunner().buildAndFail()

      assertThat(result).output().any { it.contains("nexusUrl should be defined", ignoreCase = true) }
    }
  }

  @Nested
  inner class NormalUse {
    @ParameterizedTest
    @ValueSource(strings = ["7.4", "7.4.1", "7.6.1","8.0.2"])
    fun `sane defaults`(gradleVersion: String) {
      val result = createRunner(gradleVersion).build()

      assertThat(parseDependencyRepos(result.output)).dependencyRepos {
        each {
          it.includeMatchers().isEmpty()
        }
        index(0).excludeMatchers().groupRegexes("com\\.example(\\..*)?")
        index(1).excludeMatchers().isEmpty()
      }
    }

    @ParameterizedTest
    @ValueSource(strings = ["7.4", "7.4.1", "7.6.1","8.0.2"])
    fun `correct plugin repos`(gradleVersion: String) {
      val result = createRunner(gradleVersion).build()

      assertThat(parsePluginRepos(result.output)).all {
        hasSize(3)
        index(0).nexus()
        index(1).mavenCentral()
        index(2).gradlePluginPortal()
      }
    }

    @Test
    fun `without default regexes`() {
      projectDir.resolve("gradle.properties").appendText("""
      nexusDefaultGroupRegex = false
      """.trimIndent())

      val result = createRunner().build()

      assertThat(parseDependencyRepos(result.output)).dependencyRepos {
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

      val result = createRunner().build()

      assertThat(parseDependencyRepos(result.output)).dependencyRepos {
        each { it.includeMatchers().isEmpty() }
        index(0).excludeMatchers().groupRegexes("org\\.example(\\..*)?", "dev\\..*")
        index(1).excludeMatchers().isEmpty()
      }
    }

    @Test
    fun `with explicit groups`() {
      projectDir.resolve("gradle.properties").appendText("""
      nexusGroups = org.example, org.example.gradle
      """.trimIndent())

      val result = createRunner().build()

      assertThat(parseDependencyRepos(result.output)).dependencyRepos {
        each { it.includeMatchers().isEmpty() }
        index(0).excludeMatchers().groups("org.example", "org.example.gradle")
        index(1).excludeMatchers().isEmpty()
      }
    }
  }

  abstract inner class BootstrapManifestsBase {
    @BeforeEach
    fun init() {
      projectDir.resolve("dummy-repo.init.gradle.kts").writeText("""
        gradle.beforeSettings {
          // dummy repo to resolve manifest from
          pluginManagement.repositories.maven {
            name = "TestMaven"
            url = uri("repo")
          }
          // dummy repo to resolve catalog from
          dependencyResolutionManagement.repositories.maven {
            name = "TestMaven"
            url = uri("repo")
          }
        }
      """.trimIndent())

      configureBootstrap()
    }

    abstract fun configureBootstrap()
    abstract fun configureBootstrapCatalogs()
    protected open val gradleVersion: String? = null

    @Test
    fun `plugins added by bootstrap`() {
      projectDir.resolve("build.gradle.kts").writeText("""
        plugins { id("org.example.dummy") }
      """.trimIndent())

      val result = createRunner(gradleVersion).withArguments("-I", "dummy-repo.init.gradle.kts", "-i", "clean").build()

      assertThat(result).output().any { it.contains("DUMMY|dummy plugin applied") }
    }

    @Test
    fun `catalog added by bootstrap`() {
      val d = "$"
      projectDir.resolve("build.gradle.kts").writeText("""
        plugins { java }
        dependencies { implementation(dummy.guava) }
        configurations.compileClasspath.get().resolvedConfiguration.resolvedArtifacts.forEach {
          logger.lifecycle("DEP|$d{it.name}|$d{it.type}|$d{it.moduleVersion.id.version}|$d{it.file}")
        }
      """.trimIndent())

      configureBootstrapCatalogs()

      val result = createRunner(gradleVersion).withArguments("-I", "dummy-repo.init.gradle.kts", "-i", "clean").build()

      assertThat(result).output().any { it.contains("DEP|guava|jar|31") }
    }
  }

  @Nested
  open inner class BootstrapManifests : BootstrapManifestsBase() {
    override val gradleVersion = "7.4"

    override fun configureBootstrap() {
      projectDir.resolve("settings.gradle.kts").appendText("""
        privateRepo {
          manifests {
            create("dummy") { from("org.example:manifest:1.0") }
          }
        }

      """.trimIndent())
    }

    override fun configureBootstrapCatalogs() {
      // no-op
    }
  }

  @Nested
  inner class BootstrapManifestsDependencyResolutionServicesRegression : BootstrapManifests() {
    override val gradleVersion = "7.4.1"
  }

  private fun createRunner(gradleVersion: String? = null) = createRunner(projectDir, gradleVersion)
    .withArguments("clean")

  @BeforeEach
  fun baseProject() {
    val d = "$"

    projectDir.resolve("settings.gradle.kts").writeText("""
      import com.fasterxml.jackson.annotation.JsonAutoDetect
      import com.fasterxml.jackson.databind.ObjectMapper
      import org.gradle.api.internal.artifacts.repositories.RepositoryContentDescriptorInternal

      buildscript {
        repositories.mavenCentral()
        dependencies.classpath("com.fasterxml.jackson.core:jackson-databind:2.13.4")
      }

      plugins { id("ws.gross.private-repo") }

      val mapper = ObjectMapper().setDefaultVisibility(
        JsonAutoDetect.Value.defaultVisibility().withFieldVisibility(JsonAutoDetect.Visibility.ANY)
      )
      pluginManagement.repositories.forEach {
        val r = it as MavenArtifactRepository
        var f: Any? = null
        r.content { f = (this as RepositoryContentDescriptorInternal).toContentFilter() }
        println("PLUGIN_REPO|$d{r.name}|$d{r.url}|$d{mapper.writeValueAsString(f)}")
      }
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

private fun Assert<Repo>.nexus() = repo("nexus", "https://nexus.example.com/repository/public")
private fun Assert<Repo>.mavenCentral() = repo(DEFAULT_MAVEN_CENTRAL_REPO_NAME, MAVEN_CENTRAL_URL)
private fun Assert<Repo>.gradlePluginPortal() = repo(GRADLE_PLUGIN_PORTAL_REPO_NAME, PLUGIN_PORTAL_DEFAULT_URL)

private fun Assert<List<Repo>>.dependencyRepos(alsoCheck: Assert<List<Repo>>.() -> Unit) = all {
  hasSize(2)
  index(0).mavenCentral()
  index(1).nexus()
  alsoCheck()
}

private fun Assert<Repo>.repo(name: String, url: String): Assert<Repo> = transform { repo ->
  prop("name") { it.name }.isEqualTo(name)
  prop("url") { it.url }.isEqualTo(url)
  includeMatchers().allValid()
  excludeMatchers().allValid()
  repo
}

private fun Assert<Repo>.filters() = prop("filters") { it.filters }

private fun Assert<Repo>.includeMatchers() = filters().prop("include") { it.includeMatchers }

private fun Assert<Repo>.excludeMatchers() = filters().prop("exclude") { it.excludeMatchers }

private fun Assert<List<RepoFilter>>.allValid() = transform { filters ->
  each { filter -> filter.prop("valid") { it.isValid() }.isTrue() }
  filters
}

private fun Assert<List<RepoFilter>>.groupRegexes(vararg regex: String) = prop("groupPattern") { filters ->
  filters.mapNotNull { it.groupPattern }.toSet()
}.containsOnly(*regex)

private fun Assert<List<RepoFilter>>.groups(vararg regex: String) = prop("group") { filters ->
  filters.mapNotNull { it.group }.toSet()
}.containsOnly(*regex)

private val mapper = jacksonObjectMapper()
  .findAndRegisterModules()
  .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

private fun parseRepos(data: String, prefix: String) = data.lineSequence()
  .filter { it.startsWith("$prefix|") }
  .map { it.split('|') }
  .map { Repo(it[1], it[2], mapper.readValue(it[3])) }
  .toList()

private fun parseDependencyRepos(data: String): List<Repo> = parseRepos(data, "REPO")
  .sortedBy { it.name.toLowerCase() }

private fun parsePluginRepos(data: String): List<Repo> = parseRepos(data, "PLUGIN_REPO")
