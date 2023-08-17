/*
 * SPDX-FileCopyrightText: 2023 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

plugins {
    `java-library`
    `maven-publish`
    `war`

    id("org.openapi.generator") version "6.6.0"
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
    sourceSets["main"].java.srcDir("$rootDir/src/generated/java")
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("--enable-preview"))
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

tasks {
    compileJava {
        dependsOn("generateJavaClients")
    }
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkZoekenClient") {
    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/zoeken-openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.kvk.zoeken.model")
    // TODO: how to prevent generating apis and model docs (overriden pom.xml & README.md even)...?
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    //supportingFilesConstrainedTo.set(emptyList())
    //apiFilesConstrainedTo.set(emptyList())
    //modelFilesConstrainedTo.set(listOf("..."))
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
    // https://stackoverflow.com/questions/62783236/how-can-i-set-openapi-generator-global-properites-in-build-gradle-kts
    // is not working
    // with this setting nothing gets generated anymore..
    //globalProperties.put("modelDocs", "false")
    //globalProperties.put("apis", "false")

    // https://github.com/OpenAPITools/openapi-generator/blob/master/modules/openapi-generator-gradle-plugin/src/main/kotlin/org/openapitools/generator/gradle/plugin/extensions/OpenApiGeneratorGenerateExtension.kt

    // systemProperties.put("apis", "false")
    //systemProperties.put("modelDocs", "false")
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkBasisProfielClient") {
    // these openapi generate tasks cannot be run in parallel because they generate files in the same directory
    mustRunAfter("generateKvkZoekenClient")

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/basisprofiel-openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.kvk.basisprofiel.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKvkVestigingsProfielClient") {
    mustRunAfter("generateKvkBasisProfielClient")

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/kvk/vestigingsprofiel-openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.kvk.vestigingsprofiel.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBrpClient") {
    mustRunAfter("generateKvkVestigingsProfielClient")

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/brp/openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.brp.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateVrlClient") {
    mustRunAfter("generateBrpClient")

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/vrl/openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.vrl.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateBagClient") {
    mustRunAfter("generateVrlClient")

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/bag/openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.bag.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateKlantenClient") {
    mustRunAfter("generateBagClient")

    // was not enabled in Maven build either; model files have been added to the code base itself for some reason
    setEnabled(false)

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/klanten/openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.klanten.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

task<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generateContactMomentenClient") {
    mustRunAfter("generateKlantenClient")

    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api-specs/contactmomenten/openapi.yaml")
    outputDir.set("$rootDir")
    modelPackage.set("net.atos.client.contactmomenten.model")
    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "library" to "microprofile",
            "microprofileRestClientVersion" to "2.0",
            "sourceFolder" to "src/generated/java",
            "dateLibrary" to "java8",
            "disallowAdditionalPropertiesIfNotPresent" to "false"
        )
    )
}

tasks.register("generateJavaClients") {
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

