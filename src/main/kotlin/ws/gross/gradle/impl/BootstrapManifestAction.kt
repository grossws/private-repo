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

package ws.gross.gradle.impl

import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.*
import ws.gross.gradle.extensions.PrivateRepoExtension
import ws.gross.gradle.isVersionCatalogsEnabled
import ws.gross.gradle.isVersionCatalogsExperimental

@Suppress("UnstableApiUsage")
internal class BootstrapManifestAction(private val name: String) : Action<Settings> {
  companion object {
    val logger: Logger = Logging.getLogger(BootstrapManifestAction::class.java)
  }

  override fun execute(settings: Settings): Unit = settings.run {
    val bootstrap = the<PrivateRepoExtension>().manifests.getByName(name)
    logger.info("Adding bootstrap manifest ${bootstrap.name}: ${bootstrap.description.getOrElse("")}")

    val version = bootstrap.version.get().displayName

    pluginManagement.plugins {
      bootstrap.pluginIds.get().forEach {
        logger.info("Adding plugin $it $version")
        id(it) version version
      }
    }

    if (isVersionCatalogsExperimental() && !isVersionCatalogsEnabled()) {
      // VERSION_CATALOGS is still feature preview but not enabled
      logger.info("Version catalogs are still feature preview but not explicitly enabled, skipping catalogs bootstrap")
      return@run
    }

    dependencyResolutionManagement.versionCatalogs {
      bootstrap.catalogs.get().forEach { (alias, dependencyNotation) ->
        logger.info("Adding catalog $alias -> $dependencyNotation:$version")
        create(alias) { from("$dependencyNotation:$version") }
      }
    }
  }
}
