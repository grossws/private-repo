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
  kotlin("jvm")
  idea
}

apply<org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin>()
configure<org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension> {
  annotation(HasImplicitReceiver::class.qualifiedName!!)
}

dependencies {
  implementation(platform(kotlin("bom")))

  implementation(gradleKotlinDsl())
  implementation(kotlin("stdlib-jdk8"))

  testImplementation(platform("org.junit:junit-bom:5.7.1"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

  testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "11"
    languageVersion = "1.4"
    apiVersion = "1.4"
  }
}

val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest")
configurations[functionalTestSourceSet.implementationConfigurationName]
  .extendsFrom(configurations["testImplementation"])
configurations[functionalTestSourceSet.runtimeOnlyConfigurationName]
  .extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath

  shouldRunAfter("test")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks.named("check") {
  dependsOn(functionalTest)
}

pluginManager.withPlugin("idea") {
  the<IdeaModel>().module {
    val kotlinSourceSet = kotlin.sourceSets[functionalTestSourceSet.name]

    testSourceDirs = testSourceDirs + kotlinSourceSet.kotlin.sourceDirectories
    testResourceDirs = testResourceDirs + kotlinSourceSet.resources.sourceDirectories

    scopes["TEST"]?.apply {
      val plus = computeIfAbsent("plus") { mutableListOf<Configuration>() }
      plus.add(configurations[functionalTestSourceSet.compileClasspathConfigurationName])
      plus.add(configurations[functionalTestSourceSet.runtimeClasspathConfigurationName])
    }
  }
}
