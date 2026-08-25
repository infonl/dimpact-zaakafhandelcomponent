/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import net.atos.zac.app.admin.model.createRestMailTemplate
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.createMailTemplate

class RestMailtemplateConverterTest : BehaviorSpec({

    context("toMailTemplateWithoutID extension function") {
        given("A REST mail template with an ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 999L,
                mailTemplateName = "Test Template",
                subject = "<p>Test Subject</p>",
                body = "Test Body",
                mail = Mail.ZAAK_ALGEMEEN,
                defaultTemplate = true
            )

            `when`("toMailTemplateWithoutID is called") {
                val domainMailTemplate = restMailTemplate.toMailTemplateWithoutID()

                then("the domain model should not have an ID set") {
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

        given("A REST mail template with HTML paragraph tags in subject") {
            val restMailTemplate = createRestMailTemplate(
                subject = "<p>Complex</p><p>Subject</p><p>With</p><p>Tags</p>"
            )

            `when`("toMailTemplateWithoutID is called") {
                val domainMailTemplate = restMailTemplate.toMailTemplateWithoutID()

                then("HTML paragraph tags should be stripped from subject") {
                    domainMailTemplate.onderwerp shouldBe "ComplexSubjectWithTags"
                }
            }
        }

        given("A REST mail template with different mail types") {
            listOf(
                Mail.ZAAK_ALGEMEEN,
                Mail.SIGNALERING_TAAK_OP_NAAM,
            ).forEach { mailType ->
                val restMailTemplate = createRestMailTemplate(mail = mailType)

                `when`("toMailTemplateWithoutID is called with mail type ${mailType.name}") {
                    val domainMailTemplate = restMailTemplate.toMailTemplateWithoutID()

                    then("the mail enum should be correctly converted") {
                        domainMailTemplate.mail shouldBe mailType
                    }
                }
            }
        }

        given("REST mail templates with various ID values") {
            val testCases = listOf(
                0L,
                1L,
                999L,
                Long.MAX_VALUE
            )

            testCases.forEach { idValue ->
                val restMailTemplate = createRestMailTemplate(id = idValue)

                `when`("toMailTemplateWithoutID is called with ID $idValue") {
                    val result = restMailTemplate.toMailTemplateWithoutID()

                    then("the result should always have ID = 0 regardless of input ID") {
                        result.id shouldBe 0L
                    }
                }
            }
        }

        given("A REST mail template with a template name containing whitespace") {
            val restMailTemplate = createRestMailTemplate().apply { mailTemplateNaam = "  Test Template  " }

            `when`("toMailTemplateWithoutID is called") {
                val result = restMailTemplate.toMailTemplateWithoutID()

                then("it should trim whitespace from the template name") {
                    result.mailTemplateNaam shouldBe "Test Template"
                }
            }
        }
    }

    context("toMailTemplate and toRestMailtemplate extension functions") {
        given("A REST mail template with ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 789L,
                mailTemplateName = "Legacy Template"
            )

            `when`("toMailTemplate is called") {
                val result = restMailTemplate.toMailTemplate()

                then("it should preserve the ID") {
                    result.id shouldBe 789L
                    result.mailTemplateNaam shouldBe "Legacy Template"
                }
            }
        }

        given("A domain mail template") {
            val domainMailTemplate = createMailTemplate(
                id = 321L,
                name = "Domain Template"
            )

            `when`("toRestMailtemplate is called") {
                val result = domainMailTemplate.toRestMailtemplate()

                then("it should correctly convert to REST model") {
                    result.id shouldBe 321L
                    result.mailTemplateNaam shouldBe "Domain Template"
                }
            }
        }
    }
})
