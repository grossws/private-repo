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

package ws.gross.gradle.impl;

import java.util.Collections;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import ws.gross.gradle.BootstrapPlugin;
import ws.gross.gradle.utils.NexusConfiguration;
import ws.gross.gradle.PrivateRepoBasePlugin;
import ws.gross.gradle.PrivateRepoPlugin;
import ws.gross.gradle.utils.GradleUtils;

import static org.gradle.api.artifacts.ArtifactRepositoryContainer.DEFAULT_MAVEN_CENTRAL_REPO_NAME;
import static org.gradle.api.internal.artifacts.dsl.DefaultRepositoryHandler.GRADLE_PLUGIN_PORTAL_REPO_NAME;
import static ws.gross.gradle.utils.GradleUtils.maven;
import static ws.gross.gradle.utils.NexusConfiguration.NEXUS_REPO_NAME;

public class PrivateRepoPluginImpl implements Plugin<Settings> {
  private static final Logger logger = Logging.getLogger(PrivateRepoPluginImpl.class);

  private Settings settings;
  private NexusConfiguration conf;
  private Provider<String> repo;

  @Override
  public void apply(Settings settings) {
    settings.getPluginManager().apply(PrivateRepoBasePlugin.class);

    this.settings = settings;

    @SuppressWarnings("UnstableApiUsage")
    ProviderFactory providers = settings.getProviders();
    conf = NexusConfiguration.from(providers);
    repo = providers.gradleProperty("nexusRepo").orElse("public");

    if (!conf.getBaseUrl().isPresent()) {
      throw new GradleException("nexusUrl should be defined in gradle properties");
    }

    configurePluginRepos();
    configureRepos();

    // apply only after repositories are configured
    settings.getPluginManager().apply(BootstrapPlugin.class);
  }

  private void configurePluginRepos() {
    settings.getPluginManagement().repositories(rh -> {
      rh.removeIf(r -> r.getName().equals(GRADLE_PLUGIN_PORTAL_REPO_NAME));

      Provider<String> repoUrl = conf.repoUrl(repo);
      logger.info("Adding {}({}) to pluginManagement", NEXUS_REPO_NAME, repoUrl.get());
      maven(rh, NEXUS_REPO_NAME, repoUrl, conf.getCredentials());

      logger.info("Adding mavenCentral to pluginManagement");
      rh.mavenCentral();

      logger.info("Adding gradlePluginPortal to pluginManagement");
      rh.gradlePluginPortal();
    });
  }

  @SuppressWarnings("UnstableApiUsage")
  private void configureRepos() {
    ProviderFactory providers = settings.getProviders();

    List<String> groups = providers.gradleProperty("nexusGroups")
        .map(GradleUtils::parseList)
        .orElse(Collections.emptyList())
        .get();

    List<String> groupRegexes = providers.gradleProperty("nexusGroupRegexes")
        .map(GradleUtils::parseList)
        .orElse(conf.getDefaultGroupRegex())
        .get();

    settings.getDependencyResolutionManagement().repositories(rh -> {
      logger.info("Adding mavenCentral to dependencyResolutionManagement");
      if (rh.findByName(DEFAULT_MAVEN_CENTRAL_REPO_NAME) == null) {
        rh.mavenCentral(r -> {
          r.content(cd -> {
            groups.forEach(cd::excludeGroup);
            groupRegexes.forEach(cd::excludeGroupByRegex);
          });
        });
      }

      Provider<String> repoUrl = conf.repoUrl(repo);
      boolean exclusive = providers.gradleProperty("nexusExclusive")
          .map(Boolean::valueOf)
          .orElse(false)
          .get();

      if (exclusive) {
        logger.info("Adding exclusive {}({}) to dependencyResolutionManagement", NEXUS_REPO_NAME, repoUrl.get());
        rh.exclusiveContent(ecr -> {
          ecr.forRepository(() -> maven(rh, NEXUS_REPO_NAME, repoUrl, conf.getCredentials()));
          ecr.filter(cd -> {
            groups.forEach(cd::includeGroup);
            groupRegexes.forEach(cd::includeGroupByRegex);
          });
        });
      } else {
        logger.info("Adding {}({}) to dependencyResolutionManagement", NEXUS_REPO_NAME, repoUrl.get());
        maven(rh, NEXUS_REPO_NAME, repoUrl, conf.getCredentials());
      }
    });
  }

  public static String getPluginVersion() {
    return PrivateRepoPlugin.class.getPackage().getImplementationVersion();
  }
}
