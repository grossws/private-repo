plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(kotlin("gradle-plugin"))
  implementation(kotlin("sam-with-receiver"))

  implementation("com.gradle.publish:plugin-publish-plugin:0.14.0")
}
