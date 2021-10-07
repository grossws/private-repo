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
import org.gradle.api.GradleException
import org.gradle.api.internal.tasks.userinput.UserInputHandler
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

abstract class ReleaseApproveTask : DefaultTask() {
  @get:Input
  abstract val approve: Property<String>

  @TaskAction
  fun run() {
    if (approve.get() == "true") {
      return
    } else if (approve.get() == "false") {
      throw GradleException("Not approved")
    }

    val userInputHandler = services.get(UserInputHandler::class.java)
    if (!userInputHandler.askYesNoQuestion("Release ${project.version} version?", false)) {
      throw GradleException("Not approved, use -Prelease.approve=true for non-interactive mode to approve release")
    }
  }
}
