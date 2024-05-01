import com.bisnode.opa.configuration.ExecutableMode
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.gradle.node.npm.task.NpmTask
import io.smallrye.openapi.api.OpenApiConfig
import org.apache.tools.ant.taskdefs.condition.Os
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.util.Locale

/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

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
    alias(libs.plugins.docker.remote.api)
    alias(libs.plugins.spotless)
    alias(libs.plugins.allopen)
    alias(libs.plugins.noarg)
    alias(libs.plugins.opa)
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "net.atos.common-ground"
description = "Zaakafhandelcomponent"

// make sure the Java version is supported by WildFly
// and update our base Docker image and JDK versions in our GitHubs workflows accordingly
val javaVersion = JavaVersion.VERSION_21

val zacDockerImage by extra {
    if (project.hasProperty("zacDockerImage")) {
        project.property("zacDockerImage").toString()
    } else {
        "ghcr.io/infonl/zaakafhandelcomponent:dev"
    }
}

val versionNumber by extra {
    if (project.hasProperty("versionNumber")) {
        project.property("versionNumber").toString()
    } else {
        "dev"
    }
}

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

// create custom configuration for extra dependencies that are required in the generated WAR
val warLib: Configuration by configurations.creating {
    extendsFrom(configurations["compileOnly"])
}

// create custom configuration for the JaCoCo agent JAR used to generate code coverage of our integration tests
// see: https://blog.akquinet.de/2018/09/06/test-coverage-for-containerized-java-apps/
val jacocoAgentJarForItest: Configuration by configurations.creating {
    isTransitive = false
}

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
    implementation(libs.mailjet.client)
    implementation(libs.itextpdf.kernel)
    implementation(libs.itextpdf.layout)
    implementation(libs.itextpdf.io)
    implementation(libs.itextpdf.html2pdf)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.apache.solr)
    implementation(libs.webdav.servlet)
    implementation(libs.htmlcleaner)
    implementation(libs.unboundid.ldapsdk)
    implementation(libs.caffeine)
    implementation(libs.jackson.core)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.jsr310)
    implementation(libs.opentelemetry.api)
    implementation(libs.opentelemetry.instrumentation.annotations)
    implementation(libs.opentelemetry.extension.kotlin)

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

    // integration test dependencies
    "itestImplementation"(libs.testcontainers.testcontainers)
    "itestImplementation"(libs.testcontainers.mockserver)
    "itestImplementation"(libs.testcontainers.postgresql)
    "itestImplementation"(libs.json)
    "itestImplementation"(libs.kotest.runner.junit5)
    "itestImplementation"(libs.kotest.assertions.json)
    "itestImplementation"(libs.slf4j.simple)
    "itestImplementation"(libs.squareup.okhttp)
    "itestImplementation"(libs.squareup.okhttp.urlconnection)
    "itestImplementation"(libs.awaitility)
    "itestImplementation"(libs.mockserver.client)
    "itestImplementation"(libs.auth0.java.jwt)
    "itestImplementation"(libs.github.kotlin.logging)
    "itestImplementation"(libs.kotlin.csv.jvm)

    jacocoAgentJarForItest(variantOf(libs.jacoco.agent) { classifier("runtime") })
}

tasks.register<io.gitlab.arturbosch.detekt.Detekt>("detektApply") {
    description = "Apply detekt fixes."
    autoCorrect = true
    ignoreFailures = true
}
tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    config.setFrom("$rootDir/config/detekt.yml")
    setSource(files("src/main/kotlin", "src/test/kotlin", "src/itest/kotlin", "build.gradle.kts"))
    // our Detekt configuration build builds upon the default configuration
    buildUponDefaultConfig = true
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

if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
    opa {
        srcDir = "$rootDir/src/main/resources/policies"
        testDir = "$rootDir/src/test/resources/policies"
        version = libs.versions.opa.binary.get()
        mode = ExecutableMode.DOWNLOAD
        location = "$rootDir/build/opa/$version/opa"
    }
}

java {
    java.sourceCompatibility = javaVersion
    java.targetCompatibility = javaVersion

    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion.majorVersion)
    }

    // add our generated client code to the main source set
    sourceSets["main"].java.srcDir("$rootDir/src/generated/java")
}

