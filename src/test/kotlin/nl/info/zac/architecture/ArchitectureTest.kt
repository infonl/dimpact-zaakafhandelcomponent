/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.architecture

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields
import com.tngtech.archunit.library.freeze.FreezingArchRule.freeze
import io.kotest.core.spec.style.BehaviorSpec

private const val REST_LAYER_PACKAGE = "nl.info.zac.app.."
private const val CLIENT_PACKAGE = "nl.info.client.."
private const val GENERATED_PACKAGE = "..generated.."

class ArchitectureTest : BehaviorSpec({
    val zacClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("nl.info", "net.atos")

    context("The dependencies of the domain code") {
        given("the domain classes outside the REST layer") {
            `when`("their dependencies are checked") {
                then("they do not depend on the REST layer, apart from the frozen existing violations") {
                    freeze(
                        noClasses()
                            .that().resideInAnyPackage("nl.info.zac..", "net.atos..")
                            .and().resideOutsideOfPackage(REST_LAYER_PACKAGE)
                            .should().dependOnClassesThat().resideInAPackage(REST_LAYER_PACKAGE)
                    ).check(zacClasses)
                }
            }
        }
    }

    context("The dependencies of the clients of external APIs") {
        given("the client classes") {
            `when`("their dependencies are checked") {
                then("they do not depend on the REST layer, apart from the frozen existing violations") {
                    freeze(
                        noClasses()
                            .that().resideInAPackage(CLIENT_PACKAGE)
                            .should().dependOnClassesThat().resideInAPackage(REST_LAYER_PACKAGE)
                    ).check(zacClasses)
                }
            }
        }
    }

    context("The coding conventions") {
        given("all ZAC classes") {
            `when`("their fields are checked") {
                then("no field is annotated with @Inject, because ZAC uses constructor injection") {
                    noFields().should().beAnnotatedWith("jakarta.inject.Inject").check(zacClasses)
                }
            }
        }

        given("the ZAC classes that are not generated") {
            `when`("their dependencies are checked") {
                then("they do not depend on the javax Java EE APIs that Jakarta EE replaced") {
                    noClasses()
                        .that().resideOutsideOfPackage(GENERATED_PACKAGE)
                        .should().dependOnClassesThat().resideInAnyPackage(
                            "javax.annotation",
                            "javax.annotation.security..",
                            "javax.annotation.sql..",
                            "javax.ejb..",
                            "javax.enterprise..",
                            "javax.inject..",
                            "javax.jms..",
                            "javax.json..",
                            "javax.mail..",
                            "javax.persistence..",
                            "javax.security.enterprise..",
                            "javax.servlet..",
                            "javax.transaction",
                            "javax.validation..",
                            "javax.websocket..",
                            "javax.ws.rs.."
                        )
                        .check(zacClasses)
                }
            }
        }
    }
})
