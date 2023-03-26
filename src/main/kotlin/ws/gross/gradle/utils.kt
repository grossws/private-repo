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

package ws.gross.gradle

import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderConvertible
import org.gradle.plugin.use.PluginDependency

fun DependencyHandler.plugin(id: String, version: String) = create("$id:$id.gradle.plugin:$version")

fun DependencyHandler.plugin(plugin: Provider<PluginDependency>) = plugin.get().run {
  plugin(pluginId, version.displayName)
}

fun DependencyHandler.plugin(plugin: ProviderConvertible<PluginDependency>) = plugin.asProvider().get().run {
  plugin(pluginId, version.displayName)
}

fun VersionCatalog.getVersion(alias: String) =
  findVersion(alias).orElseThrow { NoSuchElementException("version $alias not found in the version catalog $name") }

fun VersionCatalog.getPlugin(alias: String) =
  findPlugin(alias).orElseThrow { NoSuchElementException("plugin $alias not found in the version catalog $name") }

fun VersionCatalog.getLibrary(alias: String) =
  findLibrary(alias).orElseThrow { NoSuchElementException("library $alias not found in the version catalog $name") }

fun VersionCatalog.getBundle(alias: String) =
  findBundle(alias).orElseThrow { NoSuchElementException("bundle $alias not found in the version catalog $name") }
