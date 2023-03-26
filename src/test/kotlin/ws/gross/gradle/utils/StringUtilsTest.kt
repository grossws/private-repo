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

import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ws.gross.gradle.capitalize
import ws.gross.gradle.utils.StringUtils.*

class StringUtilsTest {
  @Test
  fun `null and empty to snake_case`() {
    assertThat(toSnakeCase(null)).isNull()
    assertThat(toSnakeCase("")).isNotNull().isEmpty()
  }

  @ParameterizedTest
  @CsvSource(
    "test           , test            ",
    "TEST           , test            ",
    "a_test         , a_test          ",
    "aTest          , a_test          ",
    "ATest          , atest           ",
    "anotherTest    , another_test    ",
    "AnotherTest    , another_test    ",
    "TestVersion2.0 , test_version_2.0",
    "someATTRTest   , some_attrtest   ",
  )
  fun `normal to snake_case`(from: String, expected: String) {
    assertThat(toSnakeCase(from)).isNotNull().isEqualTo(expected)
  }

  @Test
  fun `null and empty to lower camelCase`() {
    assertThat(toLowerCamelCase(null)).isNull()
    assertThat(toLowerCamelCase("")).isNotNull().isEmpty()
  }

  @Test
  fun `null and empty to upper CamelCase`() {
    assertThat(toUpperCamelCase(null)).isNull()
    assertThat(toUpperCamelCase("")).isNotNull().isEmpty()
  }

  @ParameterizedTest
  @CsvSource(
    "test   , test ",
    "a_test , aTest",
    "a-test , aTest",
    "a.test , aTest",
    "A_TEST , aTest",
  )
  fun `normal to camelCase`(from: String, expected: String) {
    assertThat(toLowerCamelCase(from)).isNotNull().isEqualTo(expected)
    assertThat(toUpperCamelCase(from)).isNotNull().isEqualTo(expected.capitalize())
  }
}
