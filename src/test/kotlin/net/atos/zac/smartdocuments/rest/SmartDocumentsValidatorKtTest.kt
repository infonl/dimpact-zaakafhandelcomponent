/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.rest

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.string.shouldContain
import net.atos.zac.smartdocuments.exception.SmartDocumentsConfigurationException
import java.util.UUID

class SmartDocumentsValidatorKtTest : BehaviorSpec({

    val smartDocumentsTemplates = setOf(
        createRESTTemplateGroup(
            name = "root",
            groups = setOf(
                createRESTTemplateGroup(
                    name = "group 1",
                    templates = setOf(
                        createRESTTemplate(name = "group 1 template 1"),
                        createRESTTemplate(name = "group 1 template 2")
                    ),
                    groups = emptySet()
                ),
                createRESTTemplateGroup(
                    name = "group 2",
                    templates = setOf(
                        createRESTTemplate(name = "group 2 template 1"),
                        createRESTTemplate(name = "group 2 template 2")
                    ),
                    groups = emptySet()
                )
            ),
            templates = setOf(
                createRESTTemplate(name = "root template 1"),
                createRESTTemplate(name = "root template 2")
            )
        )
    )

    Given("a valid REST request") {
        val expectedInformatieobjectTypeUUID = UUID.randomUUID()
        val restTemplateRequest = setOf(
            createRESTMappedTemplateGroup(
                id = smartDocumentsTemplates.first().id,
                name = "root",
                groups = setOf(
                    createRESTMappedTemplateGroup(
                        id = smartDocumentsTemplates.first().groups?.first()?.id!!,
                        name = "group 1",
                        templates = setOf(
                            createRESTMappedTemplate(
                                id = smartDocumentsTemplates.first().groups?.first()?.templates?.first()?.id!!,
                                name = "group 1 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRESTMappedTemplate(
                                id = smartDocumentsTemplates.first().groups?.first()?.templates?.last()?.id!!,
                                name = "group 1 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    ),
                    createRESTMappedTemplateGroup(
                        id = smartDocumentsTemplates.first().groups?.last()?.id!!,
                        name = "group 2",
                        templates = setOf(
                            createRESTMappedTemplate(
                                id = smartDocumentsTemplates.first().groups?.last()?.templates?.first()?.id!!,
                                name = "group 2 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRESTMappedTemplate(
                                id = smartDocumentsTemplates.first().groups?.last()?.templates?.last()?.id!!,
                                name = "group 2 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    )
                ),
                templates = setOf(
                    createRESTMappedTemplate(
                        id = smartDocumentsTemplates.first().templates?.first()?.id!!,
                        name = "root template 1",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    ),
                    createRESTMappedTemplate(
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
            createRESTMappedTemplateGroup(
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
