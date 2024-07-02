/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.templates

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.atos.client.smartdocuments.model.createTemplatesResponse
import net.atos.zac.smartdocuments.SmartDocumentsException
import net.atos.zac.smartdocuments.rest.createRESTTemplate
import net.atos.zac.smartdocuments.rest.createRESTTemplateGroup
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toModel
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toREST
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toStringRepresentation
import net.atos.zac.smartdocuments.templates.model.createSmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.createSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.validate
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters
import java.util.UUID

class SmartDocumentsTemplateConverterTest : BehaviorSpec({

    Given("a template response from SmartDocuments") {
        val templateResponse = createTemplatesResponse()

        When("convert to REST is called") {
            val restTemplateGroup = templateResponse.toREST()

            Then("it produces the right rest model") {
                restTemplateGroup.size shouldBe 1
                with(restTemplateGroup.first()) {
                    id shouldBe templateResponse.documentsStructure.templatesStructure.templateGroups.first().id
                    name shouldBe "Dimpact"

                    with(groups!!.first()) {
                        name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                        templates!!.size shouldBe 1
                        templates!!.first().name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                    }

                    with(groups!!.last()) {
                        name shouldBe "Indienen aansprakelijkstelling door derden behandelen"
                        templates!!.size shouldBe 2
                        templates!!.first().name shouldBe "Data Test"
                        templates!!.last().name shouldBe "OpenZaakTest"
                    }

                    templates!!.size shouldBe 2
                    with(templates!!) {
                        first().name shouldBe "Aanvullende informatie nieuw"
                        last().name shouldBe "Aanvullende informatie oud"
                    }
                }
            }
        }
    }

    Given("a REST request") {
        val expectedInformatieobjectTypeUUID = UUID.randomUUID()
        val restTemplateRequest = setOf(
            createRESTTemplateGroup(name = "root").apply {
                groups = setOf(
                    createRESTTemplateGroup(name = "group 1").apply {
                        templates = setOf(
                            createRESTTemplate(name = "group 1 template 1", informatieObjectTypeUUID = expectedInformatieobjectTypeUUID),
                            createRESTTemplate(name = "group 1 template 2", informatieObjectTypeUUID = expectedInformatieobjectTypeUUID)
                        )
                        groups = emptySet()
                    },
                    createRESTTemplateGroup(name = "group 2").apply {
                        templates = setOf(
                            createRESTTemplate(name = "group 2 template 1", informatieObjectTypeUUID = expectedInformatieobjectTypeUUID),
                            createRESTTemplate(name = "group 2 template 2", informatieObjectTypeUUID = expectedInformatieobjectTypeUUID)
                        )
                        groups = emptySet()
                    }
                )
                templates = setOf(
                    createRESTTemplate(name = "root template 1", informatieObjectTypeUUID = expectedInformatieobjectTypeUUID),
                    createRESTTemplate(name = "root template 2", informatieObjectTypeUUID = expectedInformatieobjectTypeUUID)
                )
            }
        )

        When("convert to JPA model is called") {
            val zaakafhandelParametersFixture = createZaakafhandelParameters()
            val jpaModel = restTemplateRequest.toModel(zaakafhandelParametersFixture)

            Then("it produces a correct jpa representation") {
                jpaModel.size shouldBe 1
                with(jpaModel.first()) {
                    name shouldBe "root"
                    zaakafhandelParameters shouldBe zaakafhandelParametersFixture

                    templates!!.size shouldBe 2
                    with(templates!!.first()) {
                        name shouldBe "root template 1"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                        informatieObjectTypeUUID shouldBe expectedInformatieobjectTypeUUID
                        templateGroup.name shouldBe "root"
                    }
                    with(templates!!.last()) {
                        name shouldBe "root template 2"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                        informatieObjectTypeUUID shouldBe expectedInformatieobjectTypeUUID
                        templateGroup.name shouldBe "root"
                    }

                    children!!.size shouldBe 2
                    with(children!!.first()) {
                        name shouldBe "group 1"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture

                        templates!!.size shouldBe 2
                        with(templates!!.first()) {
                            name shouldBe "group 1 template 1"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            informatieObjectTypeUUID shouldBe expectedInformatieobjectTypeUUID
                            templateGroup.name shouldBe "group 1"
                        }
                        with(templates!!.last()) {
                            name shouldBe "group 1 template 2"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            informatieObjectTypeUUID shouldBe expectedInformatieobjectTypeUUID
                            templateGroup.name shouldBe "group 1"
                        }

                        parent!!.name shouldBe "root"
                        children shouldBe null
                    }
                    with(children!!.last()) {
                        name shouldBe "group 2"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture

                        with(templates!!.first()) {
                            name shouldBe "group 2 template 1"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            informatieObjectTypeUUID shouldBe expectedInformatieobjectTypeUUID
                            templateGroup.name shouldBe "group 2"
                        }
                        with(templates!!.last()) {
                            name shouldBe "group 2 template 2"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            informatieObjectTypeUUID shouldBe expectedInformatieobjectTypeUUID
                            templateGroup.name shouldBe "group 2"
                        }

                        parent!!.name shouldBe "root"
                        children shouldBe null
                    }
                }
            }
        }

        When("convert to string representation is requested") {
            val stringSet = restTemplateRequest.toStringRepresentation()

            Then("it produces a correct set of strings") {
                stringSet.size shouldBe 9

                with(restTemplateRequest.first()) {
                    val rootId = id
                    val rootTemplate1Id = templates!!.first().id
                    val rootTemplate2Id = templates!!.last().id
                    val group1Id = groups!!.first().id
                    val group2Id = groups!!.last().id
                    val group1Template1Id = groups!!.first().templates!!.first().id
                    val group1Template2Id = groups!!.first().templates!!.last().id
                    val group2Template1Id = groups!!.last().templates!!.first().id
                    val group2Template2Id = groups!!.last().templates!!.last().id

                    stringSet shouldContainAll setOf(
                        "group.$rootId.root",
                        "group.$rootId.root.template.$rootTemplate1Id.root template 1",
                        "group.$rootId.root.template.$rootTemplate2Id.root template 2",
                        "group.$rootId.root.group.$group1Id.group 1",
                        "group.$rootId.root.group.$group1Id.group 1.template.$group1Template1Id.group 1 template 1",
                        "group.$rootId.root.group.$group1Id.group 1.template.$group1Template2Id.group 1 template 2",
                        "group.$rootId.root.group.$group2Id.group 2",
                        "group.$rootId.root.group.$group2Id.group 2.template.$group2Template1Id.group 2 template 1",
                        "group.$rootId.root.group.$group2Id.group 2.template.$group2Template2Id.group 2 template 2"
                    )
                }
            }
        }

        When("validating with the same rest request as a superset") {
            restTemplateRequest.validate(restTemplateRequest)
            Then("it does not error") {}
        }

        When("validating invalid rest request") {
            val invalidRestTemplateRequest = setOf(
                createRESTTemplateGroup(name = "non-existing")
            )
            val exception = shouldThrow<SmartDocumentsException> {
                invalidRestTemplateRequest.validate(restTemplateRequest)
            }

            Then("error should hint what's wrong") {
                exception.message shouldContain "non-existing"
            }
        }
    }

    Given("a JPA model") {
        val jpaRoot = createSmartDocumentsTemplateGroup(name = "root")
        val jpaTemplates = mutableSetOf(
            createSmartDocumentsTemplate(name = "template 1"),
            createSmartDocumentsTemplate(name = "template 2")
        )
        val jpaGroups = mutableSetOf(
            createSmartDocumentsTemplateGroup(name = "group 1").apply {
                parent = jpaRoot
                templates = jpaTemplates
                children = mutableSetOf()
            },
            createSmartDocumentsTemplateGroup(name = "group 2").apply {
                parent = jpaRoot
                templates = jpaTemplates
                children = mutableSetOf()
            }
        )
        val jpaModel = setOf(
            jpaRoot.apply {
                children = jpaGroups
                templates = jpaTemplates
            }
        )

        When("a convert to REST model is called") {
            val restModel = jpaModel.toREST()

            Then("it produces a correct REST model") {
                restModel.size shouldBe 1
                with(restModel.first()) {
                    id shouldBe jpaRoot.smartDocumentsId
                    name shouldBe "root"

                    templates!!.size shouldBe 2
                    with(templates!!.first()) {
                        id shouldBe jpaTemplates.first().smartDocumentsId
                        name shouldBe "template 1"
                    }
                    with(templates!!.last()) {
                        id shouldBe jpaTemplates.last().smartDocumentsId
                        name shouldBe "template 2"
                    }

                    groups!!.size shouldBe 2
                    with(groups!!.first()) {
                        id shouldBe jpaGroups.first().smartDocumentsId
                        name shouldBe "group 1"
                        templates!!.size shouldBe 2
                        templates!!.first().name shouldBe "template 1"
                        templates!!.last().name shouldBe "template 2"
                        groups shouldBe null
                    }
                    with(groups!!.last()) {
                        id shouldBe jpaGroups.last().smartDocumentsId
                        name shouldBe "group 2"
                        templates!!.size shouldBe 2
                        templates!!.first().name shouldBe "template 1"
                        templates!!.last().name shouldBe "template 2"
                        groups shouldBe null
                    }
                }
            }
        }
    }
})
