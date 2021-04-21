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
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.kotlin.dsl.*

class NexusPublishPlugin : Plugin<Project> {
  private lateinit var conf: NexusConfiguration

  override fun apply(project: Project): Unit = project.run {
    pluginManager.apply("maven-publish")
    val publishing = the<PublishingExtension>()

    conf = NexusConfiguration.from(project.providers)

    val releasesRepo = providers.gradleProperty("nexusReleasesRepo").orElse("releases")
    val snapshotsRepo = providers.gradleProperty("nexusSnapshotsRepo").orElse("snapshots")

    publishing.repositories {
      maven(RELEASES_REPO_NAME, conf.repoUrl(releasesRepo), conf.credentials)
      maven(SNAPSHOTS_REPO_NAME, conf.repoUrl(snapshotsRepo), conf.credentials)
    }

    tasks.withType<PublishToMavenRepository>().configureEach {
      val repositoryName = PublishTaskInfo.from(this)?.repository ?: return@configureEach

      val repos = publishing.repositories
      if (repositoryName in listOf(RELEASES_REPO_NAME, SNAPSHOTS_REPO_NAME)) {
        onlyIf {
          val vi = version.toString().parseVersionInfo()
          vi ?: logger.warn("Can't parse project version $version")
          vi ?: return@onlyIf false

          (repository == repos[RELEASES_REPO_NAME] && vi.release)
              || (repository == repos[SNAPSHOTS_REPO_NAME] && !vi.release)
        }
      }
    }
  }
}
