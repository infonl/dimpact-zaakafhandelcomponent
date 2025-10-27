/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.gradle.node.npm.task.NpmTask
import io.gitlab.arturbosch.detekt.Detekt
import io.smallrye.openapi.api.OpenApiConfig.DuplicateOperationIdBehavior
import io.smallrye.openapi.api.OpenApiConfig.OperationIdStrategy
import org.gradle.api.plugins.JavaBasePlugin.BUILD_TASK_NAME
import org.gradle.api.plugins.JavaBasePlugin.DOCUMENTATION_GROUP
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale


plugins {
    java
    war
    jacoco

    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jsonschema2pojo)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.gradle.node)
    alias(libs.plugins.taskinfo)
    alias(libs.plugins.openapi)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.allopen)
    alias(libs.plugins.noarg)
}

repositories {
    mavenLocal()
    mavenCentral()
    // Add the Public JBoss Maven repository.
    // This is a best practice when provisioning a WildFly server, as some WildFly components may not be available in Maven Central.
    maven("https://repository.jboss.org/nexus/content/groups/public-jboss")
}

group = "nl.info.common-ground"
description = "Zaakafhandelcomponent"

val branchName by extra {
    if (project.hasProperty("branchName")) {
        project.property("branchName").toString()
    } else {
        "localdev"
    }
}

val commitHash by extra {
    if (project.hasProperty("commitHash")) {
        project.property("commitHash").toString()
    } else {
        "localdev"
    }
}

// create custom configuration for the JaCoCo agent JAR used to generate code coverage of our integration tests
// see: https://blog.akquinet.de/2018/09/06/test-coverage-for-containerized-java-apps/
val jacocoAgentJarForItest: Configuration by configurations.creating {
    isTransitive = false
}

// sets the Java version for all Kotlin and Java compilation tasks (source and target compatibility)
// make sure the Java version is supported by WildFly
// and update our base Docker image and JDK versions in our GitHubs workflows accordingly
val javaVersion = 21

val versionNumber by extra {
    if (project.hasProperty("versionNumber")) {
        project.property("versionNumber").toString()
    } else {
        "dev"
    }
}

// create custom configuration for extra dependencies that are required in the generated WAR
val warLib: Configuration by configurations.creating {
    extendsFrom(configurations["compileOnly"])
}

val zacDockerImage by extra {
    if (project.hasProperty("zacDockerImage")) {
        project.property("zacDockerImage").toString()
    } else {
        "ghcr.io/infonl/zaakafhandelcomponent:dev"
    }
}

val featureFlagPabcIntegration by extra {
    if (project.hasProperty("featureFlagPabcIntegration")) {
        project.property("featureFlagPabcIntegration").toString()
    } else {
        "true"
    }
}

fun Directory.toProjectRelativePath() = toString().replace("${layout.projectDirectory}/", "")

// For consistency, the layout of some known paths are determined here, and below as relative paths.
// This means that we can use these and are less likely to make mistakes when using them
val srcGenerated = layout.projectDirectory.dir("src/generated")
val srcResources = layout.projectDirectory.dir("src/main/resources")
val srcApp = layout.projectDirectory.dir("src/main/app")
val appPath = srcApp.toProjectRelativePath()
val srcE2e = layout.projectDirectory.dir("src/e2e")
val e2ePath = srcE2e.toProjectRelativePath()

