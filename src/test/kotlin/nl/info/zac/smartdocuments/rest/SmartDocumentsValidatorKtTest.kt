/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import nl.info.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import java.util.UUID

class SmartDocumentsValidatorKtTest : BehaviorSpec({

    val smartDocumentsTemplates = setOf(
        createRestSmartDocumentsTemplateGroup(
            name = "root",
            groups = setOf(
                createRestSmartDocumentsTemplateGroup(
                    name = "group 1",
                    templates = setOf(
                        createRestSmartDocumentsTemplate(name = "group 1 template 1"),
                        createRestSmartDocumentsTemplate(name = "group 1 template 2")
                    ),
                    groups = emptySet()
                ),
                createRestSmartDocumentsTemplateGroup(
                    name = "group 2",
                    templates = setOf(
                        createRestSmartDocumentsTemplate(name = "group 2 template 1"),
                        createRestSmartDocumentsTemplate(name = "group 2 template 2")
                    ),
                    groups = emptySet()
                )
            ),
            templates = setOf(
                createRestSmartDocumentsTemplate(name = "root template 1"),
                createRestSmartDocumentsTemplate(name = "root template 2")
            )
        )
    )

    Given("a valid rest template request") {
        val expectedInformatieobjectTypeUUID = UUID.randomUUID()
        val restTemplateRequest = setOf(
            createRestMappedSmartDocumentsTemplateGroup(
                id = smartDocumentsTemplates.first().id,
                name = "root",
                groups = setOf(
                    createRestMappedSmartDocumentsTemplateGroup(
                        id = smartDocumentsTemplates.first().groups?.first()?.id!!,
                        name = "group 1",
                        templates = setOf(
                            createRestMappedSmartDocumentsTemplate(
                                id = smartDocumentsTemplates.first().groups?.first()?.templates?.first()?.id!!,
                                name = "group 1 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRestMappedSmartDocumentsTemplate(
                                id = smartDocumentsTemplates.first().groups?.first()?.templates?.last()?.id!!,
                                name = "group 1 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    ),
                    createRestMappedSmartDocumentsTemplateGroup(
                        id = smartDocumentsTemplates.first().groups?.last()?.id!!,
                        name = "group 2",
                        templates = setOf(
                            createRestMappedSmartDocumentsTemplate(
                                id = smartDocumentsTemplates.first().groups?.last()?.templates?.first()?.id!!,
                                name = "group 2 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRestMappedSmartDocumentsTemplate(
                                id = smartDocumentsTemplates.first().groups?.last()?.templates?.last()?.id!!,
                                name = "group 2 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    )
                ),
                templates = setOf(
                    createRestMappedSmartDocumentsTemplate(
                        id = smartDocumentsTemplates.first().templates?.first()?.id!!,
                        name = "root template 1",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    ),
                    createRestMappedSmartDocumentsTemplate(
                        id = smartDocumentsTemplates.first().templates?.last()?.id!!,
                        name = "root template 2",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    )
                )
            )
        )

        When("validating with the same rest request as a superset") {
            restTemplateRequest isSubsetOf smartDocumentsTemplates
            Then("it does not error") {}
        }
    }

    Given("an invalid REST request") {
        val invalidRestTemplateRequest = setOf(
            createRestMappedSmartDocumentsTemplateGroup(
                id = "000-000",
                name = "non-existing",
                groups = emptySet(),
                templates = emptySet()
            )
        )

        When("validating invalid rest request") {
            val exception = shouldThrow<SmartDocumentsConfigurationException> {
                invalidRestTemplateRequest isSubsetOf smartDocumentsTemplates
            }

            Then("error should hint what's wrong") {
                exception.message shouldContain "non-existing"
            }
        }
    }
})
