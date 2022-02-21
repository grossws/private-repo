/*
 * Copyright 2022 Konstantin Gribov
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

import assertk.Assert
import assertk.assertions.*
import assertk.assertions.support.appendName
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskOutcome
import java.io.StringReader
import java.util.Properties

fun Assert<BuildResult>.task(name: String): Assert<BuildTask> = prop("task[$name]") { it.task(name) }
  .isNotNull()

fun Assert<BuildTask>.isSuccess() = prop("outcome") { it.outcome }.isEqualTo(TaskOutcome.SUCCESS)
fun Assert<BuildTask>.isFailed() = prop("outcome") { it.outcome }.isEqualTo(TaskOutcome.FAILED)
fun Assert<BuildTask>.isUpToDate() = prop("outcome") { it.outcome }.isEqualTo(TaskOutcome.UP_TO_DATE)
fun Assert<BuildTask>.isSkipped() = prop("outcome") { it.outcome }.isEqualTo(TaskOutcome.SKIPPED)
fun Assert<BuildTask>.isFromCache() = prop("outcome") { it.outcome }.isEqualTo(TaskOutcome.FROM_CACHE)
fun Assert<BuildTask>.isNoSource() = prop("outcome") { it.outcome }.isEqualTo(TaskOutcome.NO_SOURCE)

fun Assert<BuildResult>.output() = prop("output") { it.output.lines() }

fun Assert<BuildResult>.reusedConfigurationCache() = output().any {
  it.contains("reusing configuration cache", ignoreCase = true)
}

fun Assert<String>.asProperties() = transform { text ->
  Properties().apply { load(StringReader(text)) }
}

fun Assert<Properties>.key(key: String) = transform(appendName(show(key, "[]"))) {
  if (it.containsKey(key)) {
    it.getProperty(key)
  } else {
    expected("to have key:${show(key)}")
  }
}