// create custom source set for our integration tests
val itest by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.apache.commons.lang)
    implementation(libs.apache.commons.text)
    implementation(libs.apache.commons.collections)
    implementation(libs.commons.io)
    implementation(libs.opencsv)
    implementation(libs.flowable.engine)
    implementation(libs.flowable.cdi)
    implementation(libs.flowable.cmmn.engine)
    implementation(libs.flowable.cmmn.cdi)
    implementation(libs.flowable.cmmn.engine.configurator)
    implementation(libs.slf4j.jdk14)
    implementation(libs.auth0.java.jwt)
    implementation(libs.javax.cache.api)
    implementation(libs.google.guava)
    implementation(libs.itextpdf.kernel)
    implementation(libs.itextpdf.layout)
    implementation(libs.itextpdf.io)
    implementation(libs.itextpdf.html2pdf)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.apache.solr)
    implementation(libs.webdav.servlet)
    implementation(libs.htmlcleaner)
    implementation(libs.caffeine)
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(libs.opentelemetry.extension.kotlin)
    implementation(libs.keycloak.admin.client) {
        // exclude RESTEasy transitive dependencies because RESTEasy is already provided for by WildFly
        // and these transitive dependencies may cause conflicts with the RESTEasy version provided by WildFly
        exclude(group = "org.jboss.resteasy")
    }
    implementation(libs.jacobras.human.readable)
    implementation(libs.okhttp)
    implementation(libs.okhttp.urlconnection)

    // enable detekt formatting rules. see: https://detekt.dev/docs/rules/formatting/
    detektPlugins(libs.detekt.formatting)

    runtimeOnly(libs.infinispan.jcache)
    runtimeOnly(libs.infinispan.cdi.embedded)

    // declare dependencies that are required in the generated WAR; see war section below
    // simply marking them as 'compileOnly' or 'implementation' does not work
    warLib(libs.apache.httpclient)
    warLib(libs.reactive.streams)
    // WildFly does already include the Jakarta Mail API lib so not sure why, but we need to
    // include it in the WAR or else ZAC will fail to be deployed
    warLib(libs.jakarta.mail)

    // dependencies provided by Wildfly
    // update these versions when upgrading WildFly
    // you can find most of these dependencies in the WildFly pom.xml file
    // of the WidFly version you are using on https://github.com/wildfly/wildfly
    // for others you need to check the 'modules' directory of your local WildFly installation
    providedCompile(libs.jakarta.jakartaee)
    providedCompile(libs.eclipse.microprofile.rest.client.api)
    providedCompile(libs.eclipse.microprofile.config.api)
    providedCompile(libs.eclipse.microprofile.health.api)
    providedCompile(libs.eclipse.microprofile.fault.tolerance.api)
    providedCompile(libs.jboss.resteasy.multipart.provider)
    providedCompile(libs.wildfly.security.elytron.http.oidc)
    providedCompile(libs.hibernate.validator)
    // ~dependencies provided by Wildfly

    // yasson is required for using a JSONB context in our unit tests
    // where we do not have the WildFly runtime environment available
    testImplementation(libs.eclipse.yasson)
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.mockk)
    testImplementation(libs.json)
    // Hibernate Validator requires an implementation of the Jakarta Expression Language
    // runtime this is provided for by the WildFly runtime environment
    // for our unit tests we use the reference implementation
    testImplementation(libs.glassfish.expressly)

    // integration test dependencies
    "itestImplementation"(libs.testcontainers.testcontainers)
    "itestImplementation"(libs.testcontainers.mockserver)
    "itestImplementation"(libs.testcontainers.postgresql)
    "itestImplementation"(libs.json)
    "itestImplementation"(libs.kotest.runner.junit5)
    "itestImplementation"(libs.kotest.assertions.json)
    "itestImplementation"(libs.slf4j.simple)
    "itestImplementation"(libs.okhttp)
    "itestImplementation"(libs.okhttp.urlconnection)
    "itestImplementation"(libs.auth0.java.jwt)
    "itestImplementation"(libs.github.kotlin.logging)
    "itestImplementation"(libs.kotlin.csv.jvm)

    jacocoAgentJarForItest(variantOf(libs.jacoco.agent) { classifier("runtime") })
}

allOpen {
    // enable all-open plugin for Kotlin so that WildFly's dependency injection framework (Weld)
    // can proxy our Kotlin classes when they have our custom annotation
    // because by default Kotlin classes are final
    annotation("nl.info.zac.util.AllOpen")
}

