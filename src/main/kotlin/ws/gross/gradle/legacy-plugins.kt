/*
 * Copyright 2022 Konstantin Gribov
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
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.kotlin.dsl.*

@Deprecated(
  message = "Replaced with PrivateRepoPlugin",
  level = DeprecationLevel.WARNING
)
class NexusPlugin : Plugin<Settings> {
  companion object {
    // decoupling to not fail with obscure message on gradle
    val pluginVersion: String
      get() = PrivateRepoPlugin.pluginVersion
    private val logger = Logging.getLogger(NexusPlugin::class.java)
  }

  override fun apply(settings: Settings): Unit = settings.run {
    logger.lifecycle("NexusPlugin is deprecated, use PrivateRepoPlugin instead")
    apply<PrivateRepoPlugin>()
  }
}

@Deprecated(
  message = "Replaced with PrivateRepoPublishPlugin",
  level = DeprecationLevel.WARNING
)
class NexusPublishPlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    logger.lifecycle("NexusPublishPlugin is deprecated, use PrivateRepoPublishPlugin instead")
    apply<PrivateRepoPublishPlugin>()
  }
}
