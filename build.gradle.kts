/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

plugins {
    id("java-library")
    id("maven-publish")
    id("war")

    id("org.jsonschema2pojo") version "1.2.1"
    id("org.openapi.generator") version "6.6.0"
    id("com.github.node-gradle.node") version "7.0.0"
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

dependencies {
    api("org.apache.commons:commons-lang3:3.12.0")
    api("org.apache.commons:commons-text:1.10.0")
    api("org.apache.commons:commons-collections4:4.4")
    api("com.opencsv:opencsv:5.7.0")
    api("org.flowable:flowable-engine:6.7.2")
    api("org.flowable:flowable-cdi:6.7.2")
    api("org.flowable:flowable-cmmn-engine:6.7.2")
    api("org.flowable:flowable-cmmn-cdi:6.7.2")
    api("org.flowable:flowable-cmmn-engine-configurator:6.7.2")
    api("org.slf4j:slf4j-jdk14:2.0.3")
    api("com.auth0:java-jwt:4.0.0")
    api("javax.cache:cache-api:1.1.1")
    api("com.google.guava:guava:30.1.1-jre")
    api("com.mailjet:mailjet-client:5.2.1")
    api("org.flywaydb:flyway-core:9.4.0")
    api("org.apache.solr:solr-solrj:9.1.0")
    api("net.sf.webdav-servlet:webdav-servlet:2.0")
    api("com.itextpdf:itextpdf:5.5.13")
    api("com.itextpdf.tool:xmlworker:5.5.13")
    api("net.sourceforge.htmlcleaner:htmlcleaner:2.6.1")
    runtimeOnly("org.infinispan:infinispan-jcache:13.0.10.Final")
    runtimeOnly("org.infinispan:infinispan-cdi-embedded:13.0.10.Final")
    testImplementation("org.eclipse:yasson:1.0.11")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    // provided in Wildfly
    providedCompile("jakarta.platform:jakarta.jakartaee-api:8.0.0")
    providedCompile("org.eclipse.microprofile.rest.client:microprofile-rest-client-api:2.0")
    providedCompile("org.eclipse.microprofile.config:microprofile-config-api:2.0")
    providedCompile("org.eclipse.microprofile.health:microprofile-health-api:3.1")
    providedCompile("org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api:3.0")
    providedCompile("org.jboss.resteasy:resteasy-multipart-provider:4.7.7.Final")
    providedCompile("org.wildfly.security:wildfly-elytron-http-oidc:1.19.1.Final")
}

group = "net.atos.common-ground"
version = "latest-SNAPSHOT"
description = "Zaakafhandelcomponent"

java {
    java.sourceCompatibility = JavaVersion.VERSION_17
    java.targetCompatibility = JavaVersion.VERSION_17

    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }

    // add our generated client code to the main source set
    sourceSets["main"].java.srcDir("$rootDir/src/generated/java")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

jsonSchema2Pojo {
    // generates Java model files for the "gemeente Den Haag productaanvraag" JSON schema
    setSource(files("${rootDir}/src/main/resources/json-schema"))
    targetDirectory = file("${rootDir}/src/generated/java")
    setFileExtensions(".schema.json")
    targetPackage = "net.atos.zac.aanvraag"
    setAnnotationStyle("JSONB1")
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
    version.set("18.10.0")
    distBaseUrl.set("https://nodejs.org/dist")
    nodeProjectDir.set(file("${rootDir}/src/main/app"))
}

tasks {
    processResources {
        dependsOn("generateJavaClients")
        dependsOn("buildFrontend")
    }

    withType<Javadoc>() {
        options.encoding = "UTF-8"
    }

    withType<JavaCompile>() {
        options.encoding = "UTF-8"
        options.compilerArgs.add("--enable-preview")
    }

    clean {
        delete("$rootDir/src/main/app/dist")
        delete("$rootDir/src/generated")
        // what about /src/main/app/.angular and /src/main/app/node_modules?
    }

    withType<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>() {
        generatorName.set("java")
        outputDir.set("$rootDir")
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
                "microprofileRestClientVersion" to "2.0",
                "sourceFolder" to "src/generated/java",
                "dateLibrary" to "java8",
                "disallowAdditionalPropertiesIfNotPresent" to "false",
                "openApiNullable" to "false"
            )
        )
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkZoekenClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/zoeken-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.zoeken.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkBasisProfielClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateKvkZoekenClient")

        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.basisprofiel.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkVestigingsProfielClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateKvkBasisProfielClient")

        inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml")
        modelPackage.set("net.atos.client.kvk.vestigingsprofiel.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBrpClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateKvkVestigingsProfielClient")

        inputSpec.set("$rootDir/src/main/resources/api-specs/brp/openapi.yaml")
        modelPackage.set("net.atos.client.brp.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateVrlClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateBrpClient")

        inputSpec.set("$rootDir/src/main/resources/api-specs/vrl/openapi.yaml")
        modelPackage.set("net.atos.client.vrl.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBagClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateVrlClient")

        inputSpec.set("$rootDir/src/main/resources/api-specs/bag/openapi.yaml")
        modelPackage.set("net.atos.client.bag.model")
        // we use a different date library for this client
        configOptions.set(
            mapOf(
                "library" to "microprofile",
                "microprofileRestClientVersion" to "2.0",
                "sourceFolder" to "src/generated/java",
                "dateLibrary" to "java8-localdatetime",
                "disallowAdditionalPropertiesIfNotPresent" to "false",
                "openApiNullable" to "false"
            )
        )
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKlantenClient") {
        // this task was not enabled in the original Maven build either; these model files were added to the code base manually instead
        isEnabled = false

        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateBagClient")

        inputSpec.set("$rootDir/src/main/resources/api-specs/klanten/openapi.yaml")
        modelPackage.set("net.atos.client.klanten.model")
    }

    register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateContactMomentenClient") {
        // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
        mustRunAfter("generateKlantenClient")

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
            "generateContactMomentenClient",
        )
    }

    register("buildFrontend") {
        // run build frontend tasks after generating the Java clients because these tasks
        // use the same output folder (= $rootDir)
        mustRunAfter("generateJavaClients")
        getByName("npmInstall").setMustRunAfter(listOf("generateJavaClients"))

        dependsOn("npmInstall")
        dependsOn("npm_run_build")
    }
}

