plugins {
    `java-library`
    `maven-publish`
}

group = "dev.normlanguage"
version = Regex("""artifact: "windows", version: "([^"]+)"""")
    .find(file("windows/module.norm").readText())!!.groupValues[1]

repositories { mavenCentral() }
java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
dependencies {
    implementation("net.java.dev.jna:jna-platform:5.18.1")
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}
tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
tasks.test {
    dependsOn(tasks.jar)
    useJUnitPlatform()
    classpath = files(sourceSets.test.get().output, tasks.jar.flatMap { it.archiveFile }) +
        configurations.testRuntimeClasspath.get()
    systemProperty("windows.test.classpath", classpath.asPath)
    testLogging { events("passed", "failed", "skipped") }
}
publishing {
    publications { create<MavenPublication>("library") { from(components["java"]) } }
    repositories { maven { url = uri(layout.buildDirectory.dir("maven")) } }
}
