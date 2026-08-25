/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import jakarta.validation.Validation
import net.atos.zac.app.admin.model.createRestMailTemplate
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.createMailTemplate

class RestMailtemplateTest : BehaviorSpec({
    val validator = Validation.buildDefaultValidatorFactory().validator

    context("Validation of RestMailtemplate") {
        given("a mail template with all required fields present") {
            val restMailTemplate = createRestMailTemplate()

            `when`("validating the mail template") {
                val violations = validator.validate(restMailTemplate)

                then("there should be no constraint violations") {
                    violations.isEmpty() shouldBe true
                }
            }
        }

        given("a mail template with a blank name") {
            val restMailTemplate = createRestMailTemplate(mailTemplateName = "")

            `when`("validating the mail template") {
                val violations = validator.validate(restMailTemplate)

                then("there should be one constraint violation on the name") {
                    violations shouldHaveSize 1
                    violations.first().propertyPath.toString() shouldBe "mailTemplateNaam"
                }
            }
        }
    }

    context("toMailTemplate extension function") {
        given("A REST mail template without an ID") {
            val restMailTemplate = createRestMailTemplate(
                mailTemplateName = "Test Template",
                subject = "<p>Test Subject</p>",
                body = "Test Body",
                mail = Mail.ZAAK_ALGEMEEN,
                defaultTemplate = true
            ).apply { id = null }

            `when`("toMailTemplate is called") {
                val domainMailTemplate = restMailTemplate.toMailTemplate()

                then("the domain model should keep the default ID of 0") {
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

        given("A REST mail template with an ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 789L,
                mailTemplateName = "Legacy Template"
            )

            `when`("toMailTemplate is called") {
                val result = restMailTemplate.toMailTemplate()

                then("the ID should be preserved") {
                    result.id shouldBe 789L
                    result.mailTemplateNaam shouldBe "Legacy Template"
                }
            }
        }

        given("A REST mail template with HTML paragraph tags in subject") {
            val restMailTemplate = createRestMailTemplate(
                subject = "<p>Complex</p><p>Subject</p><p>With</p><p>Tags</p>"
            )

            `when`("toMailTemplate is called") {
                val domainMailTemplate = restMailTemplate.toMailTemplate()

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

                `when`("toMailTemplate is called with mail type ${mailType.name}") {
                    val domainMailTemplate = restMailTemplate.toMailTemplate()

                    then("the mail enum should be correctly converted") {
                        domainMailTemplate.mail shouldBe mailType
                    }
                }
            }
        }

        given("A REST mail template with a template name containing whitespace") {
            val restMailTemplate = createRestMailTemplate().apply { mailTemplateNaam = "  Test Template  " }

            `when`("toMailTemplate is called") {
                val result = restMailTemplate.toMailTemplate()

                then("it should trim whitespace from the template name") {
                    result.mailTemplateNaam shouldBe "Test Template"
                }
            }
        }
    }

    context("toRestMailtemplate extension function") {
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
