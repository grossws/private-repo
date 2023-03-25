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

import org.gradle.api.Action;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import ws.gross.gradle.bootstrap.NamedBootstrapManifestBuilder;
import ws.gross.gradle.extensions.PrivateRepoExtension;

public class BootstrapManifestAction implements Action<Settings> {
  private static final Logger logger = Logging.getLogger(BootstrapManifestAction.class);

  private final String name;

  public BootstrapManifestAction(String name) {
    this.name = name;
  }

  @Override
  @SuppressWarnings("UnstableApiUsage")
  public void execute(Settings settings) {
    PrivateRepoExtension ext = settings.getExtensions().getByType(PrivateRepoExtension.class);
    NamedBootstrapManifestBuilder manifest = ext.getManifests().getByName(name);
    logger.info("Adding bootstrap manifest {}{}", name,
        manifest.getDescription().map(d -> ": " + d).getOrElse(""));

    String version = manifest.getVersion().get().getRequiredVersion();

    settings.getPluginManagement().plugins(ps -> {
      manifest.getPluginIds().get().forEach(pluginId -> {
        logger.info("Adding plugin {} {}", pluginId, version);
        ps.id(pluginId).version(version);
      });
    });

    settings.dependencyResolutionManagement(drm -> {
      drm.versionCatalogs(vc -> {
        manifest.getCatalogs().get().forEach((alias, dependencyNotation) -> {
          String dependencyNotationWithVersion = dependencyNotation + ":" + version;
          logger.info("Adding catalog {} -> {}", alias, dependencyNotationWithVersion);
          vc.create(alias, c -> c.from(dependencyNotationWithVersion));
        });
      });
    });
  }
}
