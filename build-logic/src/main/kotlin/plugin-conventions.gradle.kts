plugins {
  id("kotlin-conventions")
  `java-gradle-plugin`
  `maven-publish`
  id("com.gradle.plugin-publish") //`version "0.14.0"
}

gradlePlugin {
  testSourceSets(sourceSets["functionalTest"])
}
