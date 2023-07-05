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

package ws.gross.gradle;

import java.util.function.Supplier;

import javax.inject.Inject;

import org.gradle.api.Describable;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Category;
import org.gradle.api.component.AdhocComponentWithVariants;
import org.gradle.api.component.SoftwareComponentFactory;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.api.internal.artifacts.dependencies.DefaultMutableVersionConstraint;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;
import ws.gross.gradle.bootstrap.NamedBootstrapManifestBuilder;
import ws.gross.gradle.extensions.BootstrapManifestPluginExtension;
import ws.gross.gradle.extensions.DefaultBootstrapManifestPluginExtension;
import ws.gross.gradle.tasks.GenerateBootstrapManifest;
import ws.gross.gradle.utils.GradleUtils;

@SuppressWarnings("CodeBlock2Expr")
public class BootstrapManifestBasePlugin implements Plugin<Project> {
  public static final String GENERATE_BOOTSTRAP_MANIFEST_TASK_NAME = "generateBootstrapManifest";

  public static final String MANIFEST_CATEGORY_ATTRIBUTE = "manifest";

  private final SoftwareComponentFactory softwareComponentFactory;

  @Inject
  public BootstrapManifestBasePlugin(
      SoftwareComponentFactory softwareComponentFactory
  ) {
    this.softwareComponentFactory = softwareComponentFactory;
  }

  @Override
  public void apply(Project project) {
    project.getPluginManager().apply("base");

    if (!GradleUtils.isDslAccessorsGeneration(project) && project.getGroup().toString().isEmpty()) {
      throw new IllegalArgumentException("Project group required for ws.gross.bootstrap-manifest plugin");
    }

    DefaultBootstrapManifestPluginExtension ext = (DefaultBootstrapManifestPluginExtension) project.getExtensions().create(
        BootstrapManifestPluginExtension.class,
        "manifest",
        DefaultBootstrapManifestPluginExtension.class,
        getDrsSupplier(project)
    );

    Provider<String> projectVersion = project.provider(() -> project.getVersion().toString());
    NamedBootstrapManifestBuilder manifest = ext.getBootstrapManifest();
    manifest.getVersion().convention(projectVersion.map(v -> new DefaultMutableVersionConstraint(v).asImmutable()));

    TaskProvider<GenerateBootstrapManifest> task = project.getTasks().register(GENERATE_BOOTSTRAP_MANIFEST_TASK_NAME, GenerateBootstrapManifest.class, t -> {
      t.setGroup(LifecycleBasePlugin.BUILD_GROUP);
      t.setDescription("Generate bootstrap manifest");

      t.getPluginIds().convention(manifest.getPluginIds());
      t.getCatalogIds().convention(manifest.getCatalogs());
      t.getVersion().convention(manifest.getVersion().map(Describable::getDisplayName));
      t.getManifestDescription().convention(manifest.getDescription());

      t.getOutputFile().convention(project.getLayout().getBuildDirectory().file(manifest.getName() + ".properties"));
    });

    Configuration configuration = project.getConfigurations().create("manifest", cnf -> {
      cnf.setCanBeConsumed(true);
      cnf.setCanBeResolved(false);

      cnf.attributes(a -> {
        a.attribute(Category.CATEGORY_ATTRIBUTE,
            project.getObjects().named(Category.class, MANIFEST_CATEGORY_ATTRIBUTE));
      });

      cnf.getOutgoing().artifact(task.map(t -> t.getOutputFile().get()), ar -> {
        ar.setType("manifest");
        ar.setExtension("properties");
      });
    });

    AdhocComponentWithVariants manifestComponent = softwareComponentFactory.adhoc("manifest");
    manifestComponent.addVariantsFromConfiguration(configuration, GradleUtils.doNothing());
    project.getComponents().add(manifestComponent);

    project.getPluginManager().withPlugin("maven-publish", ap -> {
      PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
      publishing.getPublications().create("manifestMaven", MavenPublication.class, p -> {
        p.from(manifestComponent);
        p.suppressAllPomMetadataWarnings();
      });
    });

    project.getTasks().named(LifecycleBasePlugin.ASSEMBLE_TASK_NAME, t -> {
      t.dependsOn(task);
    });
  }

  private Supplier<DependencyResolutionServices> getDrsSupplier(Project project) {
    return () -> ((ProjectInternal) project).getServices().get(DependencyResolutionServices.class);
  }
}
