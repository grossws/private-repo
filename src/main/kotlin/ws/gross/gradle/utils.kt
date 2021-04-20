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

package ws.gross.gradle

internal fun String?.parseList(): List<String> =
  (this ?: "").split(',').map { it.trim() }.filterNot { it.isEmpty() }

internal fun String.parsePair(): Pair<String, String> =
  split('=', limit = 2).map { it.trim() }.let { it[0] to it[1] }

internal fun String?.parseMap(): Map<String, String> =
  parseList().associate { it.parsePair() }
