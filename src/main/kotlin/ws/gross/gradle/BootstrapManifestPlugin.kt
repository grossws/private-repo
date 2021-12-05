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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.component.SoftwareComponentFactory
import org.gradle.api.internal.artifacts.DependencyResolutionServices
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.language.base.plugins.LifecycleBasePlugin
import ws.gross.gradle.extensions.BootstrapManifestPluginExtension
import ws.gross.gradle.extensions.DefaultBootstrapManifestPluginExtension
import ws.gross.gradle.tasks.GenerateBootstrapManifest
import java.util.function.Supplier
import javax.inject.Inject

class BootstrapManifestPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    logger.info("Applying ws.gross.bootstrap-manifest plugin")
    pluginManager.apply(BootstrapManifestBasePlugin::class)
    pluginManager.apply("maven-publish")
  }
}

class BootstrapManifestBasePlugin @Inject constructor(
  private val componentFactory: SoftwareComponentFactory
) : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    pluginManager.apply("base")

    if (group.toString().isEmpty()) {
      throw IllegalArgumentException("Project group required for ws.gross.bootstrap-manifest plugin")
    }

    val ext = extensions.create(
      BootstrapManifestPluginExtension::class.java,
      "manifest",
      DefaultBootstrapManifestPluginExtension::class.java,
      Supplier { serviceOf<DependencyResolutionServices>() }
    ) as DefaultBootstrapManifestPluginExtension

    val projectVersion = provider { project.version.toString() }
    val manifest = ext.bootstrapManifest.apply {
      version.convention(projectVersion.map { DefaultMutableVersionConstraint(it).asImmutable() })
    }

    val task = tasks.register<GenerateBootstrapManifest>("generateBootstrapManifest") {
      group = LifecycleBasePlugin.BUILD_GROUP
      description = "Generate bootstrap manifest"

      pluginIds.convention(manifest.pluginIds)
      catalogIds.convention(manifest.catalogs)
      version.convention(manifest.version.map { it.displayName })
      manifestDescription.convention(manifest.description)

      outputFile.convention(layout.buildDirectory.file("${manifest.name}.properties"))
    }

    val configuration = configurations.create("manifest") {
      isCanBeConsumed = true
      isCanBeResolved = false
      attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("manifest"))
      }
      outgoing {
        artifact(task.flatMap { it.outputFile }) {
          type = "manifest"
          extension = "properties"
          classifier = manifest.name
          builtBy(task)
        }
      }
    }

    val manifestComponent = componentFactory.adhoc("manifest").apply {
      components.add(this)
      addVariantsFromConfiguration(configuration) {}
    }

    pluginManager.withPlugin("maven-publish") {
      the<PublishingExtension>().publications {
        create<MavenPublication>("manifestMaven") {
          from(manifestComponent)
          suppressAllPomMetadataWarnings()
        }
      }
    }

    tasks.named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME) {
      dependsOn(task)
    }
  }
}
