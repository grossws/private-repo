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

import org.gradle.api.GradleException
import org.gradle.api.attributes.Category
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.File
import java.util.Properties

data class Bootstrap(
  val pluginIds: List<String>,
  val version: String
) {
  companion object {
    private val logger = Logging.getLogger(Bootstrap::class.java)

    fun from(settings: Settings, dependencyNotation: Any): Bootstrap = settings.run {
      val objects = settings.serviceOf<ObjectFactory>()

      val manifest = buildscript.dependencies.create(dependencyNotation)
      val configuration = buildscript.configurations.detachedConfiguration(manifest).apply {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes {
          attribute(Category.CATEGORY_ATTRIBUTE, objects.named("manifest"))
        }
      }
      val files = configuration.resolve()

      if (files.size != 1) {
        logger.warn("Bootstrap manifest $manifest resolved to more than 1 file: ${files.joinToString(", ")}")
        throw GradleException("Invalid bootstrap manifest $manifest")
      }

      return from(files.first())
    }

    private fun from(file: File): Bootstrap {
      val props = Properties().apply { file.reader().use { load(it) } }
      return Bootstrap(
        pluginIds = props.getProperty("pluginIds").parseList(),
        version = props.getProperty("version")
      )
    }
  }
}
