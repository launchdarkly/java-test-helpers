
// Centralize dependencies here instead of writing them out in the top-level
// build script(s).

object Versions {
    const val gson = "2.7"
    const val guava = "30.1-jre"
    const val ldNanoHttpd = "1.0.0-SNAPSHOT"
    const val okhttpTls = "4.8.1"
}

object PluginVersions {
    const val nexusPublish = "0.4.0"
    const val nexusStaging = "0.21.2"
}

object Libs {
    val implementation = listOf<String>(
        // We would put anything here that we want to go into the Gradle "implementation"
        // configuration, if and only if we want those things to show up in pom.xml.
        "com.launchdarkly.labs:nanohttpd:${Versions.ldNanoHttpd}",
        "com.google.code.gson:gson:${Versions.gson}",
        "com.google.guava:guava:${Versions.guava}",
        "com.squareup.okhttp3:okhttp-tls:${Versions.okhttpTls}",
        "org.hamcrest:hamcrest-library:1.3"
    )

    val javaTestImplementation = listOf(
        "com.squareup.okhttp3:okhttp:4.5.0",
        "junit:junit:4.12"
        
        // "com.launchdarkly:test-helpers:${Versions.testHelpers}"
        // test-helpers is special-cased in build.gradle.kts and build-android.gradle
    )

    val androidTestImplementation = javaTestImplementation + listOf(    
        "androidx.test:core:1.4.0",
        "androidx.test:runner:1.4.0",
        "androidx.test:rules:1.4.0",
        "androidx.test.ext:junit:1.1.3"
    )

    val javaBuiltInGradlePlugins = listOf(
        "java",
        "java-library",
        "checkstyle",
        "signing",
        "maven-publish",
        "idea"
    )

    val javaExtGradlePlugins = mapOf(
        "de.marcphilipp.nexus-publish" to PluginVersions.nexusPublish,
        "io.codearte.nexus-staging" to PluginVersions.nexusStaging
    )
}
