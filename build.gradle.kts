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

base {
    group = "com.launchdarkly"
    archivesBaseName = "test-helpers"
    version = version
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

object Versions {
    const val gson = "2.7"
    const val guava = "32.0.1-jre"
    const val ldNanoHttpd = "1.0.0-SNAPSHOT"
    const val okhttpTls = "4.8.1"
}

dependencies {
    implementation("com.launchdarkly.labs:nanohttpd:${Versions.ldNanoHttpd}")
    implementation("com.google.code.gson:gson:${Versions.gson}")
    implementation("com.google.guava:guava:${Versions.guava}")
    implementation("com.squareup.okhttp3:okhttp-tls:${Versions.okhttpTls}")
    implementation("org.hamcrest:hamcrest-library:1.3")

    testImplementation("com.squareup.okhttp3:okhttp:4.5.0")
    testImplementation("junit:junit:4.12")
}

checkstyle {
    toolVersion = "9.3"
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
            artifactId = "test-helpers"

            pom {
                name.set("test-helpers")
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
