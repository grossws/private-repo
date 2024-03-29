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

plugins {
  id("com.gradle.plugin-publish")
  id("com.netflix.nebula.release")
}

tasks.withType<JavaCompile>().configureEach { options.release.set(8) }
pluginManager.withPlugin("org.jetbrains.kotlin.jvm", KotlinConfigureAction(project))

dependencyLocking {
  lockAllConfigurations()
}

dependencies {
  compileOnly(kotlin("stdlib-jdk8", embeddedKotlinVersion))
}

@Suppress("UnstableApiUsage")
testing {
  suites {
    val libs = project.the<VersionCatalogsExtension>().named("libs")
    val junitVersion = libs.findVersion("junit")
      .orElseThrow { NoSuchElementException("junit version not found in the version catalog")  }
      .requiredVersion

    val test by getting(JvmTestSuite::class) {
      useJUnitJupiter(junitVersion)
    }

    val functionalTest by creating(JvmTestSuite::class) {
      useJUnitJupiter(junitVersion)

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

publishing {
  repositories.maven {
    name = "local"
    setUrl(rootProject.layout.buildDirectory.dir("repo"))
  }
}
