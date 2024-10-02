/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.smartdocuments.templates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.smartdocuments.model.createsmartDocumentsTemplatesResponse
import net.atos.zac.admin.model.createZaakafhandelParameters
import net.atos.zac.smartdocuments.rest.createRESTMappedTemplate
import net.atos.zac.smartdocuments.rest.createRESTMappedTemplateGroup
import net.atos.zac.smartdocuments.rest.toRestSmartDocumentsTemplateGroup
import net.atos.zac.smartdocuments.rest.toRestSmartDocumentsTemplateGroupSet
import net.atos.zac.smartdocuments.rest.toSmartDocumentsTemplateGroupSet
import net.atos.zac.smartdocuments.templates.model.createSmartDocumentsTemplate
import net.atos.zac.smartdocuments.templates.model.createSmartDocumentsTemplateGroup
import java.util.UUID

class SmartDocumentsTemplateConverterTest : BehaviorSpec({

    Given("a template response from SmartDocuments") {
        val templateResponse = createsmartDocumentsTemplatesResponse()

        When("convert to REST is called") {
            val restTemplateGroup = templateResponse.toRestSmartDocumentsTemplateGroupSet()

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
            createRESTMappedTemplateGroup(
                name = "root",
                groups = setOf(
                    createRESTMappedTemplateGroup(
                        name = "group 1",
                        templates = setOf(
                            createRESTMappedTemplate(
                                name = "group 1 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRESTMappedTemplate(
                                name = "group 1 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    ),
                    createRESTMappedTemplateGroup(
                        name = "group 2",
                        templates = setOf(
                            createRESTMappedTemplate(
                                name = "group 2 template 1",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            ),
                            createRESTMappedTemplate(
                                name = "group 2 template 2",
                                informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                            )
                        ),
                        groups = emptySet()
                    )
                ),
                templates = setOf(
                    createRESTMappedTemplate(
                        name = "root template 1",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    ),
                    createRESTMappedTemplate(
                        name = "root template 2",
                        informatieObjectTypeUUID = expectedInformatieobjectTypeUUID
                    )
                )
            )
        )

        When("convert to JPA model is called") {
            val zaakafhandelParametersFixture = createZaakafhandelParameters()
            val jpaModel = restTemplateRequest.toSmartDocumentsTemplateGroupSet(zaakafhandelParametersFixture)

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
            val restModel = jpaModel.toRestSmartDocumentsTemplateGroup()

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
