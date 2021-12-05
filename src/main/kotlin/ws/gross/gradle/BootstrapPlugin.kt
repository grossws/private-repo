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
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.*
import ws.gross.gradle.extensions.PrivateRepoExtension
import ws.gross.gradle.impl.BootstrapManifestAction

@Suppress("UnstableApiUsage")
class BootstrapPlugin : Plugin<Settings> {
  companion object {
    private val logger: Logger = Logging.getLogger(BootstrapPlugin::class.java)
  }

  override fun apply(settings: Settings): Unit = settings.run {
    apply<PrivateRepoBasePlugin>()

    val ext = the<PrivateRepoExtension>()

    val bootstrapManifests = providers.gradleProperty("nexusBootstrap")
      .forUseAtConfigurationTime()
      .orElse("").map { it.parseList() }.get()
    if (bootstrapManifests.isNotEmpty()) {
      logger.warn("""
        nexusBootstrap property is deprecated:
          use privateRepo extension in settings to add manifests.
      """.trimIndent())
    }

    val bootstrapCatalogs = providers.gradleProperty("nexusBootstrapCatalogs")
      .forUseAtConfigurationTime()
      .map { it.toBoolean() }.orElse(false).get()
    if (bootstrapCatalogs) {
      enableFeaturePreview("VERSION_CATALOGS")
      logger.warn("""
        nexusBootstrapCatalogs property is deprecated:
          use enableFeaturePreview("VERSION_CATALOGS") in settings.gradle.kts
          to add catalogs from manifests automatically.
      """.trimIndent())
    }

    bootstrapManifests.forEachIndexed { i, manifest ->
      ext.manifests.create("legacy$i") { from(manifest) }
    }

    ext.manifests.all { gradle.settingsEvaluated(BootstrapManifestAction(name)) }
  }
}
