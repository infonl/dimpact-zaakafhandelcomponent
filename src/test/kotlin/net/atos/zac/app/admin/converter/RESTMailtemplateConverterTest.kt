/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.atos.zac.app.admin.converter.RESTMailtemplateConverter
import net.atos.zac.app.admin.model.createRestMailTemplate
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.createMailTemplate

class RESTMailtemplateConverterTest : BehaviorSpec({

    Context("convertForCreate method") {
        Given("A REST mail template with an ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 999L,
                mailTemplateName = "Test Template",
                subject = "<p>Test Subject</p>",
                body = "Test Body",
                mail = Mail.ZAAK_ALGEMEEN.name,
                defaultTemplate = true
            )

            When("convertForCreate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

                Then("the domain model should not have an ID set") {
                    domainMailTemplate.id shouldBe 0L
                }

                And("all other fields should be correctly mapped") {
                    domainMailTemplate.mailTemplateNaam shouldBe "Test Template"
                    domainMailTemplate.onderwerp shouldBe "Test Subject" // HTML tags stripped
                    domainMailTemplate.body shouldBe "Test Body"
                    domainMailTemplate.mail shouldBe Mail.ZAAK_ALGEMEEN
                    domainMailTemplate.isDefaultMailtemplate shouldBe true
                }
            }
        }

        Given("A REST mail template with HTML paragraph tags in subject") {
            val restMailTemplate = createRestMailTemplate(
                subject = "<p>Complex</p><p>Subject</p><p>With</p><p>Tags</p>"
            )

            When("convertForCreate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

                Then("HTML paragraph tags should be stripped from subject") {
                    domainMailTemplate.onderwerp shouldBe "ComplexSubjectWithTags"
                }
            }
        }

        Given("A REST mail template with different mail types") {
            listOf(
                Mail.ZAAK_ALGEMEEN,
                Mail.SIGNALERING_TAAK_OP_NAAM,
            ).forEach { mailType ->
                val restMailTemplate = createRestMailTemplate(mail = mailType.name)

                When("convertForCreate is called with mail type ${mailType.name}") {
                    val domainMailTemplate = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

                    Then("the mail enum should be correctly converted") {
                        domainMailTemplate.mail shouldBe mailType
                    }
                }
            }
        }
    }

    Context("convertForUpdate method") {
        Given("A REST mail template with an ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 123L,
                mailTemplateName = "Updated Template",
                subject = "<p>Updated Subject</p>",
                body = "Updated Body",
                mail = Mail.ZAAK_ALGEMEEN.name,
                defaultTemplate = false
            )

            When("convertForUpdate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                Then("the domain model should not have an ID set (will be set by service layer)") {
                    domainMailTemplate.id shouldBe 0L
                }

                And("all other fields should be correctly mapped") {
                    domainMailTemplate.mailTemplateNaam shouldBe "Updated Template"
                    domainMailTemplate.onderwerp shouldBe "Updated Subject" // HTML tags stripped
                    domainMailTemplate.body shouldBe "Updated Body"
                    domainMailTemplate.mail shouldBe Mail.ZAAK_ALGEMEEN
                    domainMailTemplate.isDefaultMailtemplate shouldBe false
                }
            }
        }

        Given("A REST mail template with complex HTML in subject") {
            val restMailTemplate = createRestMailTemplate(
                subject = "<p>Start</p>Middle<p>End</p>"
            )

            When("convertForUpdate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                Then("HTML paragraph tags should be stripped correctly") {
                    domainMailTemplate.onderwerp shouldBe "StartMiddleEnd"
                }
            }
        }
    }

    Context("Comparison between convertForCreate and convertForUpdate") {
        Given("The same REST mail template") {
            val restMailTemplate = createRestMailTemplate(
                id = 456L,
                mailTemplateName = "Same Template",
                subject = "<p>Same Subject</p>",
                body = "Same Body",
                mail = Mail.SIGNALERING_TAAK_OP_NAAM.name,
                defaultTemplate = true
            )

            When("both convertForCreate and convertForUpdate are called") {
                val createResult = RESTMailtemplateConverter.convertForCreate(restMailTemplate)
                val updateResult = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                Then("both should produce identical domain models except for ID handling") {
                    createResult.id shouldBe 0L
                    updateResult.id shouldBe 0L
                    
                    createResult.mailTemplateNaam shouldBe updateResult.mailTemplateNaam
                    createResult.onderwerp shouldBe updateResult.onderwerp
                    createResult.body shouldBe updateResult.body
                    createResult.mail shouldBe updateResult.mail
                    createResult.isDefaultMailtemplate shouldBe updateResult.isDefaultMailtemplate
                }

                And("all non-ID fields should match the input") {
                    createResult.mailTemplateNaam shouldBe "Same Template"
                    createResult.onderwerp shouldBe "Same Subject"
                    createResult.body shouldBe "Same Body"
                    createResult.mail shouldBe Mail.SIGNALERING_TAAK_OP_NAAM
                    createResult.isDefaultMailtemplate shouldBe true
                }
            }
        }
    }

    Context("ID handling verification") {
        Given("REST mail templates with various ID values") {
            val testCases = listOf(
                0L, 1L, 999L, Long.MAX_VALUE
            )

            testCases.forEach { idValue ->
                val restMailTemplate = createRestMailTemplate(id = idValue)

                When("convertForCreate is called with ID $idValue") {
                    val result = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

                    Then("the result should always have ID = 0 regardless of input ID") {
                        result.id shouldBe 0L
                    }
                }

                When("convertForUpdate is called with ID $idValue") {
                    val result = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                    Then("the result should always have ID = 0 regardless of input ID") {
                        result.id shouldBe 0L
                    }
                }
            }
        }
    }

    Context("Existing convert method behavior (for comparison)") {
        Given("A REST mail template") {
            val restMailTemplate = createRestMailTemplate(
                id = 789L,
                mailTemplateName = "Legacy Template"
            )

            When("the existing convert method is called") {
                val result = RESTMailtemplateConverter.convert(restMailTemplate)

                Then("it should preserve the ID (legacy behavior)") {
                    result.id shouldBe 789L
                    result.mailTemplateNaam shouldBe "Legacy Template"
                }
            }
        }

        Given("A domain mail template") {
            val domainMailTemplate = createMailTemplate(
                id = 321L,
                name = "Domain Template"
            )

            When("the existing convert method is called") {
                val result = RESTMailtemplateConverter.convert(domainMailTemplate)

                Then("it should correctly convert to REST model") {
                    result.id shouldBe 321L
                    result.mailTemplateNaam shouldBe "Domain Template"
                }
            }
        }
    }
})
