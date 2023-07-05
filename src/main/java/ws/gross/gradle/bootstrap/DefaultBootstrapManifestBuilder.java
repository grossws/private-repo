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

package ws.gross.gradle.bootstrap;

import java.util.Properties;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.MutableVersionConstraint;
import org.gradle.api.artifacts.VersionConstraint;
import org.gradle.api.attributes.Category;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import ws.gross.gradle.utils.GradleUtils;

import static ws.gross.gradle.utils.GradleUtils.parseList;
import static ws.gross.gradle.utils.GradleUtils.parseMap;
import static ws.gross.gradle.utils.StringUtils.toUpperCamelCase;

@SuppressWarnings("CodeBlock2Expr")
public class DefaultBootstrapManifestBuilder implements NamedBootstrapManifestBuilder {
  private final String name;

  private final Property<String> description;
  private final Property<VersionConstraint> version;
  private final ListProperty<String> pluginIds;
  private final MapProperty<String, String> catalogs;

  private final ObjectFactory objectFactory;

  private final Supplier<? extends DependencyResolutionServices> dependencyResolutionServicesSupplier;

  @Inject
  public DefaultBootstrapManifestBuilder(
      String name,
      ObjectFactory objectFactory,
      Supplier<? extends DependencyResolutionServices> dependencyResolutionServicesSupplier
  ) {
    this.name = name;
    this.objectFactory = objectFactory;
    this.dependencyResolutionServicesSupplier = dependencyResolutionServicesSupplier;

    description = objectFactory.property(String.class);
    version = objectFactory.property(VersionConstraint.class);
    pluginIds = objectFactory.listProperty(String.class);
    catalogs = objectFactory.mapProperty(String.class, String.class);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Property<String> getDescription() {
    return description;
  }

  @Override
  public Property<VersionConstraint> getVersion() {
    return version;
  }

  @Override
  public ListProperty<String> getPluginIds() {
    return pluginIds;
  }

  @Override
  public MapProperty<String, String> getCatalogs() {
    return catalogs;
  }

  @Override
  public void from(Object dependencyNotation) {
    from(dependencyNotation, GradleUtils.doNothing());
  }

  @Override
  public void from(Object dependencyNotation, Action<? super MutableVersionConstraint> versionSpec) {
    DependencyResolutionServices drs = dependencyResolutionServicesSupplier.get();

    String configurationName = "incomingBootstrapManifestFor" + toUpperCamelCase(name);
    Configuration cnf = drs.getConfigurationContainer().create(configurationName, c -> {
      c.getResolutionStrategy().activateDependencyLocking();
      c.setCanBeConsumed(false);
      c.setCanBeResolved(true);
      c.attributes(a -> {
        a.attribute(Category.CATEGORY_ATTRIBUTE, objectFactory.named(Category.class, "manifest"));
      });
    });

    Dependency dep = drs.getDependencyHandler().create(dependencyNotation);
    if (dep instanceof ExternalDependency) {
      ((ExternalDependency) dep).version(versionSpec);
    }

    cnf.getDependencies().add(dep);

    cnf.getIncoming().getArtifacts().getArtifacts().forEach(ar -> {
      if (!ar.getFile().exists()) {
        throw new GradleException(String.format("Import of external bootstrap manifest failed: %s doesn't exist", ar.getFile()));
      }

      Properties props = GradleUtils.readProperties(ar.getFile().toPath());
      description.set(props.getProperty("description", ""));
      version(props.getProperty("version"));
      parseMap(props.getProperty("catalogIds")).forEach(this::catalog);
      parseList(props.getProperty("pluginIds")).forEach(this::plugin);
    });
  }

  @Override
  public void version(String version) {
    this.version.set(new DefaultMutableVersionConstraint(version).asImmutable());
  }

  @Override
  public void version(Action<? super MutableVersionConstraint> versionSpec) {
    DefaultMutableVersionConstraint version = new DefaultMutableVersionConstraint("");
    versionSpec.execute(version);
    this.version.set(version.asImmutable());
  }

  @Override
  public void catalog(String alias, String groupAndModule) {
    catalogs.put(alias, groupAndModule);
  }

  @Override
  public void plugin(String pluginId) {
    pluginIds.add(pluginId);
  }
}
