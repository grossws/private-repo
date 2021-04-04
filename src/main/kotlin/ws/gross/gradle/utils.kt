package ws.gross.gradle

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.authentication.http.BasicAuthentication
import org.gradle.kotlin.dsl.*
import java.net.URI

internal fun RepositoryHandler.findOrCreateNexusRepo(
  baseUrl: String?,
  repoName: String,
  repoPath: String,
  configuration: MavenArtifactRepository.() -> Unit
): ArtifactRepository = findByName(repoName) ?: maven {
  name = repoName
  url = URI.create("$baseUrl/repository/$repoPath")
  configuration()
}

internal fun MavenArtifactRepository.withAuth() {
  credentials(PasswordCredentials::class.java)
  authentication.create<BasicAuthentication>("basic")
}

internal fun String?.parseList(): List<String> =
  (this ?: "").split(',').map { it.trim() }.filterNot { it.isEmpty() }

internal fun String?.parseSwitch(defaultValue: Boolean): Boolean =
  when ((this ?: defaultValue.toString()).trim().toLowerCase()) {
    "true" -> true
    "on" -> true
    "false" -> false
    "off" -> false
    else -> {
      throw IllegalArgumentException("can't parse $this to boolean: use true/false or on/off")
    }
  }