noArg {
    // enable no-arg plugin for Kotlin so that WildFly's dependency injection framework (Weld)
    // can instantiate our Kotlin classes without a no-arg constructor
    annotation("nl.info.zac.util.NoArgConstructor")
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

java {
    // add our generated client code to the main source set
    sourceSets["main"].java
        .srcDir(srcGenerated.dir("productaanvraag/java"))
        .srcDir(srcGenerated.dir("kvk/zoeken/java"))
        .srcDir(srcGenerated.dir("kvk/basisprofiel/java"))
        .srcDir(srcGenerated.dir("kvk/vestigingsprofiel/java"))
        .srcDir(srcGenerated.dir("brp/java"))
        .srcDir(srcGenerated.dir("bag/java"))
        .srcDir(srcGenerated.dir("klanten/java"))
        .srcDir(srcGenerated.dir("zgw/brc/java"))
        .srcDir(srcGenerated.dir("zgw/drc/java"))
        .srcDir(srcGenerated.dir("zgw/zrc/java"))
        .srcDir(srcGenerated.dir("zgw/ztc/java"))
        .srcDir(srcGenerated.dir("or/objects/java"))
        .srcDir(srcGenerated.dir("pabc/java"))
}

jsonSchema2Pojo {
    // generates Java model files for the "productaanvraag" JSON schema(s)
    setSource(srcResources.dir("json-schemas/productaanvraag").asFileTree)
    setSourceType("jsonschema")
    targetDirectory = srcGenerated.dir("productaanvraag/java").asFile
    targetPackage = "nl.info.zac.productaanvraag.model.generated"
    setAnnotationStyle("JSONB2")
    dateType = "java.time.LocalDate"
    dateTimeType = "java.time.ZonedDateTime"
    timeType = "java.time.LocalTime"
    includeHashcodeAndEquals = false
    includeToString = false
    initializeCollections = false
    includeAdditionalProperties = false
}

kotlin {
    // set the Java version for all Kotlin and Java compilation tasks
    // including source and target compatibility
    // see: https://www.baeldung.com/kotlin/gradle-kotlin-bytecode-version
    jvmToolchain(javaVersion)
}

node {
    fun isNodeVersionAvailable(version: String): Boolean {
        val url = URI("https://nodejs.org/dist/v$version/").toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "HEAD"
        return connection.responseCode == HttpURLConnection.HTTP_OK
    }

    fun getLatestAvailableVersion(version: String): String {
        val baseVersion = version.substringBeforeLast('.')
        var patchVersion = version.substringAfterLast('.').toInt()
        while (patchVersion >= 0) {
            val currentVersion = "$baseVersion.$patchVersion"
            if (isNodeVersionAvailable(currentVersion)) {
                return currentVersion
            }
            patchVersion--
        }
        error("No available version found for base version $baseVersion")
    }

    fun packageJsonNodeVersion(): String {
        val packageJson = file("$srcApp/package.json").readText()
        val regex = """"node":\s*"([^"]+)"""".toRegex()
        return regex.find(packageJson)?.groups?.get(1)?.value ?: error("Node version not found")
    }

    fun packageJsonNpmVersion(): String {
        val packageJson = file("$srcApp/package.json").readText()
        val regex = """"npm":\s*"([^"]+)"""".toRegex()
        return regex.find(packageJson)?.groups?.get(1)?.value ?: error("npm version not found")
    }

    download.set(true)
    version.set(packageJsonNodeVersion().let(::getLatestAvailableVersion))
    npmVersion.set(packageJsonNpmVersion())
    distBaseUrl.set("https://nodejs.org/dist")
    nodeProjectDir.set(srcApp.asFile)
    if (System.getenv("CI") != null) {
        npmInstallCommand.set("ci")
    } else {
        npmInstallCommand.set("install")
    }
}

smallryeOpenApi {
    infoTitle.set("Zaakafhandelcomponent backend API")
    schemaFilename.set("META-INF/openapi/openapi")
    operationIdStrategy.set(OperationIdStrategy.METHOD)
    duplicateOperationIdBehavior.set(DuplicateOperationIdBehavior.FAIL)
    outputFileTypeFilter.set("YAML")
}

configure<SpotlessExtension> {
    format("misc") {
        target(".gitattributes", ".gitignore", ".containerignore", ".dockerignore")

        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }
    java {
        val genPath = srcGenerated.toProjectRelativePath()
        targetExclude("$genPath/**", "build/**")

        removeUnusedImports()
        importOrderFile("config/importOrder.txt")

        formatAnnotations()

        // Latest supported version:
        // https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatter
        eclipse(libs.versions.spotless.eclipse.formatter.get()).configFile("config/zac.xml")
    }
    format("e2e") {
        target("$e2ePath/**/*.js", "$e2ePath/**/*.ts")
        targetExclude("$e2ePath/node_modules/**")

        prettier(
            mapOf(
                "prettier" to libs.versions.spotless.prettier.base.get(),
                "prettier-plugin-organize-imports" to libs.versions.spotless.prettier.organize.imports.get()
            )
        ).config(mapOf("parser" to "typescript", "plugins" to arrayOf("prettier-plugin-organize-imports")))
    }
    gherkin {
        target("$e2ePath/**/*.feature")
        targetExclude("$e2ePath/node_modules/**")

        gherkinUtils()
    }
    format("app") {
        target("$appPath/**/*.js", "$appPath/**/*.ts")
        targetExclude(
            "$appPath/node_modules/**",
            "$appPath/src/generated/**",
            "$appPath/coverage/**",
            "$appPath/dist/**",
            "$appPath/.angular/**"
        )

        prettier(
            mapOf(
                "prettier" to libs.versions.spotless.prettier.base.get(),
                "prettier-plugin-organize-imports" to libs.versions.spotless.prettier.organize.imports.get()
            )
        ).config(mapOf("parser" to "typescript", "plugins" to arrayOf("prettier-plugin-organize-imports")))
    }
    format("json") {
        target("src/**/*.json", "scripts/**/*.json")
        targetExclude(
            "$e2ePath/node_modules/**",
            "$e2ePath/reports/**",
            "$appPath/node_modules/**",
            "$appPath/dist/**",
            "$appPath/.angular/**",
            "src/**/package-lock.json",
            "$appPath/coverage/**",
            "**/.venv/**",
            "scripts/docker-compose/volume-data/**"
        )

        prettier(mapOf("prettier" to libs.versions.spotless.prettier.base.get())).config(mapOf("parser" to "json"))
    }
    format("html") {
        target("src/**/*.html", "src/**/*.htm")
        targetExclude(
            "$e2ePath/node_modules/**",
            "$e2ePath/reports/**",
            "$appPath/node_modules/**",
            "$appPath/dist/**",
            "$appPath/.angular/**",
            "$appPath/coverage/**",
        )

        prettier(
            mapOf("prettier" to libs.versions.spotless.prettier.base.get())
        ).config(mapOf("parser" to "angular"))
    }
    format("less") {
        target("src/**/*.less")
        targetExclude(
            "$e2ePath/node_modules/**",
            "$e2ePath/reports/**",
            "$appPath/node_modules/**",
            "$appPath/dist/**",
            "$appPath/.angular/**",
        )

        prettier(mapOf("prettier" to libs.versions.spotless.prettier.base.get())).config(mapOf("parser" to "less"))
    }
}

tasks {
    clean {
        dependsOn("cleanMaven")

        delete(".gradle/configuration-cache")
        delete(srcGenerated)

        finalizedBy("cleanApp", "cleanE2e")
    }

    register<Delete>("cleanApp") {
        description = "Deletes the App build output"
        group = "build"

        delete(srcApp.dir("dist"))
        delete(srcApp.dir("reports"))
        delete(srcApp.dir("src/generated"))
        delete(srcApp.dir("coverage"))
    }

    register<Delete>("cleanE2e") {
        description = "Cleans the e2e build output"
        group = "build"

        delete(srcE2e.dir("reports"))
    }

    build {
        dependsOn("generateWildflyBootableJar")
    }

    test {
        dependsOn("npmRunTest")
    }

    compileJava {
        dependsOn("generateJavaClients")
    }

    compileKotlin {
        dependsOn("generateJavaClients")

        compilerOptions {
            // see: https://youtrack.jetbrains.com/issue/KT-73255
            freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
        }
    }

    jacocoTestReport {
        dependsOn(test)

        reports {
            xml.required = true
            html.required = false
        }
    }

    processResources {
        // exclude resources that we do not need in the build artefacts
        exclude("api-specs/**")
        exclude("wildfly/**")
    }

    register<Detekt>("detektApply") {
        description = "Apply detekt fixes."
        autoCorrect = true
        ignoreFailures = true
    }
    withType<Detekt>().configureEach {
        config.setFrom("$rootDir/config/detekt.yml")
        setSource(files("src/main/kotlin", "src/test/kotlin", "src/itest/kotlin", "build.gradle.kts"))
        // our Detekt configuration build builds upon the default configuration
        buildUponDefaultConfig = true
    }

    getByName("spotlessApply").finalizedBy(listOf("detektApply"))

    // run all spotless frontend tasks after the frontend linting task because
    // the linting task has as it's output the frontend source files which are
    // input for the spotless tasks
    getByName("spotlessApp").dependsOn("npmRunBuild").mustRunAfter("npmRunLint")
    getByName("spotlessHtml").dependsOn("npmRunBuild").mustRunAfter("npmRunLint")
    getByName("spotlessJson").dependsOn("npmRunBuild").mustRunAfter("npmRunLint")
    getByName("spotlessLess").dependsOn("npmRunBuild").mustRunAfter("npmRunLint")

    getByName("compileItestKotlin") {
        dependsOn("copyJacocoAgentForItest")
        mustRunAfter("buildDockerImage")
    }

    withType<JacocoReport> {
        // exclude Java client code that was auto generated at build time
        afterEvaluate {
            classDirectories.setFrom(
                classDirectories.files.map {
                    fileTree(it).matching {
                        exclude("**/generated/**")
                    }
                }
            )
        }
    }

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    withType<War> {
        dependsOn("npmRunBuild")

        // add built frontend resources to WAR archive
        from("$appPath/dist/zaakafhandelcomponent")

        // explicitly add our 'warLib' 'transitive' dependencies that are required in the generated WAR
        classpath(files(configurations["warLib"]))
    }

    // Generic configuration for the generate Java clients tasks.
    // Note that we do not specify one generic output directory but instead
    // specify a specific non-overlapping output directory per task so that
    // Gradle can cache the outputs for these tasks.
    withType<GenerateTask> {
        group = BUILD_TASK_NAME
        generatorName.set("java")
        generateApiTests.set(false)
        generateApiDocumentation.set(false)
        generateModelTests.set(false)
        generateModelDocumentation.set(false)
        globalProperties.set(
            mapOf(
                // generate model files only (note that an empty string indicates: generate all)
                "modelDocs" to "false",
                "apis" to "false",
                "models" to ""
            )
        )
        configOptions.set(
            mapOf(
                "library" to "microprofile",
                "microprofileRestClientVersion" to libs.versions.openapi.generator.eclipse.microprofile.rest.client.api.get(),
                "sourceFolder" to "",
                "dateLibrary" to "java8",
                "useJakartaEe" to "true",
                "useBeanValidation" to "true"
            )
        )
        // Specify custom Mustache template dir as temporary workaround for issues we have with the OpenAPI Generator.
        // Both issues have to do with the support for JSON-B polymorphism type annotations introduced by
        // https://github.com/OpenAPITools/openapi-generator/pull/20164 in OpenAPI Generator version 7.11.
        // Instead of overriding these Mustache templates, the obvious workaround seems to set the additional property
        // 'jsonbPolymorphism' to false in this Gradle build file. However, that does not seem to work.
        // Probably because this property is set by the OpenAPI Generator library itself regardless of our configuration.
        templateDir.set("$rootDir/src/main/resources/openapi-generator-templates")
    }

    register<GenerateTask>("generateKvkZoekenClient") {
        description = "Generates Java client code for the KVK Zoeken API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/zoeken-openapi.yaml")
        outputDir.set("$rootDir/src/generated/kvk/zoeken/java")
        modelPackage.set("nl.info.client.kvk.zoeken.model.generated")
    }

    register<GenerateTask>("generateKvkBasisProfielClient") {
        description = "Generates Java client code for the KVK Basisprofiel API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml")
        outputDir.set("$rootDir/src/generated/kvk/basisprofiel/java")
        modelPackage.set("nl.info.client.kvk.basisprofiel.model.generated")
    }

    register<GenerateTask>("generateKvkVestigingsProfielClient") {
        description = "Generates Java client code for the KVK Vestigingsprofiel API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml")
        outputDir.set("$rootDir/src/generated/kvk/vestigingsprofiel/java")
        modelPackage.set("nl.info.client.kvk.vestigingsprofiel.model.generated")
    }

    register<GenerateTask>("generateBrpClient") {
        description = "Generates Java client code for the BRP API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/brp/brp-openapi.yaml")
        outputDir.set("$rootDir/src/generated/brp/java")
        modelPackage.set("nl.info.client.brp.model.generated")
    }

    register<GenerateTask>("generateBagClient") {
        description = "Generates Java client code for the BAG API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/bag/bag-openapi.yaml")
        outputDir.set("$rootDir/src/generated/bag/java")
        modelPackage.set("nl.info.client.bag.model.generated")
        // We need to use the `java8-localdatetime` date library for this client,
        // or else certain date time fields for this client cannot be deserialized.
        // This is because the BAG API uses the ISO 8601 standard for `date-time` fields, where a trailing time zone is optional,
        // instead of the more commonly used RFC 3339 extension, where a trailing time zone is required.
        // E.g., the BAG API uses `2024-01-01T00:00:00` instead of `2024-01-01T00:00:00Z` for `date-time` fields.
        // See: https://github.com/lvbag/BAG-API/blob/master/Getting%20started.md
        configOptions.put("dateLibrary", "java8-localdatetime")
    }

    register<GenerateTask>("generateKlantenClient") {
        description = "Generates Java client code for the Klanten API"
        // disabled because the generated Java code is not a working OpenKlanten client
        isEnabled = false

        // To generate a new version of the client:
        //
        // 1. Modify OpenAPI definition to add empty enum value where `oneOf` construct is used
        // 2. Copy the generated client from `src/generated/klanten` to `src/main/java/net/atos/client/klant/model`
        // 3. Change in `ExpandPartijAllOfExpand`
        //    a) jsob property name from `digitale_adressen` to `digitaleAdressen`
        //       (see https://github.com/maykinmedia/open-klant/issues/396)
        //    b) `Betrokkene` usage to `ExpandBetrokkene`
        //       (see https://github.com/maykinmedia/open-klant/issues/216)

        inputSpec.set("$rootDir/src/main/resources/api-specs/klanten/klanten-openapi.yaml")
        outputDir.set("$rootDir/src/generated/klanten/java")
        modelPackage.set("nl.info.client.klanten.model.generated")
    }

    register<GenerateTask>("generateZgwBrcClient") {
        description = "Generates Java client code for the ZGW BRC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/brc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/brc/java")
        modelPackage.set("nl.info.client.zgw.brc.model.generated")
    }

    register<GenerateTask>("generateZgwDrcClient") {
        description = "Generates Java client code for the ZGW DRC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/drc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/drc/java")
        modelPackage.set("nl.info.client.zgw.drc.model.generated")
    }

    register<GenerateTask>("generateZgwZrcClient") {
        description = "Generates Java client code for the ZGW ZRC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/zrc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/zrc/java")
        modelPackage.set("nl.info.client.zgw.zrc.model.generated")
    }

    register<GenerateTask>("generateZgwZtcClient") {
        description = "Generates Java client code for the ZGW ZTC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/ztc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/ztc/java")
        modelPackage.set("nl.info.client.zgw.ztc.model.generated")
    }

    register<GenerateTask>("generateOrObjectsClient") {
        description = "Generates Java client code for the Objects API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/or/objects-openapi.yaml")
        outputDir.set("$rootDir/src/generated/or/objects/java")
        modelPackage.set("nl.info.client.or.objects.model.generated")
    }

    register<GenerateTask>("generatePabcClient") {
        description = "Generates Java client code for the Platform Autorisatie Beheer Component API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/pabc/pabc-openapi.json")
        outputDir.set("$rootDir/src/generated/pabc/java")
        modelPackage.set("nl.info.client.pabc.model.generated")
    }

    register("generateJavaClients") {
        description = "Generates Java client code for the various REST APIs"
        dependsOn(
            generateJsonSchema2Pojo,
            "generateKvkZoekenClient",
            "generateKvkBasisProfielClient",
            "generateKvkVestigingsProfielClient",
            "generateBrpClient",
            "generateBagClient",
            "generateKlantenClient",
            "generateZgwBrcClient",
            "generateZgwDrcClient",
            "generateZgwZrcClient",
            "generateZgwZtcClient",
            "generateOrObjectsClient",
            "generatePabcClient"
        )
    }

    getByName("npmInstall") {
        description = "Installs the frontend application dependencies"
        group = "build"
        inputs.file("$appPath/package.json")
        outputs.dir("$appPath/node_modules")
    }

    register<NpmTask>("npmRunLint") {
        description = "Runs the linter"
        group = "verification"
        dependsOn("npmInstall")
        dependsOn("generateOpenApiSpec")

        npmCommand.set(listOf("run", "lint"))

        inputs.files(fileTree("$appPath/node_modules"))
        inputs.files(fileTree("$appPath/src"))
        outputs.files(fileTree("$appPath/src"))
        outputs.cacheIf { true }
    }

    register<NpmTask>("npmRunBuild") {
        description = "Builds the frontend application"
        group = "build"
        dependsOn("npmInstall")
        dependsOn("generateOpenApiSpec")
        dependsOn("npmRunLint")

        npmCommand.set(listOf("run", "build"))

        inputs.files(fileTree("$appPath/node_modules"))
        inputs.files(fileTree("$appPath/src"))
        outputs.files(fileTree("$appPath/dist/zaakafhandelcomponent"))
        outputs.files(fileTree("$appPath/src/generated/types"))
        outputs.cacheIf { true }
    }

    register<NpmTask>("npmRunTest") {
        description = "Runs the frontend test suite"
        group = "verification"
        dependsOn("npmRunBuild")

        npmCommand.set(listOf("run", "test"))

        inputs.files(fileTree("$appPath/node_modules"))
        inputs.files(fileTree("$appPath/src"))

        // directory used by the Jest reporter(s) that we have configured
        outputs.dir("$appPath/reports")
    }

    register<NpmTask>("npmRunTestCoverage") {
        description = "Generates the frontend test suite code coverage report"
        group = "verification"
        dependsOn("npmRunTest")

        npmCommand.set(listOf("run", "test:report"))
        outputs.dir("$appPath/coverage")
    }

    register<Exec>("buildDockerImage") {
        description = "Builds the Docker image for the Zaakafhandelcomponent"
        group = "build"
        dependsOn("generateWildflyBootableJar")

        inputs.file("Dockerfile")
        inputs.file("target/zaakafhandelcomponent.jar")
        inputs.files(fileTree("certificates"))

        workingDir(".")
        commandLine(
            "scripts/docker/build-docker-image.sh",
            "-v", versionNumber,
            "-b", branchName,
            "-c", commitHash,
            "-t", zacDockerImage
        )
    }

    register<Copy>("copyJacocoAgentForItest") {
        description = "Copies and renames the JaCoCo agent runtime JAR file for instrumentation during the integration tests"
        from(configurations.getByName("jacocoAgentJarForItest"))
        // simply rename the JaCoCo agent runtime JAR file name to strip away the version number
        rename {
            "org.jacoco.agent-runtime.jar"
        }
        into(layout.buildDirectory.dir("jacoco/itest/jacoco-agent"))
    }

    register<Test>("itest") {
        description = "Runs the integration test suite"
        group = "verification"
        dependsOn("buildDockerImage")

        testClassesDirs = itest.output.classesDirs
        classpath = itest.runtimeClasspath
        systemProperty("zacDockerImage", zacDockerImage)
        systemProperty("featureFlagPabcIntegration", featureFlagPabcIntegration)
        // do not use the Gradle build cache for this task
        outputs.cacheIf { false }
    }

    register<JacocoReport>("jacocoIntegrationTestReport") {
        dependsOn("itest")

        description = "Generates code coverage report for the integration tests"
        group = "verification"
        val resultFile = layout.buildDirectory.file("jacoco/itest/jacoco-report/jacoco-it.exec").orNull
        inputs.files(resultFile)
        executionData.setFrom(resultFile)
        // tell JaCoCo to report on our code base
        sourceSets(sourceSets["main"])
        reports {
            xml.required = true
            html.required = false
        }
        // do not use the Gradle build cache for this task
        outputs.cacheIf { false }
        outputs.dir(layout.buildDirectory.dir("reports/jacoco/jacocoIntegrationTestReport"))
    }

    register<Maven>("generateWildflyBootableJar") {
        description = "Generates a WildFly bootable JAR"
        group = "build"
        dependsOn("war")
        execGoal("wildfly:package")

        val wildflyResources = srcResources.dir("wildfly")
        inputs.files(wildflyResources.asFileTree)
        inputs.file(layout.buildDirectory.file("libs/zaakafhandelcomponent.war"))
        inputs.file(layout.projectDirectory.file("pom.xml"))
        inputs.file(wildflyResources.file("configure-wildfly.cli"))
        inputs.file(wildflyResources.file("deploy-zaakafhandelcomponent.cli"))
        outputs.dir(layout.projectDirectory.dir("target"))
    }

    register<Exec>("generateZacApiDocs") {
        description = "Generate ZAC HTML API documentation from OpenAPI spec."
        dependsOn("generateOpenApiSpec")
        group = DOCUMENTATION_GROUP
        commandLine(
            "npx",
            "@redocly/cli",
            "build-docs",
            file("$rootDir/build/generated/openapi/META-INF/openapi/openapi.yaml"),
            "-o",
            file("build/generated/zac-api-docs/index.html")
        )
    }

    register<Maven>("cleanMaven") {
        description = "Deletes the Maven build output"
        group = "build"
        execGoal("clean")
    }
}

abstract class Maven : Exec() {
    // Simple function to invoke a maven goal, dependent on the os, with optional arguments
    fun execGoal(goal: String, vararg args: String) = commandLine(
        if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) {
            "./mvnw.cmd"
        } else {
            "./mvnw"
        },
        goal,
        *args
    )
}
