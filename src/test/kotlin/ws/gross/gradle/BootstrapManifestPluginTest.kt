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
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.internal.plugins.PluginApplicationException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.*
import ws.gross.gradle.extensions.BootstrapManifestPluginExtension
import ws.gross.gradle.tasks.GenerateBootstrapManifest

class BootstrapManifestPluginTest {
  companion object {
    fun baseProject(group: String? = "some.group"): Project = ProjectBuilder.builder()
      .withName("test")
      .build()
      .also { project ->
        group?.let { project.group = it }
        project.pluginManager.apply("ws.gross.bootstrap-manifest")
      }
  }

  private val project = baseProject()

  @Test
  fun `fails without group`() {
    assertThat { baseProject(group = null) }
      .isFailure()
      .isInstanceOf(PluginApplicationException::class)
      .cause()
      .isNotNull()
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("group required")
  }

  @Test
  fun `plugin present`() {
    assertThat(project.plugins.findPlugin("ws.gross.bootstrap-manifest"))
      .isNotNull()
      .isInstanceOf(BootstrapManifestPlugin::class)
  }

  @Test
  fun `extension present`() {
    assertThat(project.extensions.findByName("manifest"))
      .isNotNull()
      .isInstanceOf(BootstrapManifestPluginExtension::class)
  }

  @Test
  fun `generate task present`() {
    assertThat(project.tasks.findByName("generateBootstrapManifest"))
      .isNotNull()
      .isInstanceOf(GenerateBootstrapManifest::class)
  }

  @Test
  fun `software component present`() {
    assertThat(project.components.findByName("manifest"))
      .isNotNull()
      .isInstanceOf(AdhocComponentWithVariants::class)
  }

  @Test
  fun `consumable configuration present`() {
    assertThat(project.configurations.findByName("manifest"))
      .isNotNull()
      .all {
        prop("isCanBeConsumed") { it.isCanBeConsumed }.isTrue()
        prop("isCanBeResolved") { it.isCanBeResolved }.isFalse()
        prop("attributes") { it.attributes }.all {
          prop("category") { it.getAttribute(Category.CATEGORY_ATTRIBUTE) }
            .isNotNull()
            .prop("name") { it.name }.isEqualTo("manifest")
        }
      }
  }

  @Test
  fun `creates publication when maven-publish plugin applied`() {
    project.pluginManager.apply("maven-publish")
    assertThat(project)
      .prop("publishing") { it.the<PublishingExtension>() }
      .prop("publications") { it.publications }
      .prop("manifestMaven") { it.findByName("manifestMaven") }
      .isNotNull()
      .isInstanceOf(MavenPublication::class)
  }
}
