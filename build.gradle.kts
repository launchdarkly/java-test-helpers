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

plugins {
    java
    "java-library"
    checkstyle
    signing
    "maven-publish"
    idea
    id("de.marcphilipp.nexus-publish") version "0.4.0"
    id("io.codearte.nexus-staging") version "0.21.2"
}

repositories {
    mavenLocal()
    // Before LaunchDarkly release artifacts get synced to Maven Central they are here along with snapshots:
    maven { url = uri("https://oss.sonatype.org/content/groups/public/") }
    mavenCentral()
}

apply { from("build-shared.gradle") }

base {
    group = "com.launchdarkly"
    archivesBaseName = "launchdarkly-test-helpers"
    version = version
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
    packageGroup = "com.launchdarkly"
    numberOfRetries = 40 // we've seen extremely long delays in closing repositories
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = "com.launchdarkly"
            artifactId = "launchdarkly-test-helpers"

            pom {
                name.set("launchdarkly-test-helpers")
                description.set("LaunchDarkly Java test helpers")
                url.set("https://github.com/launchdarkly/java-test-helpers")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("LaunchDarkly SDK Team")
                        email.set("sdks@launchdarkly.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/launchdarkly/java-test-helpers.git")
                    developerConnection.set("scm:git:ssh:git@github.com:launchdarkly/java-test-helpers.git")
                    url.set("https://github.com/launchdarkly/java-test-helpers")
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
