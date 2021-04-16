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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

class BootstrapManifestPlugin : Plugin<Project> {
  override fun apply(project: Project) = project.run {
    val manifests = objects.domainObjectContainer(Manifest::class.java)
    extensions.add("manifests", manifests)

    manifests.all {
      val manifest = this
      outputFileName.convention("$name.properties")
      version.convention(provider { project.version.toString() })
      tasks.register("generate${name.capitalize()}Manifest", GenerateManifest::class.java) {
        this.manifest.set(manifest)
      }
    }
  }
}

abstract class Manifest @Inject constructor(@get:Internal val name: String) {
  @get:Input
  abstract val ids: ListProperty<String>

  @get:Input
  abstract val version: Property<String>

  @get:OutputDirectory
  abstract val outputDir: DirectoryProperty

  @get:Input
  abstract val outputFileName: Property<String>
}

abstract class GenerateManifest : WriteProperties() {
  @get:Nested
  abstract val manifest: Property<Manifest>

  @TaskAction
  override fun writeProperties() {
    val m = manifest.get()
    outputFile = m.outputDir.file(m.outputFileName).get().asFile
    property("ids", m.ids.get().sorted().joinToString(","))
    property("version", m.version.get())

    super.writeProperties()
  }
}
