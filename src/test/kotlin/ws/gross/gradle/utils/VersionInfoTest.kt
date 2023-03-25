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

package ws.gross.gradle.utils

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class VersionInfoTest {
  companion object {
    private val WITH_EXPLICIT_TYPE = mutableSetOf<String>().apply {
      addAll(VersionInfo.RELEASE_TYPES)
      remove("final")
      addAll(listOf("dev", "milestone"))
    }
  }

  @ParameterizedTest
  @CsvSource(
    "1.2.3,                                        true,  final     ",
    "1.2.3-rc.1,                                   true,  rc        ",
    "1.2.3-beta.1,                                 true,  beta      ",
    "1.2.3-alpha.1,                                true,  alpha     ",
    "1.2.3-milestone.1,                            false, milestone ",
    "1.2.3-dev.1+deadbee,                          false, dev       ",
    "1.2.3-dev.1.uncommitted+deadbee,              false, dev       ",
    "1.2.3-dev.1+feature.flag.deadbee,             false, dev       ",
    "1.2.3-dev.1.uncommitted+feature.flag.deadbee, false, dev       ",
  )
  fun `versions parsed successfully`(version: String, release: Boolean, significant: String) {
    assertThat(VersionInfo.of(version).orElse(null)).isNotNull().all {
      prop("major") { it.major }.isEqualTo(1)
      prop("minor") { it.minor }.isEqualTo(2)
      prop("patch") { it.patch }.isEqualTo(3)
      prop("release") { it.isRelease }.isEqualTo(release)
      prop("significant") { it.significant }.isEqualTo(significant)
      prop("iteration") { it.iteration }.all {
        if (WITH_EXPLICIT_TYPE.any { version.contains("-$it.") }) isEqualTo(1) else isNull()
      }
      prop("dirty") { it.isDirty }.isEqualTo(version.contains(".uncommitted+"))
      prop("feature") { it.feature }.all {
        if (version.contains("+feature.")) isEqualTo("feature.flag") else isNull()
      }
      prop("hash") { it.hash }.all {
        if (version.contains("-dev.")) isEqualTo("deadbee") else isNull()
      }
    }
  }
}
