plugins {
  kotlin("jvm")
}

apply<org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin>()
configure<org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverExtension> {
  annotation(HasImplicitReceiver::class.qualifiedName!!)
}

dependencies {
  implementation(platform(kotlin("bom")))

  implementation(gradleKotlinDsl())
  implementation(kotlin("stdlib-jdk8"))

  testImplementation(platform("org.junit:junit-bom:5.7.1"))
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

  testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.23.1")
}

tasks.withType<JavaCompile>().configureEach {
  options.release.set(11)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "11"
    languageVersion = "1.4"
    apiVersion = "1.4"
  }
}

val functionalTestSourceSet: SourceSet = sourceSets.create("functionalTest")
configurations[functionalTestSourceSet.implementationConfigurationName]
  .extendsFrom(configurations["testImplementation"])
configurations[functionalTestSourceSet.runtimeOnlyConfigurationName]
  .extendsFrom(configurations["testRuntimeOnly"])

val functionalTest by tasks.registering(Test::class) {
  testClassesDirs = functionalTestSourceSet.output.classesDirs
  classpath = functionalTestSourceSet.runtimeClasspath
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

tasks.named("check") {
  dependsOn(functionalTest)
}
