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