jsonSchema2Pojo {
    // generates Java model files for the "productaanvraag" JSON schema(s)
    setSource(files("$rootDir/src/main/resources/json-schema"))
    targetDirectory = file("$rootDir/src/generated/java")
    setFileExtensions(".schema.json")
    targetPackage = "net.atos.zac.aanvraag.model.generated"
    setAnnotationStyle("JSONB2")
    dateType = "java.time.LocalDate"
    dateTimeType = "java.time.ZonedDateTime"
    timeType = "java.time.LocalTime"
    includeHashcodeAndEquals = false
    includeToString = false
    initializeCollections = false
    includeAdditionalProperties = false
}

node {
    download.set(true)
    version.set(libs.versions.nodejs.get())
    distBaseUrl.set("https://nodejs.org/dist")
    nodeProjectDir.set(file("$rootDir/src/main/app"))
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

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
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
}
tasks.getByName("spotlessApply").finalizedBy(listOf("detektApply"))

// run npm install task after generating the Java clients because they
// share the same output folder (= $rootDir)
tasks.getByName("npmInstall").setMustRunAfter(listOf("generateJavaClients"))
tasks.getByName("generateSwaggerUIZaakafhandelcomponent").setDependsOn(listOf("generateOpenApiSpec"))

tasks.getByName("compileItestKotlin") {
    dependsOn("copyJacocoAgentForItest")
    mustRunAfter("buildDockerImage")
}

tasks.war {
    dependsOn("npmRunBuild")

    // add built frontend resources to WAR archive
    from("src/main/app/dist/zaakafhandelcomponent")

    // explicitly add our 'warLib' 'transitive' dependencies that are required in the generated WAR
    classpath(files(configurations["warLib"]))
}

