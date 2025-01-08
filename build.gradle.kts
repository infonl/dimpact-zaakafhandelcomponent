/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

import com.diffplug.gradle.spotless.SpotlessExtension
import com.github.gradle.node.npm.task.NpmTask
import io.gitlab.arturbosch.detekt.Detekt
import io.smallrye.openapi.api.OpenApiConfig
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
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
    alias(libs.plugins.swagger.generator)
    alias(libs.plugins.detekt)
    alias(libs.plugins.spotless)
    alias(libs.plugins.allopen)
    alias(libs.plugins.noarg)
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "net.atos.common-ground"
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

val srcGenerated = layout.projectDirectory.dir("src/generated")
val srcMainResources = layout.projectDirectory.dir("src/main/resources")
val srcMainApp = layout.projectDirectory.dir("src/main/app")
val srcE2e = layout.projectDirectory.dir("src/e2e")

sourceSets {
    // create custom integration test source set
    create("itest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
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
    implementation(libs.keycloak.admin.client)

    swaggerUI(libs.swagger.ui)

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
    annotation("nl.lifely.zac.util.AllOpen")
}

noArg {
    // enable no-arg plugin for Kotlin so that WildFly's dependency injection framework (Weld)
    // can instantiate our Kotlin classes without a no-arg constructor
    annotation("nl.lifely.zac.util.NoArgConstructor")
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
        .srcDir(srcGenerated.dir("or/objecttypes/java"))
}

jsonSchema2Pojo {
    // generates Java model files for the "productaanvraag" JSON schema(s)
    setSource(srcMainResources.dir("json-schemas/productaanvraag").asFileTree)
    setSourceType("jsonschema")
    targetDirectory = srcGenerated.dir("productaanvraag/java").asFile
    targetPackage = "net.atos.zac.productaanvraag.model.generated"
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
    download.set(true)
    version.set(libs.versions.nodejs.get())
    distBaseUrl.set("https://nodejs.org/dist")
    nodeProjectDir.set(srcMainApp.asFile)
    if (System.getenv("CI") != null) {
        npmInstallCommand.set("ci")
    } else {
        npmInstallCommand.set("install")
    }
}

smallryeOpenApi {
    infoTitle.set("Zaakafhandelcomponent backend API")
    schemaFilename.set("META-INF/openapi/openapi")
    operationIdStrategy.set(OpenApiConfig.OperationIdStrategy.METHOD)
    outputFileTypeFilter.set("YAML")
}

swaggerSources {
    register("zaakafhandelcomponent") {
        setInputFile(file("$rootDir/build/generated/openapi/META-INF/openapi/openapi.yaml"))
    }
}

configure<SpotlessExtension> {
    format("misc") {
        target(".gitattributes", ".gitignore", ".containerignore", ".dockerignore")

        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
    java {
        targetExclude("src/generated/**", "build/generated/**")

        removeUnusedImports()
        importOrderFile("config/importOrder.txt")

        formatAnnotations()

        // Latest supported version:
        // https://github.com/diffplug/spotless/tree/main/lib-extra/src/main/resources/com/diffplug/spotless/extra/eclipse_wtp_formatter
        eclipse(libs.versions.spotless.eclipse.formatter.get()).configFile("config/zac.xml")
    }
    format("e2e") {
        target("src/e2e/**/*.js", "src/e2e/**/*.ts")
        targetExclude("src/e2e/node_modules/**")

        prettier(
            mapOf(
                "prettier" to libs.versions.spotless.prettier.base.get(),
                "prettier-plugin-organize-imports" to libs.versions.spotless.prettier.organize.imports.get()
            )
        ).config(mapOf("parser" to "typescript", "plugins" to arrayOf("prettier-plugin-organize-imports")))
    }
    gherkin {
        target("src/e2e/**/*.feature")
        targetExclude("src/e2e/node_modules/**")

        gherkinUtils()
    }
    format("app") {
        target("src/main/app/**/*.js", "src/main/app/**/*.ts")
        targetExclude(
            "src/main/app/node_modules/**",
            "src/main/app/dist/**",
            "src/main/app/.angular/**"
        )
        targetExclude(
            "src/main/app/node_modules/**",
            "src/main/app/src/generated/**",
            "src/main/app/coverage/**",
            "src/main/app/dist/**",
            "src/main/app/.angular/**"
        )

        prettier(
            mapOf(
                "prettier" to libs.versions.spotless.prettier.base.get(),
                "prettier-plugin-organize-imports" to libs.versions.spotless.prettier.organize.imports.get()
            )
        ).config(mapOf("parser" to "typescript", "plugins" to arrayOf("prettier-plugin-organize-imports")))
    }
    format("json") {
        target("src/**/*.json")
        targetExclude(
            "src/e2e/node_modules/**",
            "src/e2e/reports/**",
            "src/main/app/node_modules/**",
            "src/main/app/dist/**",
            "src/main/app/.angular/**",
            "src/**/package-lock.json",
            "src/main/app/coverage/**.json"
        )

        prettier(mapOf("prettier" to libs.versions.spotless.prettier.base.get())).config(mapOf("parser" to "json"))
    }
    format("html") {
        target("src/**/*.html", "src/**/*.htm")
        targetExclude(
            "src/e2e/node_modules/**",
            "src/e2e/reports/**",
            "src/main/app/node_modules/**",
            "src/main/app/dist/**",
            "src/main/app/.angular/**",
        )

        prettier(mapOf("prettier" to libs.versions.spotless.prettier.base.get())).config(mapOf("parser" to "html"))
    }
    format("less") {
        target("src/**/*.less")
        targetExclude(
            "src/e2e/node_modules/**",
            "src/e2e/reports/**",
            "src/main/app/node_modules/**",
            "src/main/app/dist/**",
            "src/main/app/.angular/**",
        )

        prettier(mapOf("prettier" to libs.versions.spotless.prettier.base.get())).config(mapOf("parser" to "less"))
    }
}

tasks {
    clean {
        dependsOn("mavenClean")

        delete(srcMainApp.dir("dist"))
        delete(srcMainApp.dir("reports"))
        delete(srcMainApp.dir("src/generated"))
        delete(srcMainApp.dir("coverage"))
        delete(srcGenerated)
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

    getByName("generateSwaggerUIZaakafhandelcomponent").setDependsOn(listOf("generateOpenApiSpec"))

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
        from("src/main/app/dist/zaakafhandelcomponent")

        // explicitly add our 'warLib' 'transitive' dependencies that are required in the generated WAR
        classpath(files(configurations["warLib"]))
    }

    // Generic configuration for the generate Java clients tasks.
    // Note that we do not specify one generic output directory but instead
    // specify a specific non-overlapping output directory per task so that
    // Gradle can cache the outputs for these tasks.
    withType<GenerateTask> {
        group = "build"
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
                "microprofileRestClientVersion" to libs.versions.microprofile.rest.client.get(),
                "sourceFolder" to "",
                "dateLibrary" to "java8",
                "disallowAdditionalPropertiesIfNotPresent" to "false",
                "openApiNullable" to "false",
                "useJakartaEe" to "true"
            )
        )
        // Specify custom Mustache template dir as temporary workaround for the issue where OpenAPI Generator
        // fails to generate import statements for @JsonbCreator annotations.
        templateDir.set("$rootDir/src/main/resources/openapi-generator-templates")
    }

    register<GenerateTask>("generateKvkZoekenClient") {
        description = "Generates Java client code for the KVK Zoeken API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/zoeken-openapi.yaml")
        outputDir.set("$rootDir/src/generated/kvk/zoeken/java")
        modelPackage.set("net.atos.client.kvk.zoeken.model.generated")
    }

    register<GenerateTask>("generateKvkBasisProfielClient") {
        description = "Generates Java client code for the KVK Basisprofiel API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml")
        outputDir.set("$rootDir/src/generated/kvk/basisprofiel/java")
        modelPackage.set("net.atos.client.kvk.basisprofiel.model.generated")
    }

    register<GenerateTask>("generateKvkVestigingsProfielClient") {
        description = "Generates Java client code for the KVK Vestigingsprofiel API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml")
        outputDir.set("$rootDir/src/generated/kvk/vestigingsprofiel/java")
        modelPackage.set("net.atos.client.kvk.vestigingsprofiel.model.generated")
    }

    register<GenerateTask>("generateBrpClient") {
        description = "Generates Java client code for the BRP API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/brp/brp-openapi.yaml")
        outputDir.set("$rootDir/src/generated/brp/java")
        modelPackage.set("net.atos.client.brp.model.generated")
    }

    register<GenerateTask>("generateBagClient") {
        description = "Generates Java client code for the BAG API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/bag/bag-openapi.yaml")
        outputDir.set("$rootDir/src/generated/bag/java")
        modelPackage.set("net.atos.client.bag.model.generated")
        // we need to use the java8-localdatetime date library for this client
        // or else certain date time fields for this client cannot be deserialized
        configOptions.set(
            mapOf(
                "library" to "microprofile",
                "microprofileRestClientVersion" to libs.versions.microprofile.rest.client.get(),
                "sourceFolder" to "",
                "dateLibrary" to "java8-localdatetime",
                "disallowAdditionalPropertiesIfNotPresent" to "false",
                "openApiNullable" to "false",
                "useJakartaEe" to "true"
            )
        )
    }

    register<GenerateTask>("generateKlantenClient") {
        description = "Generates Java client code for the Klanten API"
        // disabled because (at least with our current settings) this results
        // in uncompilable generated Java code
        // this task was not enabled in the original Maven build either;
        // these model files were added to the code base manually instead
        isEnabled = false

        inputSpec.set("$rootDir/src/main/resources/api-specs/klanten/klanten-openapi.yaml")
        outputDir.set("$rootDir/src/generated/klanten/java")
        modelPackage.set("net.atos.client.klanten.model.generated")
    }

    register<GenerateTask>("generateZgwBrcClient") {
        description = "Generates Java client code for the BRC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/brc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/brc/java")
        modelPackage.set("net.atos.client.zgw.brc.model.generated")
    }

    register<GenerateTask>("generateZgwDrcClient") {
        description = "Generates Java client code for the DRC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/drc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/drc/java")

        // this OpenAPI spec contains a schema validation error: `schema: null`
        // so we disable the schema validation for this spec until this is fixed in a future version of this spec
        validateSpec.set(false)

        modelPackage.set("net.atos.client.zgw.drc.model.generated")
    }

    register<GenerateTask>("generateZgwZrcClient") {
        description = "Generates Java client code for the ZRC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/zrc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/zrc/java")
        modelPackage.set("net.atos.client.zgw.zrc.model.generated")
    }

    register<GenerateTask>("generateZgwZtcClient") {
        description = "Generates Java client code for the ZTC API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/ztc-openapi.yaml")
        outputDir.set("$rootDir/src/generated/zgw/ztc/java")
        modelPackage.set("net.atos.client.zgw.ztc.model.generated")
    }

    register<GenerateTask>("generateOrObjectsClient") {
        description = "Generates Java client code for the Objects API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/or/objects-openapi.yaml")
        outputDir.set("$rootDir/src/generated/or/objects/java")
        modelPackage.set("net.atos.client.or.objects.model.generated")
    }

    register<GenerateTask>("generateOrObjectTypesClient") {
        description = "Generates Java client code for the Object Types API"
        inputSpec.set("$rootDir/src/main/resources/api-specs/or/objecttypes-openapi.yaml")
        outputDir.set("$rootDir/src/generated/or/objecttypes/java")
        modelPackage.set("net.atos.client.or.objecttypes.model.generated")
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
            "generateOrObjectTypesClient"
        )
    }

    getByName("npmInstall") {
        description = "Installs the frontend application dependencies"
        group = "build"
        inputs.file("src/main/app/package.json")
        outputs.dir("src/main/app/node_modules")
    }

    register<NpmTask>("npmRunBuild") {
        description = "Builds the frontend application"
        group = "build"
        dependsOn("npmInstall")
        dependsOn("generateOpenApiSpec")

        npmCommand.set(listOf("run", "build"))

        inputs.files(fileTree("src/main/app/node_modules"))
        inputs.files(fileTree("src/main/app/src"))
        outputs.files(fileTree("src/main/app/dist/zaakafhandelcomponent"))
        outputs.files(fileTree("src/main/app/src/generated/types"))
        outputs.cacheIf { true }
    }

    register<NpmTask>("npmRunTest") {
        description = "Runs the frontend test suite"
        group = "verification"
        dependsOn("npmRunBuild")

        npmCommand.set(listOf("run", "test"))

        inputs.files(fileTree("src/main/app/node_modules"))
        inputs.files(fileTree("src/main/app/src"))

        // directory used by the Jest reporter(s) that we have configured
        outputs.dir("src/main/app/reports")
    }

    register<NpmTask>("npmRunTestCoverage") {
        description = "Generates the frontend test suite code coverage report"
        group = "verification"
        dependsOn("npmRunTest")

        npmCommand.set(listOf("run", "test:report"))
        outputs.dir("src/main/app/coverage")
    }

    register<Exec>("buildDockerImage") {
        description = "Builds the Docker image for the Zaakafhandelcomponent"
        group = "build"
        dependsOn("generateWildflyBootableJar")

        inputs.file("Dockerfile")
        inputs.file("target/zaakafhandelcomponent.jar")
        inputs.files(fileTree("certificates"))

        workingDir(".")
        commandLine("scripts/docker/build-docker-image.sh", "-v", versionNumber, "-b", branchName, "-c", commitHash, "-t", zacDockerImage)
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

        testClassesDirs = sourceSets["itest"].output.classesDirs
        classpath = sourceSets["itest"].runtimeClasspath
        systemProperty("zacDockerImage", zacDockerImage)
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

        val wildflyResources = srcMainResources.dir("wildfly")
        inputs.files(wildflyResources.asFileTree)
        inputs.file(layout.buildDirectory.file("libs/zaakafhandelcomponent.war"))
        inputs.file(layout.projectDirectory.file("pom.xml"))
        inputs.file(wildflyResources.file("configure-wildfly.cli"))
        inputs.file(wildflyResources.file("deploy-zaakafhandelcomponent.cli"))
        outputs.dir(layout.projectDirectory.dir("target"))
    }

    register<Maven>("mavenClean") {
        description = "Cleans the Maven build output"
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
