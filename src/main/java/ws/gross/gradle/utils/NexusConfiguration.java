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

import java.net.URI;
import java.util.Collections;
import java.util.List;

import org.gradle.api.credentials.Credentials;
import org.gradle.api.credentials.PasswordCredentials;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;

public class NexusConfiguration {
  public static final String NEXUS_REPO_NAME = "nexus";

  public static final String RELEASES_REPO_NAME = "nexusReleases";

  public static final String SNAPSHOTS_REPO_NAME = "nexusSnapshots";

  private final Provider<String> baseUrl;
  private final Provider<? extends Credentials> credentials;
  private final Provider<List<String>> defaultGroupRegex;

  public NexusConfiguration(
      Provider<String> baseUrl,
      Provider<? extends Credentials> credentials,
      Provider<List<String>> defaultGroupRegex
  ) {
    this.baseUrl = baseUrl;
    this.credentials = credentials;
    this.defaultGroupRegex = defaultGroupRegex;
  }

  public static NexusConfiguration from(ProviderFactory providers) {
    Provider<String> baseUrl = providers.gradleProperty("nexusUrl");

    Boolean enabled = providers.gradleProperty("nexusDefaultGroupRegex")
        .map(Boolean::getBoolean).orElse(true).get();
    Provider<String> regex = baseUrl.map(uri -> {
      String[] parts = URI.create(uri).getHost().split("\\.");
      if (parts.length < 2) {
        //noinspection DataFlowIssue
        return null;
      } else {
        return String.format("%s\\.%s(\\..*)?", parts[parts.length - 1], parts[parts.length - 2]);
      }
    });
    Provider<List<String>> defaultGroupRegex = regex.map(r -> enabled ? Collections.singletonList(r) : Collections.emptyList());

    return new NexusConfiguration(
        baseUrl,
        providers.credentials(PasswordCredentials.class, "nexus"),
        defaultGroupRegex
    );
  }

  public Provider<String> getBaseUrl() {
    return baseUrl;
  }

  public Provider<? extends Credentials> getCredentials() {
    return credentials;
  }

  public Provider<List<String>> getDefaultGroupRegex() {
    return defaultGroupRegex;
  }

  public Provider<String> repoUrl(Provider<String> repoPath) {
    return baseUrl.flatMap(url -> repoPath.map(it -> url + "/repository/" + it));
  }
}