tasks {
    clean {
        dependsOn("mavenClean")

        delete("$rootDir/src/main/app/dist")
        delete("$rootDir/src/main/app/reports")
        delete("$rootDir/src/main/app/coverage")
        delete("$rootDir/src/generated")
        delete("$rootDir/src/e2e/reports")
    }

    build {

        dependsOn("generateWildflyBootableJar")
    }

    test {
        dependsOn("npmRunTest")
        if (!Os.isFamily(Os.FAMILY_WINDOWS)) {
            dependsOn("testRegoCoverage")
        }
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

    processResources {
        // exclude resources that we do not need in the build artefacts
        exclude("api-specs/**")
        exclude("wildfly/**")
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

    withType<GenerateTask> {
        generatorName.set("java")
        outputDir.set("$rootDir/src/generated/java")
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
        // Specify custom Mustache template dir as temporary workaround for the issue where OpenAPI Generator 7.2.0
        // fails to generate import statements for @JsonbCreator annotations.
        // Maybe this workaround can be removed when we migrate to OpenAPI Generator 7.3.0.
        templateDir.set("$rootDir/src/main/resources/openapi-generator-templates")
    }

    register<GenerateTask>("generateKvkZoekenClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/zoeken-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.zoeken.model.generated")
    }

    register<GenerateTask>("generateKvkBasisProfielClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.basisprofiel.model.generated")
    }

    register<GenerateTask>("generateKvkVestigingsProfielClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.vestigingsprofiel.model.generated")
    }

    register<GenerateTask>("generateBrpClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/brp/openapi.yaml")
        modelPackage.set("net.atos.client.brp.model.generated")
    }

    register<GenerateTask>("generateVrlClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/vrl/openapi.yaml")
        modelPackage.set("net.atos.client.vrl.model.generated")
    }

    register<GenerateTask>("generateBagClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/bag/openapi.yaml")
        modelPackage.set("net.atos.client.bag.model.generated")
        // we use a different date library for this client
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
        // this task was not enabled in the original Maven build either;
        // these model files were added to the code base manually instead
        isEnabled = false

        inputSpec.set("$rootDir/src/main/resources/api-specs/klanten/openapi.yaml")
        modelPackage.set("net.atos.client.klanten.model.generated")
    }

    register<GenerateTask>("generateContactMomentenClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/contactmomenten/openapi.yaml")
        modelPackage.set("net.atos.client.contactmomenten.model.generated")
    }

    register<GenerateTask>("generateZgwBrcClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/brc-openapi.yaml")
        modelPackage.set("net.atos.client.zgw.brc.model.generated")
    }

    register<GenerateTask>("generateZgwDrcClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/drc-openapi.yaml")
        modelPackage.set("net.atos.client.zgw.drc.model.generated")
    }

    register<GenerateTask>("generateZrcDrcClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/zrc-openapi.yaml")
        modelPackage.set("net.atos.client.zgw.zrc.model.generated")
    }

    register<GenerateTask>("generateZtcDrcClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/zgw/ztc-openapi.yaml")
        modelPackage.set("net.atos.client.zgw.ztc.model.generated")
    }

    register("generateJavaClients") {
        dependsOn(
            generateJsonSchema2Pojo,
            "generateKvkZoekenClient",
            "generateKvkBasisProfielClient",
            "generateKvkVestigingsProfielClient",
            "generateBrpClient",
            "generateVrlClient",
            "generateBagClient",
            "generateKlantenClient",
            "generateContactMomentenClient",
            "generateZgwBrcClient",
            "generateZgwDrcClient",
            "generateZrcDrcClient",
            "generateZtcDrcClient"
        )
    }

    register<NpmTask>("npmRunBuild") {
        dependsOn("npmInstall")
        dependsOn("generateOpenApiSpec")
        npmCommand.set(listOf("run", "build"))

        // avoid running this task when there are no changes in the input or output files
        // see: https://github.com/node-gradle/gradle-node-plugin/blob/master/docs/faq.md
        inputs.files(fileTree("src/main/app/node_modules"))
        inputs.files(fileTree("src/main/app/src"))
        inputs.file("src/main/app/package.json")
        inputs.file("src/main/app/package-lock.json")
        outputs.dir("src/main/app/dist/zaakafhandelcomponent")
    }

    register<NpmTask>("npmRunTest") {
        dependsOn("npmRunBuild")

        npmCommand.set(listOf("run", "test"))
        // avoid running this task when there are no changes in the input or output files
        // see: https://github.com/node-gradle/gradle-node-plugin/blob/master/docs/faq.md
        inputs.files(fileTree("src/main/app/node_modules"))
        inputs.files(fileTree("src/main/app/src"))
        inputs.file("src/main/app/package.json")
        inputs.file("src/main/app/package-lock.json")

        // directory used by the Jest reporter(s) that we have configured
        outputs.dir("src/main/app/reports")
    }

    register<NpmTask>("npmRunTestCoverage") {
        dependsOn("npmRunTest")

        npmCommand.set(listOf("run", "test:report"))
        outputs.dir("src/main/app/coverage")
    }

    register<DockerBuildImage>("buildDockerImage") {
        dependsOn("generateWildflyBootableJar")

        inputDir.set(file("."))
        buildArgs.set(
            mapOf(
                "versionNumber" to versionNumber,
                "branchName" to branchName,
                "commitHash" to commitHash
            )
        )
        dockerFile.set(file("Dockerfile"))
        images.add(zacDockerImage)
    }

    register<Copy>("copyJacocoAgentForItest") {
        description = "Copies and renames the JaCoCo agent runtime JAR file for instrumentation during the integration tests"
        from(configurations.getByName("jacocoAgentJarForItest"))
        // simply rename the JaCoCo agent runtime JAR file name to strip away the version number
        rename {
            "org.jacoco.agent-runtime.jar"
        }
        into("$rootDir/build/jacoco/itest/jacoco-agent")
    }

    register<Test>("itest") {
        inputs.files(project.tasks.findByPath("compileItestKotlin")!!.outputs.files)

        testClassesDirs = sourceSets["itest"].output.classesDirs
        classpath = sourceSets["itest"].runtimeClasspath

        systemProperty("zacDockerImage", zacDockerImage)
    }

    register<JacocoReport>("jacocoIntegrationTestReport") {
        dependsOn("itest")

        description = "Generates code coverage report for the integration tests"
        executionData.setFrom("$rootDir/build/jacoco/itest/jacoco-report/jacoco-it.exec")
        // tell JaCoCo to report on our code base
        sourceSets(sourceSets["main"])
        reports {
            xml.required = true
            html.required = false
        }
    }

    // Simple function to invoke a maven goal, dependent on the os, with optional
    register<Maven>("generateWildflyBootableJar") {
        dependsOn("war")
        execGoal("wildfly-jar:package")
    }

    register<Maven>("mavenClean") {
        execGoal("clean")
    }
}

@DisableCachingByDefault(because = "Gradle would require more information to cache this task")
abstract class Maven : Exec() {
    // Simple function to invoke a maven goal, dependent on the os, with optional
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
