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
import org.gradle.api.Project
import org.gradle.kotlin.dsl.*
import ws.gross.gradle.tasks.ReleaseApproveTask

class ReleaseApprovePlugin : Plugin<Project> {
  override fun apply(project: Project): Unit = project.run {
    pluginManager.withPlugin("nebula.release") {
      val approveRelease by tasks.registering(ReleaseApproveTask::class) {
        group = "Nebula Release"
        description = "Approve rc/final release before pushing git tag"
      }

      tasks.named("candidateSetup") { dependsOn(approveRelease) }
      tasks.named("finalSetup") { dependsOn(approveRelease) }
      tasks.named("release") { mustRunAfter(approveRelease) }
    }
  }
}
