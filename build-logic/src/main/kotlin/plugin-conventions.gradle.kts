plugins {
  id("kotlin-conventions")
  `java-gradle-plugin`
  `maven-publish`
  id("com.gradle.plugin-publish")
  id("nebula.release")
}

gradlePlugin {
  testSourceSets(sourceSets["functionalTest"])
}
