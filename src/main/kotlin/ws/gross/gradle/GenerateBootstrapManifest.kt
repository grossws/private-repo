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
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WriteProperties

abstract class GenerateBootstrapManifest : DefaultTask() {
  private val delegate = WriteProperties()

  @get:Input
  abstract val pluginIds: ListProperty<String>

  @get:Input
  abstract val catalogIds: MapProperty<String, String>

  @get:Input
  abstract val version: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun writeProperties() {
    delegate.setOutputFile(outputFile)
    val catalogs = catalogIds.get().toList().sortedBy { it.first }
    val plugins = pluginIds.get().sorted()
    delegate.property("catalogIds", catalogs.joinToString(",") { "${it.first}=${it.second}" })
    delegate.property("pluginIds", plugins.joinToString(","))
    delegate.property("version", version.get())

    logger.info("""
    |Writing manifest to ${outputFile.get()}:
    |  catalogIgs = ${catalogs.joinToString(" \\\n|    ")}
    |  pluginIds = ${plugins.joinToString(" \\\n|    ")}
    |  version = ${version.get()}
    """.trimMargin())
    delegate.writeProperties()
  }
}