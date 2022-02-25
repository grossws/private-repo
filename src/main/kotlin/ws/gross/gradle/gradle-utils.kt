/*
 * Copyright 2022 Konstantin Gribov
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

package ws.gross.gradle

import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.MutableVersionConstraint
import org.gradle.api.artifacts.VersionConstraint
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.initialization.Settings
import org.gradle.api.initialization.dsl.VersionCatalogBuilder
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.support.serviceOf
import org.gradle.plugin.use.PluginDependency
import org.gradle.util.GradleVersion
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

inline fun Project.ifNotDslAccessors(block: () -> Unit) {
  if (project.name != "gradle-kotlin-dsl-accessors") block.invoke()
}

// See https://github.com/gradle/gradle/issues/18620
fun DependencyHandler.plugin(pluginId: String, version: String) =
  create("$pluginId:$pluginId.gradle.plugin:$version")
@Suppress("UnstableApiUsage")
fun DependencyHandler.plugin(plugin: Provider<PluginDependency>) =
  plugin.get().run { plugin(pluginId, version.displayName) }

fun String.parsePublishTaskInfo(): PublishTaskInfo? =
  publishTaskNameRegex.matchEntire(this)?.destructured?.let { (p, r) ->
    PublishTaskInfo(publication = p.decapitalize(), repository = r.decapitalize())
  }

data class PublishTaskInfo(val publication: String, val repository: String)

fun isVersionCatalogsSupported() = GRADLE_7_0_PLUS
fun isVersionCatalogsExperimental(): Boolean {
  if (!isVersionCatalogsSupported()) {
    throw GradleException("version catalogs not supported")
  }
  return VersionCatalogsUtil.featureActive
}

fun Settings.isVersionCatalogsEnabled() = GRADLE_7_4_PLUS
    || (isVersionCatalogsSupported() && VersionCatalogsUtil.isFeatureEnabled(this))

fun MutableVersionConstraint.fromConstraint(v: VersionConstraint) {
  if (v.preferredVersion.isNotEmpty()) prefer(v.preferredVersion)
  if (v.requiredVersion.isNotEmpty()) require(v.requiredVersion)
  if (v.strictVersion.isNotEmpty()) strictly(v.strictVersion)
  if (v.rejectedVersions.isNotEmpty()) reject(*v.rejectedVersions.toTypedArray())
}

internal object ProviderUtil {
  @JvmStatic
  private val FOR_USE_AT_CONFIGURATION_TIME = try {
    MethodHandles.publicLookup().findVirtual(
      Provider::class.java,
      "forUseAtConfigurationTime",
      MethodType.methodType(Provider::class.java)
    )
  } catch (e: NoSuchMethodException) {
    null
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> Provider<T>.wrappedForUseAtConfigurationTime(): Provider<T> = if (GRADLE_7_4_PLUS) {
    this
  } else {
    FOR_USE_AT_CONFIGURATION_TIME!!.invokeExact(this) as Provider<T>
  }
}

internal object VersionCatalogsUtil {
  val featureActive = FeaturePreviews.Feature.VERSION_CATALOGS.isActive

  fun isFeatureEnabled(settings: Settings) =
    settings.serviceOf<FeaturePreviews>().isFeatureEnabled(FeaturePreviews.Feature.VERSION_CATALOGS)
}

private val publishTaskNameRegex = """publish([A-Z][\w.]*)PublicationTo([A-Z]\w*)Repository""".toRegex()

private val GRADLE_7_4_PLUS = GradleVersion.current() >= GradleVersion.version("7.4")
private val GRADLE_7_0_PLUS = GradleVersion.current() >= GradleVersion.version("7.0")
