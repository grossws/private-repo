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

import org.gradle.api.Action
import org.gradle.api.initialization.Settings
import ws.gross.gradle.PrivateRepoPlugin
import ws.gross.gradle.extensions.PrivateRepoExtension

val Settings.privateRepo: PrivateRepoExtension
  get() = extensions.getByType(PrivateRepoExtension::class.java)

fun Settings.privateRepo(spec: Action<in PrivateRepoExtension>) =
  spec.execute(privateRepo)

val PrivateRepoPlugin.pluginVersion: String
  get() = PrivateRepoPlugin.getPluginVersion()
