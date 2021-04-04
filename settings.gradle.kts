rootProject.name = "gradle-private-repo-plugin"

includeBuild("build-logic")

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
  repositories {
    mavenCentral()
  }
}
