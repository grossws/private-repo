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

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties
import org.gradle.kotlin.dsl.*
import javax.inject.Inject

class BootstrapManifestPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    logger.info("Applying BootstrapManifestPlugin")
    pluginManager.apply(BasePlugin::class)

    val manifests = objects.domainObjectContainer(Manifest::class.java)
    extensions.add("manifests", manifests)

    manifests.whenObjectAdded {
      logger.info("Manifest `${this.name}` added")

      val manifest = apply {
        version.convention(provider { project.version.toString() })
        outputDir.convention(layout.buildDirectory)
        outputFileName.convention("$name.properties")
      }

      tasks.register("generate${name.capitalize()}Manifest", GenerateManifest::class.java) {
        group = BasePlugin.BUILD_GROUP
        description = "Generate bootstrap manifest ${manifest.name}"

        ids.convention(manifest.ids)
        version.convention(manifest.version.convention(provider { project.version.toString() }))
        outputFile.convention(manifest.outputDir.file(outputFileName))
      }
    }

    tasks.register("generateManifests") {
      group = BasePlugin.BUILD_GROUP
      description = "Lifecycle task depending on all generate*Manifest tasks"

      dependsOn(tasks.withType<GenerateManifest>())
    }
  }
}

abstract class Manifest @Inject constructor(val name: String) {
  abstract val ids: ListProperty<String>

  abstract val version: Property<String>

  abstract val outputDir: DirectoryProperty

  abstract val outputFileName: Property<String>
}

abstract class GenerateManifest : DefaultTask() {
  //  @get:Internal
  private val delegate = WriteProperties()

  @get:Input
  abstract val ids: ListProperty<String>

  @get:Input
  abstract val version: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun writeProperties() {
    delegate.setOutputFile(outputFile)
    delegate.property("ids", ids.get().sorted().joinToString(","))
    delegate.property("version", version.get())

    logger.info("""
    Writing manifest to ${outputFile.get()}:
      ids = ${ids.get().sorted().joinToString(" \\\n        ")}
      version = ${version.get()}
    """.trimIndent())
    delegate.writeProperties()
  }
}
