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

package ws.gross.gradle.bootstrap;

import org.gradle.api.Action;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

public interface BootstrapManifestBuilder {
  // @Nonnull
  Property<String> getDescription();

  // @Nonnull
  Property<VersionConstraint> getVersion();

  // @Nonnull
  ListProperty<String> getPluginIds();

  // @Nonnull
  MapProperty<String, String> getCatalogs();

  void from(Object dependencyNotation);

  void from(Object dependencyNotation, Action<? super MutableVersionConstraint> versionSpec);

  void version(String version);

  void version(Action<? super MutableVersionConstraint> versionSpec);

  void catalog(String alias, String groupAndModule);

  void plugin(String pluginId);
}
