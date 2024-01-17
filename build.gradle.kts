import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.github.gradle.node.npm.task.NpmTask
import io.smallrye.openapi.api.OpenApiConfig
import java.util.Locale

/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

plugins {
    java
    kotlin("jvm") version "1.9.22"
    war
    jacoco

    id("org.jsonschema2pojo") version "1.2.1"
    id("org.openapi.generator") version "7.2.0"
    id("com.github.node-gradle.node") version "7.0.1"
    id("org.barfuin.gradle.taskinfo") version "2.1.0"
    id("io.smallrye.openapi") version "3.8.0"
    id("org.hidetake.swagger.generator") version "2.19.2"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("com.bmuschko.docker-remote-api") version "9.4.0"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/infonl/webdav-servlet")
        credentials {
            // for local development please create a personal access token (or use an existing one)
            // with the 'read:packages' scope and set the 'gpr.user' and 'gpr.key' properties in
            // your ~/.gradle/gradle.properties file (create the file if it does not exist yet)
            username = project.findProperty("gpr.user") as String? ?: System.getenv("READ_PACKAGES_USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("READ_PACKAGES_TOKEN")
        }
    }
}

group = "net.atos.common-ground"
description = "Zaakafhandelcomponent"

// we will only upgrade Java when WildFly explicitly supports a new version
val javaVersion = JavaVersion.VERSION_17

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
val warLib by configurations.creating {
    extendsFrom(configurations["compileOnly"])
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
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-io:commons-io:2.15.1")
    implementation("com.opencsv:opencsv:5.9")
    implementation("org.flowable:flowable-engine:7.0.0")
    implementation("org.flowable:flowable-cdi:7.0.0")
    implementation("org.flowable:flowable-cmmn-engine:7.0.0")
    implementation("org.flowable:flowable-cmmn-cdi:7.0.0")
    implementation("org.flowable:flowable-cmmn-engine-configurator:7.0.0")
    implementation("org.slf4j:slf4j-jdk14:2.0.11")
    implementation("com.auth0:java-jwt:4.4.0")
    implementation("javax.cache:cache-api:1.1.1")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("com.mailjet:mailjet-client:5.2.5")
    implementation("org.flywaydb:flyway-core:10.6.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.5.0")
    implementation("org.apache.solr:solr-solrj:9.4.0")
    implementation("nl.info.webdav:webdav-servlet:1.2.13")
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    implementation("com.itextpdf.tool:xmlworker:5.5.13.3")
    implementation("net.sourceforge.htmlcleaner:htmlcleaner:2.29")
    implementation("com.unboundid:unboundid-ldapsdk:6.0.11")

    swaggerUI("org.webjars:swagger-ui:5.10.3")

    // enable detekt formatting rules. see: https://detekt.dev/docs/rules/formatting/
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.4")

    runtimeOnly("org.infinispan:infinispan-jcache:14.0.21.Final")
    runtimeOnly("org.infinispan:infinispan-cdi-embedded:14.0.21.Final")

    // declare dependencies that are required in the generated WAR; see war section below
    // simply marking them as 'compileOnly' or 'implementation' does not work
    warLib("org.apache.httpcomponents:httpclient:4.5.14")
    warLib("org.reactivestreams:reactive-streams:1.0.4")
    // WildFly does already include the Jakarta Mail API lib so not sure why, but we need to
    // include it in the WAR or else ZAC will fail to be deployed
    warLib("jakarta.mail:jakarta.mail-api:2.1.2")

    // dependencies provided by Wildfly 30
    providedCompile("jakarta.platform:jakarta.jakartaee-api:10.0.0")
    providedCompile("org.eclipse.microprofile.rest.client:microprofile-rest-client-api:3.0.1")
    providedCompile("org.eclipse.microprofile.config:microprofile-config-api:3.0.2")
    providedCompile("org.eclipse.microprofile.health:microprofile-health-api:4.0.1")
    providedCompile("org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:4.0.2")
    providedCompile("org.jboss.resteasy:resteasy-multipart-provider:6.2.6.Final")
    providedCompile("org.wildfly.security:wildfly-elytron-http-oidc:2.2.2.Final")
    providedCompile("org.hibernate.validator:hibernate-validator:8.0.1.Final")

    // yasson is required for using a JSONB context in our unit tests
    // where we do not have the WildFly runtime environment available
    testImplementation("org.eclipse:yasson:3.0.3")
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.mockk:mockk:1.13.9")

    // integration test dependencies
    "itestImplementation"("org.testcontainers:testcontainers:1.19.3")
    "itestImplementation"("org.testcontainers:mockserver:1.19.3")
    "itestImplementation"("org.testcontainers:postgresql:1.19.3")
    "itestImplementation"("io.kotest:kotest-runner-junit5:5.8.0")
    "itestImplementation"("io.kotest:kotest-assertions-json:5.8.0")
    "itestImplementation"("org.slf4j:slf4j-simple:2.0.11")
    "itestImplementation"("io.github.oshai:kotlin-logging-jvm:6.0.3")
    "itestImplementation"("org.danilopianini:khttp:1.4.3")
    "itestImplementation"("org.awaitility:awaitility-kotlin:4.2.0")
    "itestImplementation"("org.mock-server:mockserver-client-java:5.15.0")
}

detekt {
    config.setFrom("$rootDir/config/detekt.yml")
    source.setFrom("src/main/kotlin", "src/test/kotlin", "src/itest/kotlin")
    // our Detekt configuration build builds upon the default configuration
    buildUponDefaultConfig = true
}

