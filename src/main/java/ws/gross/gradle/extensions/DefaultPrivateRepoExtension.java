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
import org.jetbrains.annotations.NotNull;
import ws.gross.gradle.bootstrap.BootstrapManifestContainer;
import ws.gross.gradle.bootstrap.DefaultBootstrapManifestContainer;

public class DefaultPrivateRepoExtension implements PrivateRepoExtension {
  private final DefaultBootstrapManifestContainer manifestContainer;

  @Inject
  public DefaultPrivateRepoExtension(ObjectFactory objectFactory, Supplier<DependencyResolutionServices> dependencyResolutionServicesSupplier) {
    manifestContainer = objectFactory.newInstance(
        DefaultBootstrapManifestContainer.class,
        dependencyResolutionServicesSupplier
    );
  }

  @NotNull
  @Override
  public BootstrapManifestContainer getManifests() {
    return manifestContainer;
  }

  @Override
  public void manifests(@Nonnull Action<? super BootstrapManifestContainer> spec) {
    spec.execute(manifestContainer);
  }
}
