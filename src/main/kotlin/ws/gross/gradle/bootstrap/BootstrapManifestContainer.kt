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

package ws.gross.gradle.bootstrap

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.internal.AbstractNamedDomainObjectContainer
import org.gradle.api.internal.CollectionCallbackActionDecorator
import org.gradle.api.internal.artifacts.DependencyResolutionServices
import org.gradle.api.model.ObjectFactory
import org.gradle.api.reflect.TypeOf
import org.gradle.internal.reflect.Instantiator
import org.gradle.kotlin.dsl.*
import java.util.function.Supplier
import javax.inject.Inject

interface BootstrapManifestContainer : NamedDomainObjectContainer<NamedBootstrapManifestBuilder>

open class DefaultBootstrapManifestContainer @Inject constructor(
  instantiator: Instantiator,
  private val objects: ObjectFactory,
  private val dependencyResolutionServices: Supplier<DependencyResolutionServices>,
) : AbstractNamedDomainObjectContainer<NamedBootstrapManifestBuilder>(
  NamedBootstrapManifestBuilder::class.java,
  instantiator,
  CollectionCallbackActionDecorator.NOOP
), BootstrapManifestContainer {
  override fun doCreate(name: String): NamedBootstrapManifestBuilder =
    objects.newInstance<DefaultBootstrapManifestBuilder>(name, dependencyResolutionServices)

  override fun getPublicType(): TypeOf<*> = TypeOf.typeOf(BootstrapManifestContainer::class.java)
}
