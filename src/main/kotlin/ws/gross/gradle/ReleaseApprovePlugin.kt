package ws.gross.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*

class ReleaseApprovePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    pluginManager.withPlugin("nebula.release") {
      val approveRelease by tasks.registering(ReleaseApproveTask::class) {
        group = "Nebula Release"
        description = "Approve rc/final release before pushing git tag"
        approve.set(providers.gradleProperty("release.approve").orElse(""))
      }

      tasks.named("candidateSetup") { dependsOn(approveRelease) }
      tasks.named("finalSetup") { dependsOn(approveRelease) }
      tasks.named("release") { mustRunAfter(approveRelease) }
    }
  }
}
