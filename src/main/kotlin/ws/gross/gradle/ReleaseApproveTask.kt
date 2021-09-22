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
