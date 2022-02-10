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

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.github.syari.kgit.KGit
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.*
import ws.gross.gradle.tasks.ReleaseApproveTask

class ReleaseApprovePluginTest {
  companion object {
    fun baseProject(withNebulaRelease: Boolean = true): Project = ProjectBuilder.builder()
      .withName("test")
      .build()
      .also {
        it.pluginManager.apply("ws.gross.release-approve")
        if (withNebulaRelease) {
          // git repo required for nebula.release plugin
          KGit.init { setDirectory(it.projectDir) }
          it.pluginManager.apply("nebula.release")
        }
      }
  }

  private val project = baseProject()

  @Test
  fun `plugin present`() {
    assertThat(project.plugins.findPlugin("ws.gross.release-approve"))
      .isNotNull()
      .isInstanceOf(ReleaseApprovePlugin::class)
  }

  @Test
  fun `task present`() {
    assertThat(project.tasks.findByName("approveRelease"))
      .isNotNull()
      .isInstanceOf(ReleaseApproveTask::class)
  }

  @Test
  fun `task absent without nebula release plugin`() {
    assertThat(baseProject(false).tasks.findByName("approveRelease"))
      .isNull()
  }
}
