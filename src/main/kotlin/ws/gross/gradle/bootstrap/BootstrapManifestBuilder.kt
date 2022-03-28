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

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Named
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.MutableVersionConstraint
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.attributes.Category
import org.gradle.api.internal.artifacts.DependencyResolutionServices
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.*
import ws.gross.gradle.parseList
import ws.gross.gradle.parseMap
import ws.gross.gradle.toCamelCase
import java.util.Properties
import java.util.function.Supplier
import javax.inject.Inject

interface BootstrapManifestBuilder {
  val description: Property<String>
  val version: Property<VersionConstraint>
  val pluginIds: ListProperty<String>
  val catalogs: MapProperty<String, String>

  fun from(dependencyNotation: Any)
  fun from(dependencyNotation: Any, versionSpec: Action<in MutableVersionConstraint>)

  fun version(version: String)
  fun version(versionSpec: Action<in MutableVersionConstraint>)

  fun catalog(alias: String, groupAndModule: String)
  fun plugin(pluginId: String)
}

interface NamedBootstrapManifestBuilder: BootstrapManifestBuilder, Named

open class DefaultBootstrapManifestBuilder @Inject constructor(
  private val name: String,
  private val objects: ObjectFactory,
  private val dependencyResolutionServices: Supplier<DependencyResolutionServices>,
) : NamedBootstrapManifestBuilder {
  override fun getName(): String = name

  override val description: Property<String> = objects.property()

  override val version: Property<VersionConstraint> = objects.property()

  override val pluginIds: ListProperty<String> = objects.listProperty()

  override val catalogs: MapProperty<String, String> = objects.mapProperty()

  override fun from(dependencyNotation: Any) = from(dependencyNotation) {}

  override fun from(dependencyNotation: Any, versionSpec: Action<in MutableVersionConstraint>) {
    val drs = dependencyResolutionServices.get()

    val configurationName = "incomingBootstrapManifestFor${name.toCamelCase().capitalize()}"
    val cnf = drs.configurationContainer.create(configurationName) {
      resolutionStrategy.activateDependencyLocking()
      isCanBeConsumed = false
      isCanBeResolved = true
      attributes {
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named("manifest"))
      }
    }

    val dep = drs.dependencyHandler.create(dependencyNotation)
    if (dep is ExternalDependency) {
      dep.version(versionSpec)
    }

    cnf.dependencies.add(dep)

    cnf.incoming.artifacts.artifacts.forEach { ar ->
      if (!ar.file.exists()) {
        throw GradleException("Import of external bootstrap manifest failed: ${ar.file} doesn't exist")
      }

      val props = Properties().apply { ar.file.reader().use { load(it) } }
      description.set(props.getProperty("description", ""))
      version(props.getProperty("version"))
      props.getProperty("catalogIds").parseMap().forEach(::catalog)
      props.getProperty("pluginIds").parseList().forEach(::plugin)
    }
  }

  override fun version(version: String) {
    this.version.set(DefaultMutableVersionConstraint(version).asImmutable())
  }

  override fun version(versionSpec: Action<in MutableVersionConstraint>) {
    version.set(DefaultMutableVersionConstraint("").also { versionSpec.execute(it) }.asImmutable())
  }

  override fun catalog(alias: String, groupAndModule: String) {
    catalogs.put(alias, groupAndModule)
  }

  override fun plugin(pluginId: String) {
    pluginIds.add(pluginId)
  }
}
