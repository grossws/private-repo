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

import java.util.concurrent.atomic.AtomicBoolean;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.AppliedPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import ws.gross.gradle.tasks.ReleaseApproveTask;

public class ReleaseApprovePlugin implements Plugin<Project> {
  @Override
  public void apply(Project project) {
    AtomicBoolean applied = new AtomicBoolean();

    Action<AppliedPlugin> action = ap -> {
      if(applied.get()) return;

      TaskContainer tasks = project.getTasks();

      TaskProvider<ReleaseApproveTask> task = tasks.register("approveRelease", ReleaseApproveTask.class, t -> {
        t.setGroup("Nebula Release");
        t.setDescription("Approve rc/final release before pushing git tag");
      });

      tasks.named("candidateSetup", t -> t.dependsOn(task));
      tasks.named("finalSetup", t -> t.dependsOn(task));
      tasks.named("release", t -> t.mustRunAfter(task));

      applied.set(true);
    };

    project.getPluginManager().withPlugin("nebula.release", action);
    project.getPluginManager().withPlugin("com.netflix.nebula.release", action);
  }
}
