import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.external.javadoc.CoreJavadocOptions

// These values come from gradle.properties
val ossrhUsername: String by project
val ossrhPassword: String by project

buildscript {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

plugins {  // see Dependencies.kt in buildSrc
    Libs.javaBuiltInGradlePlugins.forEach { id(it) }
    Libs.javaExtGradlePlugins.forEach { (n, v) -> id(n) version v }
}

repositories {
    mavenLocal()
    // Before LaunchDarkly release artifacts get synced to Maven Central they are here along with snapshots:
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    mavenCentral()
}

base {
    // see buildSrc/src/main/kotlin/ProjectValues.kt
    group = ProjectValues.groupId
    version = version
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

// see buildSrc/src/main/kotlin/Dependencies.kt for dependency versions
dependencies {
    Libs.implementation.forEach { api(it) }
    Libs.javaTestImplementation.forEach { testImplementation(it) }
}

checkstyle {
    configFile = file("${project.rootDir}/checkstyle.xml")
}

tasks.jar.configure {
    manifest {
        attributes(mapOf("Implementation-Version" to project.version))
    }
}

tasks.javadoc.configure {
    // Force the Javadoc build to fail if there are any Javadoc warnings. See: https://discuss.gradle.org/t/javadoc-fail-on-warning/18141/3
    // See JDK-8200363 (https://bugs.openjdk.java.net/browse/JDK-8200363)
    // for information about the -Xwerror option.
    (options as CoreJavadocOptions).addStringOption("Xwerror")
}

tasks.test.configure {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

nexusStaging {
    packageGroup = ProjectValues.groupId
    numberOfRetries = 40 // we've seen extremely long delays in closing repositories
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = ProjectValues.groupId
            artifactId = ProjectValues.artifactId

            pom {
                name.set("test-helpers")
                description.set(ProjectValues.description)
                url.set("https://github.com/${ProjectValues.githubRepo}")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set(ProjectValues.pomDeveloperName)
                        email.set(ProjectValues.pomDeveloperEmail)
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/${ProjectValues.githubRepo}.git")
                    developerConnection.set("scm:git:ssh:git@github.com:${ProjectValues.githubRepo}.git")
                    url.set("https://github.com/${ProjectValues.githubRepo}")
                }
            }
        }
    }
    repositories {
        mavenLocal()
    }
}

nexusPublishing {
    clientTimeout.set(Duration.ofMinutes(2)) // we've seen extremely long delays in creating repositories
    repositories {
        sonatype {
            username.set(ossrhUsername)
            password.set(ossrhPassword)
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}
