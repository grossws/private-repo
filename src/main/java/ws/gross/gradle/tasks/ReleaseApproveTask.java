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

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;

public abstract class ReleaseApproveTask extends DefaultTask {
  private final Provider<String> approve;

  public ReleaseApproveTask() {
    notCompatibleWithConfigurationCache("nebula-release doesn't support configuration cache");

    getProjectVersion().set(getProject().getVersion().toString());
    getApproveProperty().convention("release.approve");

    approve = getProject().getProviders().gradleProperty(getApproveProperty()).orElse("");
  }

  @Input
  public abstract Property<String> getProjectVersion();

  @Input
  public abstract Property<String> getApproveProperty();

  @Input
  public Provider<String> getApprove() {
    return approve;
  }

  @TaskAction
  void run() {
    if (approve.get().equals("true")) {
      getLogger().info("Explicitly approved via -P{}=true", getApproveProperty().get());
      return;
    } else if (approve.get().equals("false")) {
      getLogger().info("Explicitly not approved via -P{}=false", getApproveProperty().get());
    } else {
      UserInputHandler userInputHandler = getServices().get(UserInputHandler.class);
      Boolean answer = userInputHandler.askYesNoQuestion(String.format("Release %s version?", getProjectVersion().get()));
      if (answer == null) {
        getLogger().info("Interactive approve failed to get answer, maybe running in non-interactive environment");
      } else if (!answer) {
        getLogger().info("Interactively not approved");
      } else {
        getLogger().info("Interactively approved");
        return;
      }
    }
    throw new GradleException("Not approved, use -P" + getApproveProperty().get()
                              + "=true for non-interactive mode to approve release");
  }
}
