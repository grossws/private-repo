/*
 * Copyright 2023 Konstantin Gribov
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

package ws.gross.gradle;

import java.util.Optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository;
import ws.gross.gradle.utils.GradleUtils;

import static ws.gross.gradle.Gradle_utilsKt.parsePublishTaskInfo;
import static ws.gross.gradle.RepositoriesKt.*;
import static ws.gross.gradle.Version_utilsKt.parseVersionInfo;

public class PrivateRepoPublishPlugin implements Plugin<Project> {
  private NexusConfiguration conf;

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply("maven-publish");
    PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);

    ProviderFactory providers = project.getProviders();
    conf = NexusConfiguration.Companion.from(providers);

    Provider<String> releasesRepo = providers.gradleProperty("nexusReleasesRepo").orElse("releases");
    Provider<String> snapshotsRepo = providers.gradleProperty("nexusSnapshotsRepo").orElse("snapshots");

    publishing.repositories(rh -> {
      maven(rh, RELEASES_REPO_NAME, conf.repoUrl(releasesRepo), conf.getCredentials(), GradleUtils.doNothing());
      maven(rh, SNAPSHOTS_REPO_NAME, conf.repoUrl(snapshotsRepo), conf.getCredentials(), GradleUtils.doNothing());
    });

    project.getTasks().withType(PublishToMavenRepository.class).configureEach(t -> {
      String repositoryName = Optional.ofNullable(parsePublishTaskInfo(t.getName())).map(PublishTaskInfo::getRepository).orElse(null);
      if (repositoryName == null) {
        return;
      }

      RepositoryHandler repos = publishing.getRepositories();
      if (repositoryName.equals(RELEASES_REPO_NAME) || repositoryName.equals(SNAPSHOTS_REPO_NAME)) {
        t.onlyIf(s -> {
          VersionInfo versionInfo = parseVersionInfo(project.getVersion().toString());
          if (versionInfo == null) {
            project.getLogger().warn("Can't parse project version {}", project.getVersion());
            return false;
          }

          return (t.getRepository() == repos.getByName(RELEASES_REPO_NAME) && versionInfo.getRelease())
                 || (t.getRepository() == repos.getByName(SNAPSHOTS_REPO_NAME) && !versionInfo.getRelease());
        });
      }
    });
  }
}
