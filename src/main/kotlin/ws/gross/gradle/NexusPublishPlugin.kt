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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.PluginInstantiationException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.*

class NexusPublishPlugin : Plugin<Project> {
  companion object {
    const val RELEASES_REPO_NAME = "nexusReleases"
    const val SNAPSHOTS_REPO_NAME = "nexusSnapshots"
  }

  private lateinit var rh: RepoHelper

  override fun apply(project: Project): Unit = project.run {
    plugins.apply("maven-publish")

    val nexusUrl: String? by project
    rh = RepoHelper(nexusUrl ?: throw PluginInstantiationException("nexusUrl should be defined in gradle properties"))

    val publishing = the<PublishingExtension>()
    publishing.repositories {
      val nexusUsername: String by project
      val nexusPassword: String by project
      val withAuth: Action<in MavenArtifactRepository> = Action {
        authentication.create<BasicAuthentication>("basic")
        credentials {
          username = nexusUsername
          password = nexusPassword
        }
      }

      val nexusReleasesRepo: String? by project
      findOrCreateNexusRepo(rh, RELEASES_REPO_NAME, nexusReleasesRepo ?: "releases", withAuth)

      val nexusSnapshotsRepo: String? by project
      findOrCreateNexusRepo(rh, SNAPSHOTS_REPO_NAME, nexusSnapshotsRepo ?: "snapshots", withAuth)
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
      val repos = publishing.repositories
      onlyIf {
        val nexusReleaseVersionRegex: String? by project
        // release repo used for 1.2, 1.2.3, 5.6-rc.7, 5.6.7-rc.8
        val versionRegex = nexusReleaseVersionRegex ?: """\d+\.\d+(?:\.\d+)?(?:-rc\.\d+)?"""
        val release = version.toString().matches(versionRegex.toRegex())
        (repository == repos[RELEASES_REPO_NAME] && release) || (repository == repos[SNAPSHOTS_REPO_NAME] && !release)
      }
    }
  }
}
