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
import assertk.assertions.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class VersionInfoTest {
  @ParameterizedTest
  @ValueSource(strings = ["1.2.3", "1.2.3-rc.1", "1.2.3-dev.1+deadbee", "1.2.3-dev.1.uncommitted+deadbee", "1.2.3-dev.1+feature.flag.deadbee", "1.2.3-dev.1.uncommitted+feature.flag.deadbee"])
  fun `versions parsed successfully`(version: String) {
    assertThat(version.parseVersionInfo()).isNotNull().all {
      prop("major") { it.major }.isEqualTo(1)
      prop("minor") { it.minor }.isEqualTo(2)
      prop("patch") { it.patch }.isEqualTo(3)
      prop("release") { it.release }.isNotEqualTo(version.contains("-dev."))
      prop("significant") { it.significant }.all {
        if (version == "1.2.3") isEqualTo("final")
        if (version.contains("-rc.")) isEqualTo("rc")
        if (version.contains("-dev.")) isEqualTo("dev")
      }
      prop("iteration") { it.iteration }.all {
        if (version.contains("-rc.") || version.contains("-dev.")) isEqualTo(1) else isNull()
      }
      prop("dirty") { it.dirty }.isEqualTo(version.contains(".uncommitted+"))
      prop("feature") { it.feature }.all {
        if (version.contains("+feature.")) isEqualTo("feature.flag") else isNull()
      }
      prop("hash") { it.hash }.all {
        if (version.contains("-dev")) isEqualTo("deadbee") else isNull()
      }
    }
  }
}
