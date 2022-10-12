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

import org.gradle.kotlin.dsl.support.expectedKotlinDslPluginsVersion

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(plugin("org.gradle.kotlin.kotlin-dsl", expectedKotlinDslPluginsVersion))
  implementation(plugin(libs.plugins.gradle.publish))
  implementation(plugin(libs.plugins.nebula.release))
}

dependencyLocking {
  lockAllConfigurations()
}

kotlinDslPluginOptions {
  jvmTarget.set("11")
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}

fun DependencyHandler.plugin(id: String, version: String) = create("$id:$id.gradle.plugin:$version")

fun DependencyHandler.plugin(plugin: Provider<PluginDependency>) = plugin.get().run {
  plugin(pluginId, version.displayName)
}
