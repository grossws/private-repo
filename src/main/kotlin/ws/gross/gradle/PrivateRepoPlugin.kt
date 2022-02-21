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
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.kotlin.dsl.*
import org.gradle.util.GradleVersion
import ws.gross.gradle.impl.PrivateRepoPluginImpl

class PrivateRepoPlugin : Plugin<Settings> {
  companion object {
    // decoupling to not fail with obscure message on gradle
    val pluginVersion: String
      get() = PrivateRepoPluginImpl.pluginVersion

    private val logger: Logger = Logging.getLogger(PrivateRepoPlugin::class.java)
  }

  override fun apply(settings: Settings) {
    if (GradleVersion.current() < GradleVersion.version("6.8")) {
      throw PluginInstantiationException("Only Gradle 6.8+ supported")
    }

    if (GradleVersion.current() < GradleVersion.version("7.4")) {
      logger.lifecycle("Gradle 7.4+ recommended with ws.gross.private-repo plugin")
    }

    settings.apply<PrivateRepoPluginImpl>()
  }
}
