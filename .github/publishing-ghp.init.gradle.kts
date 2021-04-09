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

allprojects {
  plugins.withId("maven-publish") {
    val repo = System.getenv("GITHUB_REPOSITORY")
    if (repo == null) {
      logger.lifecycle("GITHUB_REPOSITORY not set, skip publishing config")
    }

    extensions.configure<PublishingExtension>("publishing") {
      repositories.maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/$repo")

        credentials {
          username = System.getenv("MAVEN_USERNAME")
          password = System.getenv("MAVEN_PASSWORD")
        }
      }
    }
  }
}
