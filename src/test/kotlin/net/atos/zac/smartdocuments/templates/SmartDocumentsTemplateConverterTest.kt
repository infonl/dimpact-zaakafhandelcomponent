package net.atos.zac.smartdocuments.templates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.client.smartdocuments.model.createTemplatesResponse

class SmartDocumentsTemplateConverterTest : BehaviorSpec({

    Given("a template response from SmartDocuments") {
        val templateResponse = createTemplatesResponse()

        When("convert to JPA model is called") {
            val templateGroupsSet = SmartDocumentsTemplateConverter.convert(templateResponse)

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
    }
})
