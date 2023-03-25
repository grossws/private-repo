/*
 * Copyright 2023 Konstantin Gribov
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

package ws.gross.gradle.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;
import org.gradle.api.credentials.Credentials;
import org.gradle.api.provider.Provider;
import org.gradle.authentication.http.BasicAuthentication;
import org.gradle.internal.artifacts.repositories.AuthenticationSupportedInternal;

public class GradleUtils {
  private static final Action<?> DO_NOTHING = new NoopAction<>();
  private static final Pattern SPLIT_COMMA_PATTERN = Pattern.compile(",");
  private static final Pattern SPLIT_EQUALS_PATTERN = Pattern.compile("=");

  private GradleUtils() {
  }

  @SuppressWarnings("unchecked")
  public static <T> Action<T> doNothing() {
    return (Action<T>) DO_NOTHING;
  }

  public static MavenArtifactRepository maven(
      RepositoryHandler repositories,
      String repoName,
      Provider<String> repoUrl,
      Provider<? extends Credentials> credentials
  ) {
    return maven(repositories, repoName, repoUrl, credentials, doNothing());
  }

  public static MavenArtifactRepository maven(
      RepositoryHandler repositories,
      String repoName,
      Provider<String> repoUrl,
      Provider<? extends Credentials> credentials,
      Action<? super MavenArtifactRepository> configureAction
  ) {
    ArtifactRepository repo = repositories.findByName(repoName);
    if (repo instanceof MavenArtifactRepository) {
      return (MavenArtifactRepository) repo;
    } else if (repo != null) {
      throw new GradleException("Repository with name " + repoName + " is not a MavenArtifactRepository");
    } else {
      return repositories.maven(r -> {
        r.setName(repoName);
        r.setUrl(repoUrl);
        if (credentials.isPresent()) {
          r.authentication(a -> a.create("basic", BasicAuthentication.class));
          ((AuthenticationSupportedInternal) r).getConfiguredCredentials().set(credentials);
        }
        configureAction.execute(r);
      });
    }
  }

  public static Properties readProperties(Path path) {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      Properties result = new Properties();
      result.load(reader);
      return result;
    } catch (IOException e) {
      throw new GradleException("Failed to read " + path, e);
    }
  }

  public static List<String> parseList(@Nullable String value) {
    return SPLIT_COMMA_PATTERN.splitAsStream(value == null ? "" : value)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
  }

  public static Map<String, String> parseMap(@Nullable String value) {
    return parseList(value)
        .stream()
        .map(p -> {
          String[] parts = SPLIT_EQUALS_PATTERN.split(p);
          if (parts.length != 2) {
            throw new IllegalArgumentException("Unexpected pair " + p + " while parsion map");
          }
          return parts;
        })
        .collect(Collectors.toMap(p -> p[0], p -> p[1]));
  }

  public static boolean isDslAccessorsGeneration(Project project) {
    return project.getName().equals("gradle-kotlin-dsl-accessors");
  }

  public static class NoopAction<T> implements Action<T>, Serializable {
    @Override
    public void execute(T _ignored) {
    }
  }
}
