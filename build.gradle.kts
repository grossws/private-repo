plugins {
  id("plugin-conventions")
}

gradlePlugin {
  plugins.create("private-repo") {
    id = "ws.gross.private-repo"
    displayName = "Plugin for private repository configuration"
    description = """
      Settings plugin to configure dependencyResolutionManagement and pluginManagement
      to use private Nexus/Artifactory repository with auth and convenient defaults.
    """.trimIndent()
    implementationClass = "ws.gross.gradle.NexusPlugin"
  }
}

pluginBundle {
  website = "https://github.com/grossws/gradle-private-repo-plugin"
  vcsUrl = "https://github.com/grossws/gradle-private-repo-plugin.git"
  tags = listOf("repository", "private-repository", "nexus", "artifactory")
}

dependencies {
  testImplementation(platform("com.fasterxml.jackson:jackson-bom:2.12.2"))
  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
}