jacoco {
    toolVersion = "0.8.7"
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
    // generates Java model files for the "gemeente Den Haag productaanvraag" JSON schema
    setSource(files("$rootDir/src/main/resources/json-schema"))
    targetDirectory = file("$rootDir/src/generated/java")
    setFileExtensions(".schema.json")
    targetPackage = "net.atos.zac.aanvraag"
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
    version.set("18.13.0")
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

// run npm install task after generating the Java clients because they
// share the same output folder (= $rootDir)
tasks.getByName("npmInstall").setMustRunAfter(listOf("generateJavaClients"))
tasks.getByName("generateSwaggerUIZaakafhandelcomponent").setDependsOn(listOf("generateOpenApiSpec"))
tasks.getByName("compileItestKotlin").setMustRunAfter(listOf("buildDockerImage"))

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
        delete("$rootDir/src/generated")
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

    withType<Test> {
        useJUnitPlatform()
    }

    withType<Javadoc> {
        options.encoding = "UTF-8"
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("--enable-preview")
    }

    withType<org.openapitools.generator.gradle.plugin.tasks.GenerateTask> {
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
                "microprofileRestClientVersion" to "3.0",
                "sourceFolder" to "",
                "dateLibrary" to "java8",
                "disallowAdditionalPropertiesIfNotPresent" to "false",
                "openApiNullable" to "false",
                "useJakartaEe" to "true"
            )
        )
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkZoekenClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/zoeken-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.zoeken.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkBasisProfielClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.basisprofiel.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkVestigingsProfielClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.vestigingsprofiel.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBrpClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/brp/openapi.yaml")
        modelPackage.set("net.atos.client.brp.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateVrlClient") {
        // disabled for now because the Java code generated by this task contains @JsonbCreator annotations
        // but the corresponding "import jakarta.json.bind.annotation.JsonbCreator;" import statements are missing
        // for some reason.
        // n.b. switching to jackson instead of jsonb is no option because it causes other issues
        // related: https://github.com/OpenAPITools/openapi-generator/blob/92daacd6a25873847886ac2360193a1303208300/modules/openapi-generator/src/main/resources/Java/model.mustache#L39
        // and: https://github.com/OpenAPITools/openapi-generator/blob/92daacd6a25873847886ac2360193a1303208300/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AbstractJavaCodegen.java#L1505
        isEnabled = false

        inputSpec.set("$rootDir/src/main/resources/api-specs/vrl/openapi.yaml")
        modelPackage.set("net.atos.client.vrl.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBagClient") {
        inputSpec.set("$rootDir/src/main/resources/api-specs/bag/openapi.yaml")
        modelPackage.set("net.atos.client.bag.model")
        // we use a different date library for this client
        configOptions.set(
            mapOf(
                "library" to "microprofile",
                "microprofileRestClientVersion" to "3.0",
                "sourceFolder" to "",
                "dateLibrary" to "java8-localdatetime",
                "disallowAdditionalPropertiesIfNotPresent" to "false",
                "openApiNullable" to "false",
                "useJakartaEe" to "true"
            )
        )
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKlantenClient") {
        // this task was not enabled in the original Maven build either; these model files were added to the code base manually instead
        isEnabled = false

        inputSpec.set("$rootDir/src/main/resources/api-specs/klanten/openapi.yaml")
        modelPackage.set("net.atos.client.klanten.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateContactMomentenClient") {
        // disabled for now because the Java code generated by this task contains @JsonbCreator annotations
        // but the corresponding "import jakarta.json.bind.annotation.JsonbCreator;" import statements are missing
        // for some reason
        // n.b. switching to jackson instead of jsonb is no option because it causes other issues
        // related: https://github.com/OpenAPITools/openapi-generator/blob/92daacd6a25873847886ac2360193a1303208300/modules/openapi-generator/src/main/resources/Java/model.mustache#L39
        // and: https://github.com/OpenAPITools/openapi-generator/blob/92daacd6a25873847886ac2360193a1303208300/modules/openapi-generator/src/main/java/org/openapitools/codegen/languages/AbstractJavaCodegen.java#L1505
        isEnabled = false

        inputSpec.set("$rootDir/src/main/resources/api-specs/contactmomenten/openapi.yaml")
        modelPackage.set("net.atos.client.contactmomenten.model")
    }

    register("generateJavaClients") {
        dependsOn(
            "generateKvkZoekenClient",
            "generateKvkBasisProfielClient",
            "generateKvkVestigingsProfielClient",
            "generateBrpClient",
            "generateVrlClient",
            "generateBagClient",
            "generateKlantenClient",
            "generateContactMomentenClient"
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

        // the Jest junit reporter generates file: src/main/app/reports/report.xml
        outputs.dir("src/main/app/reports")
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
        dockerFile.set(file("Containerfile"))
        images.add(zacDockerImage)
    }

    register<Test>("itest") {
        inputs.files(project.tasks.findByPath("compileItestKotlin")!!.outputs.files)

        testClassesDirs = sourceSets["itest"].output.classesDirs
        classpath = sourceSets["itest"].runtimeClasspath

        systemProperty("zacDockerImage", zacDockerImage)
        // note that the PATCH (and PUT?) HTTP requests in the integration tests currently
        // require the following environment variable to be set
        // see: https://github.com/lojewalo/khttp/issues/88
        environment("JAVA_TOOL_OPTIONS", "--add-opens=java.base/java.net=ALL-UNNAMED")
    }

    register<Exec>("generateWildflyBootableJar") {
        dependsOn("war")
        if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) {
            commandLine("./mvnw.cmd", "wildfly-jar:package")
        } else {
            commandLine("./mvnw", "wildfly-jar:package")
        }
    }

    register<Exec>("mavenClean") {
        if (System.getProperty("os.name").lowercase(Locale.ROOT).contains("windows")) {
            commandLine("./mvnw.cmd", "clean")
        } else {
            commandLine("./mvnw", "clean")
        }
    }
}
