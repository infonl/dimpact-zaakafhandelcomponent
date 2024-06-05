package net.atos.zac.smartdocuments.templates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.smartdocuments.model.createRESTTemplate
import net.atos.client.smartdocuments.model.createRESTTemplateGroup
import net.atos.client.smartdocuments.model.createTemplatesResponse
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toModel
import net.atos.zac.smartdocuments.templates.SmartDocumentsTemplateConverter.toREST
import net.atos.zac.zaaksturing.model.createZaakafhandelParameters

class SmartDocumentsTemplateConverterTest : BehaviorSpec({

    Given("a template response from SmartDocuments") {
        val templateResponse = createTemplatesResponse()

        When("convert to JPA model is called") {
            val templateGroupsSet = templateResponse.toModel()

            Then("it produces the right models") {
                templateGroupsSet.size shouldBe 1
                with(templateGroupsSet.first()) {
                    name shouldBe "Dimpact"

                    templates.size shouldBe 2
                    with(templates) {
                        first().name shouldBe "Aanvullende informatie nieuw"
                        last().name shouldBe "Aanvullende informatie oud"
                    }

                    with(children) {
                        size shouldBe 2
                        with(first()) {
                            name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                            parent?.name shouldBe "Dimpact"
                            children.size shouldBe 0
                            templates.size shouldBe 1
                            templates.first().name shouldBe "Intern zaaktype voor test volledig gebruik ZAC"
                        }
                        with(last()) {
                            name shouldBe "Indienen aansprakelijkstelling door derden behandelen"
                            parent?.name shouldBe "Dimpact"
                            children.size shouldBe 0
                            templates.size shouldBe 2
                            templates.first().name shouldBe "Data Test"
                            templates.last().name shouldBe "OpenZaakTest"
                        }
                    }
                }
            }
        }

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
        val restTemplateRequest = setOf(
            createRESTTemplateGroup(name = "root").apply {
                groups = setOf(
                    createRESTTemplateGroup(name = "group 1").apply {
                        templates = setOf(
                            createRESTTemplate(name = "group 1 template 1"),
                            createRESTTemplate(name = "group 1 template 2")
                        )
                    },
                    createRESTTemplateGroup(name = "group 2").apply {
                        templates = setOf(
                            createRESTTemplate(name = "group 2 template 1"),
                            createRESTTemplate(name = "group 2 template 2")
                        )
                    }
                )
                templates = setOf(
                    createRESTTemplate(name = "root template 1"),
                    createRESTTemplate(name = "root template 2")
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

                    templates.size shouldBe 2
                    with(templates.first()) {
                        name shouldBe "root template 1"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                        templateGroup.name shouldBe "root"
                    }
                    with(templates.last()) {
                        name shouldBe "root template 2"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                        templateGroup.name shouldBe "root"
                    }

                    children.size shouldBe 2
                    with(children.first()) {
                        name shouldBe "group 1"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture

                        templates.size shouldBe 2
                        with(templates.first()) {
                            name shouldBe "group 1 template 1"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            templateGroup.name shouldBe "group 1"
                        }
                        with(templates.last()) {
                            name shouldBe "group 1 template 2"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            templateGroup.name shouldBe "group 1"
                        }

                        parent!!.name shouldBe "root"
                        children shouldBe emptySet()
                    }
                    with(children.last()) {
                        name shouldBe "group 2"
                        zaakafhandelParameters shouldBe zaakafhandelParametersFixture

                        with(templates.first()) {
                            name shouldBe "group 2 template 1"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            templateGroup.name shouldBe "group 2"
                        }
                        with(templates.last()) {
                            name shouldBe "group 2 template 2"
                            zaakafhandelParameters shouldBe zaakafhandelParametersFixture
                            templateGroup.name shouldBe "group 2"
                        }

                        parent!!.name shouldBe "root"
                        children shouldBe emptySet()
                    }
                }
            }
        }
    }
})
