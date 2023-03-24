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

import java.util.function.Supplier;

import javax.inject.Inject;

import org.gradle.api.internal.AbstractNamedDomainObjectContainer;
import org.gradle.api.internal.CollectionCallbackActionDecorator;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.reflect.TypeOf;
import org.gradle.internal.reflect.Instantiator;

public class DefaultBootstrapManifestContainer
    extends AbstractNamedDomainObjectContainer<NamedBootstrapManifestBuilder>
    implements BootstrapManifestContainer {
  private final ObjectFactory objectFactory;

  private final Supplier<? extends DependencyResolutionServices> dependencyResolutionServicesSupplier;

  @Inject
  public DefaultBootstrapManifestContainer(
      Instantiator instantiator,
      ObjectFactory objectFactory,
      Supplier<? extends DependencyResolutionServices> dependencyResolutionServicesSupplier
  ) {
    super(
        NamedBootstrapManifestBuilder.class,
        instantiator,
        CollectionCallbackActionDecorator.NOOP
    );

    this.objectFactory = objectFactory;
    this.dependencyResolutionServicesSupplier = dependencyResolutionServicesSupplier;
  }

  @Override
  public TypeOf<?> getPublicType() {
    return TypeOf.typeOf(BootstrapManifestContainer.class);
  }

  @Override
  protected NamedBootstrapManifestBuilder doCreate(String name) {
    return objectFactory.newInstance(DefaultBootstrapManifestBuilder.class, name, dependencyResolutionServicesSupplier);
  }
}
