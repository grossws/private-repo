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

package ws.gross.gradle.extensions;

import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.model.ObjectFactory;
import ws.gross.gradle.bootstrap.BootstrapManifestBuilder;
import ws.gross.gradle.bootstrap.DefaultBootstrapManifestBuilder;
import ws.gross.gradle.bootstrap.NamedBootstrapManifestBuilder;

public class DefaultBootstrapManifestPluginExtension implements BootstrapManifestPluginExtension {
  private final DefaultBootstrapManifestBuilder bootstrapManifestBuilder;

  @Inject
  public DefaultBootstrapManifestPluginExtension(
      @Nonnull ObjectFactory objectFactory,
      @Nonnull Supplier<? extends DependencyResolutionServices> dependencyResolutionServicesSupplier
  ) {
    bootstrapManifestBuilder = objectFactory.newInstance(
        DefaultBootstrapManifestBuilder.class,
        "manifest",
        dependencyResolutionServicesSupplier
    );
  }

  @Override
  public void bootstrapManifest(@Nonnull Action<? super BootstrapManifestBuilder> spec) {
    spec.execute(bootstrapManifestBuilder);
  }

  public NamedBootstrapManifestBuilder getBootstrapManifest() {
    return bootstrapManifestBuilder;
  }
}
