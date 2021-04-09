plugins {
  id("kotlin-conventions")
  `java-gradle-plugin`
  `maven-publish`
  id("com.gradle.plugin-publish")
}

gradlePlugin {
  testSourceSets(sourceSets["functionalTest"])
}
