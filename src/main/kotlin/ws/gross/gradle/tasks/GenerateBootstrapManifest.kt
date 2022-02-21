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

package ws.gross.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.util.PropertiesUtils
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.Properties

abstract class GenerateBootstrapManifest : DefaultTask() {
  @get:Input
  abstract val pluginIds: ListProperty<String>

  @get:Input
  abstract val catalogIds: MapProperty<String, String>

  @get:Input
  abstract val version: Property<String>

  @get:Input
  @get:Optional
  abstract val manifestDescription: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun writeProperties() {
    val properties = Properties()

    val catalogs = catalogIds.get().toList().sortedBy { it.first }
    val plugins = pluginIds.get().sorted()
    properties.setProperty("catalogIds", catalogs.joinToString(",") { "${it.first}=${it.second}" })
    properties.setProperty("pluginIds", plugins.joinToString(","))
    properties.setProperty("version", version.get())
    manifestDescription.orNull?.let { properties.setProperty("description", it) }

    logger.info("""
    |Writing manifest to ${outputFile.get()}:
    |  catalogIgs = ${catalogs.joinToString(" \\\n|    ")}
    |  pluginIds = ${plugins.joinToString(" \\\n|    ")}
    |  version = ${version.get()}
    |  description = ${manifestDescription.orElse("<none>")}
    """.trimMargin())

    BufferedOutputStream(FileOutputStream(outputFile.get().asFile)).use { out ->
      PropertiesUtils.store(properties, out, null, StandardCharsets.UTF_8, "\n")
    }
  }
}
