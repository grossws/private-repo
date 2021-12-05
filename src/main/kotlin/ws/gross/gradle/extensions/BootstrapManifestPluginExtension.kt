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

package ws.gross.gradle.extensions

import org.gradle.api.Action
import org.gradle.api.internal.artifacts.DependencyResolutionServices
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.*
import ws.gross.gradle.bootstrap.BootstrapManifestBuilder
import ws.gross.gradle.bootstrap.DefaultBootstrapManifestBuilder
import java.util.function.Supplier
import javax.inject.Inject

interface BootstrapManifestPluginExtension {
  fun bootstrapManifest(spec: Action<in BootstrapManifestBuilder>)
}

open class DefaultBootstrapManifestPluginExtension @Inject constructor(
  objects: ObjectFactory,
  dependencyResolutionServices: Supplier<DependencyResolutionServices>,
) : BootstrapManifestPluginExtension {
  internal val bootstrapManifest: DefaultBootstrapManifestBuilder =
    objects.newInstance("manifest", dependencyResolutionServices)

  override fun bootstrapManifest(spec: Action<in BootstrapManifestBuilder>) {
    spec.execute(bootstrapManifest)
  }
}
