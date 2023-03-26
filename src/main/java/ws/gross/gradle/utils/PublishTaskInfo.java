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

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static ws.gross.gradle.utils.StringUtils.decapitalize;

public class PublishTaskInfo {
  private static final Pattern PUBLICATION_TASK_NAME_PATTERN =
      Pattern.compile("publish([A-Z][\\w.]*)PublicationTo([A-Z][\\w.]*)Repository");

  private final String publication;

  private final String repository;

  private PublishTaskInfo(String publication, String repository) {
    this.publication = publication;
    this.repository = repository;
  }

  public static Optional<PublishTaskInfo> of(String taskName) {
    Matcher matcher = PUBLICATION_TASK_NAME_PATTERN.matcher(taskName);
    if (matcher.matches()) {
      return Optional.of(new PublishTaskInfo(decapitalize(matcher.group(1)), decapitalize(matcher.group(2))));
    } else {
      return Optional.empty();
    }
  }

  public String getPublication() {
    return publication;
  }

  public String getRepository() {
    return repository;
  }
}
