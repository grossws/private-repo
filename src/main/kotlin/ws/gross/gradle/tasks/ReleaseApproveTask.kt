/*
 * Copyright 2021-2022 Konstantin Gribov
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
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ReleaseApproveTask : DefaultTask() {
  init {
    projectVersion.set(project.provider { project.version.toString() })
    approveProperty.convention("release.approve")
  }

  @get:Input
  abstract val projectVersion: Property<String>

  @get:Input
  abstract val approveProperty: Property<String>

  @get:Input
  val approve: Provider<String> = project.providers.gradleProperty(approveProperty).orElse("")

  @TaskAction
  fun run() {
    if (approve.get() == "true") {
      logger.info("Explicitly approved via -P${approveProperty.get()}=true")
      return
    } else if (approve.get() == "false") {
      logger.info("Explicitly not approved via -P${approveProperty.get()}=false")
    } else {
      val userInputHandler = services.get(UserInputHandler::class.java)
      when (userInputHandler.askYesNoQuestion("Release ${projectVersion.get()} version?")) {
        null -> logger.info("Interactive approve failed to get answer, maybe running in non-interactive environment")
        false -> logger.info("Interactively not approved")
        true -> {
          logger.info("Interactively approved")
          return
        }
      }
    }
    throw GradleException("Not approved, use -P${approveProperty.get()}=true for non-interactive mode to approve release")
  }
}
