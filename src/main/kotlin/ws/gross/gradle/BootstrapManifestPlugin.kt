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

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

class BootstrapManifestPlugin @Inject constructor(private val componentFactory: SoftwareComponentFactory) : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    logger.info("Applying ws.gross.bootstrap-manifest plugin")
    pluginManager.apply("base")

    val ext = extensions.create("bootstrapManifest", BootstrapManifestExtension::class.java)

    val manifestComponent = componentFactory.adhoc("manifest").also { components.add(it) }

    ext.manifests.configureEach {
      logger.info("Manifest `${this.name}` added")

      val manifest = apply {
        version.convention(provider { project.version.toString() })
        outputDir.convention(layout.buildDirectory)
        outputFileName.convention("$name.properties")
      }

      val configuration = configurations.create(manifest.configurationName) {
        isCanBeConsumed = true
        isCanBeResolved = false
        attributes {
          attribute(Category.CATEGORY_ATTRIBUTE, objects.named("manifest"))
        }
        outgoing.capability("${project.group}:${manifest.name}-manifest:1")
      }

      val task = tasks.register(manifest.generateTaskName, GenerateBootstrapManifest::class.java) {
        group = BasePlugin.BUILD_GROUP
        description = "Generate bootstrap manifest ${manifest.name}"

        pluginIds.convention(manifest.pluginIds)
        version.convention(manifest.version.convention(provider { project.version.toString() }))
        outputFile.convention(manifest.outputDir.file(outputFileName))
      }

      artifacts.add(configuration.name, task.flatMap { it.outputFile }) {
          type="man1"
          extension="propertieZ"
          classifier=manifest.name
          this.builtBy(task)
      }

      manifestComponent.addVariantsFromConfiguration(configuration) {
      }
    }

    tasks.register("generateManifests") {
      group = BasePlugin.BUILD_GROUP
      description = "Lifecycle task depending on all generate*Manifest tasks"

      dependsOn(tasks.withType<GenerateBootstrapManifest>())
    }
  }
}

abstract class BootstrapManifestExtension {
  abstract val manifests: NamedDomainObjectContainer<Manifest>
}

abstract class Manifest @Inject constructor(val name: String) {
  abstract val pluginIds: ListProperty<String>

  abstract val version: Property<String>

  abstract val outputDir: DirectoryProperty

  abstract val outputFileName: Property<String>

  val configurationName = "${name}Manifest"
  val generateTaskName = "generate${name.capitalize()}Manifest"
}
