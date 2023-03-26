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

package ws.gross.gradle.tasks;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.util.PropertiesUtils;

public abstract class GenerateBootstrapManifest extends DefaultTask {
  @Input
  public abstract ListProperty<String> getPluginIds();

  @Input
  public abstract MapProperty<String, String> getCatalogIds();

  @Input
  public abstract Property<String> getVersion();

  @Input
  @Optional
  public abstract Property<String> getManifestDescription();

  @OutputFile
  public abstract RegularFileProperty getOutputFile();

  @TaskAction
  public void writeManifest() throws IOException {
    Properties properties = new Properties();

    List<Map.Entry<String, String>> catalogs = getCatalogIds().get().entrySet().stream().sorted(Map.Entry.comparingByKey()).collect(Collectors.toList());
    List<String> plugins = getPluginIds().get().stream().sorted().collect(Collectors.toList());
    properties.setProperty("catalogIds", catalogs.stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining(",")));
    properties.setProperty("pluginIds", String.join(",", plugins));
    properties.setProperty("version", getVersion().get());
    if (getManifestDescription().isPresent()) {
      properties.setProperty("description", getManifestDescription().get());
    }

    getLogger().info("Writing manifest to {}", getOutputFile().get());
    getLogger().info("  catalogIgs = {}", catalogs.stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.joining("\n  ")));
    getLogger().info("  pluginIds = {}", String.join("\n  ", plugins));
    getLogger().info("  version = {}", getVersion().get());
    getLogger().info("  description = {}", getManifestDescription().getOrElse("<none>"));

    try (OutputStream os = Files.newOutputStream(getOutputFile().get().getAsFile().toPath())) {
      PropertiesUtils.store(properties, os, null, StandardCharsets.UTF_8, "\n");
    }
  }
}
