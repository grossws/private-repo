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

import org.gradle.plugins.ide.idea.model.IdeaModel

plugins {
  id("org.gradle.kotlin.kotlin-dsl")

  id("com.gradle.plugin-publish")
  id("nebula.release")
}

tasks.withType<JavaCompile>().configureEach { options.release.set(8) }

dependencyLocking {
  lockAllConfigurations()
}

@Suppress("UnstableApiUsage")
testing {
  suites {
    val test by getting(JvmTestSuite::class)
    val functionalTest by creating(JvmTestSuite::class) {
      targets.all {
        testTask.configure { shouldRunAfter(test) }
      }
    }

    tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) { dependsOn(functionalTest) }
    gradlePlugin { testSourceSets(functionalTest.sources) }
  }
}

tasks.named<Jar>("jar") {
  manifest {
    attributes(
      "Implementation-Title" to "ws.gross.private-repo Gradle plugins",
      "Implementation-Version" to project.version,
    )
  }
}

@Suppress("UnstableApiUsage")
pluginManager.withPlugin("idea") {
  val functionalTest by testing.suites.getting(JvmTestSuite::class)
  the<IdeaModel>().module {
    val sourceSet = functionalTest.sources
    // using legacy setters since IDEA ignores new ones
    testSourceDirs = testSourceDirs + sourceSet.allSource.sourceDirectories
    testResourceDirs = testResourceDirs + sourceSet.resources.sourceDirectories
  }
}
