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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.initialization.Settings;
import org.gradle.api.internal.artifacts.DependencyResolutionServices;
import org.gradle.plugin.use.internal.PluginDependencyResolutionServices;
import org.jetbrains.annotations.NotNull;
import ws.gross.gradle.extensions.DefaultPrivateRepoExtension;
import ws.gross.gradle.extensions.PrivateRepoExtension;

public class PrivateRepoBasePlugin implements Plugin<Settings> {
  private static final String GET_DRS_METHOD_NAME = "getDependencyResolutionServices";

  private final PluginDependencyResolutionServices pluginDependencyResolutionServices;

  @Inject
  public PrivateRepoBasePlugin(PluginDependencyResolutionServices pluginDependencyResolutionServices) {
    this.pluginDependencyResolutionServices = pluginDependencyResolutionServices;
  }

  @Override
  public void apply(Settings settings) {
    settings.getExtensions().create(
        PrivateRepoExtension.class,
        "privateRepo",
        DefaultPrivateRepoExtension.class,
        getDrsSupplier()
    );
  }

  @NotNull
  private Supplier<DependencyResolutionServices> getDrsSupplier() {
    return () -> {
      try {
        Method getter = pluginDependencyResolutionServices.getClass().getDeclaredMethod(GET_DRS_METHOD_NAME);
        getter.setAccessible(true);
        return (DependencyResolutionServices) getter.invoke(pluginDependencyResolutionServices);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        throw new GradleException(String.format("Can't invoke %s.%s: %s %s",
            PluginDependencyResolutionServices.class.getName(), GET_DRS_METHOD_NAME,
            e.getClass().getSimpleName(), e.getMessage()));
      }
    };
  }
}
