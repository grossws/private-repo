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

import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.prop
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import ws.gross.gradle.utils.PublishTaskInfo
import java.util.stream.Stream

class PublishTaskInfoTest {
  companion object {
    fun Pair<String, String>.toPublishTaskName() =
      "publish${first.capitalize()}PublicationTo${second.capitalize()}Repository"

    @JvmStatic
    fun data(): Stream<Arguments> = listOf(
      "pluginsMaven" to "local",
      "org.anenerbe.gradlePluginMarker" to "gradleReleasesRepo"
    ).map { arguments(it.toPublishTaskName(), it.first, it.second) }.stream()
  }

  @ParameterizedTest
  @MethodSource("data")
  fun `publish task name parsed correctly`(taskName: String, publication: String, repository: String) {
    assertThat(PublishTaskInfo.of(taskName).orElse(null)).isNotNull().all {
      prop("publication") { it.publication }.isEqualTo(publication)
      prop("repository") { it.repository }.isEqualTo(repository)
    }
  }

  @Test
  fun `repository name always starts with lowercase`() {
    val pair = "some" to "MavenRepo"
    assertThat(PublishTaskInfo.of(pair.toPublishTaskName()).orElse(null)).isNotNull().all {
      prop("publication") { it.publication }.isEqualTo(pair.first)
      prop("repository") { it.repository }.isEqualTo(pair.second.decapitalize())
    }
  }
}
