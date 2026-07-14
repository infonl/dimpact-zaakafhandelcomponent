/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.admin.converter

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import net.atos.zac.app.admin.model.createRestMailTemplate
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.createMailTemplate

class RESTMailtemplateConverterTest : BehaviorSpec({

    context("convertForCreate method") {
        given("A REST mail template with an ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 999L,
                mailTemplateName = "Test Template",
                subject = "<p>Test Subject</p>",
                body = "Test Body",
                mail = Mail.ZAAK_ALGEMEEN,
                defaultTemplate = true
            )

            `when`("convertForCreate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

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

            `when`("convertForCreate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

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

                `when`("convertForCreate is called with mail type ${mailType.name}") {
                    val domainMailTemplate = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

                    then("the mail enum should be correctly converted") {
                        domainMailTemplate.mail shouldBe mailType
                    }
                }
            }
        }
    }

    context("convertForUpdate method") {
        given("A REST mail template with an ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 123L,
                mailTemplateName = "Updated Template",
                subject = "<p>Updated Subject</p>",
                body = "Updated Body",
                mail = Mail.ZAAK_ALGEMEEN,
                defaultTemplate = false
            )

            `when`("convertForUpdate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                then("the domain model should not have an ID set (will be set by service layer)") {
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

        given("A REST mail template with complex HTML in subject") {
            val restMailTemplate = createRestMailTemplate(
                subject = "<p>Start</p>Middle<p>End</p>"
            )

            `when`("convertForUpdate is called") {
                val domainMailTemplate = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                then("HTML paragraph tags should be stripped correctly") {
                    domainMailTemplate.onderwerp shouldBe "StartMiddleEnd"
                }
            }
        }
    }

    context("Comparison between convertForCreate and convertForUpdate") {
        given("The same REST mail template") {
            val restMailTemplate = createRestMailTemplate(
                id = 456L,
                mailTemplateName = "Same Template",
                subject = "<p>Same Subject</p>",
                body = "Same Body",
                mail = Mail.SIGNALERING_TAAK_OP_NAAM,
                defaultTemplate = true
            )

            `when`("both convertForCreate and convertForUpdate are called") {
                val createResult = RESTMailtemplateConverter.convertForCreate(restMailTemplate)
                val updateResult = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                then("both should produce identical domain models except for ID handling") {
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

    context("ID handling verification") {
        given("REST mail templates with various ID values") {
            val testCases = listOf(
                0L,
                1L,
                999L,
                Long.MAX_VALUE
            )

            testCases.forEach { idValue ->
                val restMailTemplate = createRestMailTemplate(id = idValue)

                `when`("convertForCreate is called with ID $idValue") {
                    val result = RESTMailtemplateConverter.convertForCreate(restMailTemplate)

                    then("the result should always have ID = 0 regardless of input ID") {
                        result.id shouldBe 0L
                    }
                }

                `when`("convertForUpdate is called with ID $idValue") {
                    val result = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)

                    then("the result should always have ID = 0 regardless of input ID") {
                        result.id shouldBe 0L
                    }
                }
            }
        }
    }

    context("Existing convert method behavior (for comparison)") {
        given("A REST mail template with ID") {
            val restMailTemplate = createRestMailTemplate(
                id = 789L,
                mailTemplateName = "Legacy Template"
            )

            `when`("the existing convert method is called") {
                val result = RESTMailtemplateConverter.convert(restMailTemplate)

                then("it should preserve the ID (legacy behavior)") {
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

            `when`("the existing convert method is called") {
                val result = RESTMailtemplateConverter.convert(domainMailTemplate)

                then("it should correctly convert to REST model") {
                    result.id shouldBe 321L
                    result.mailTemplateNaam shouldBe "Domain Template"
                }
            }
        }
    }

    given("Error handling for convertForCreate") {
        `when`("convertForCreate is called with null input") {
            then("it should throw IllegalArgumentException") {
                val exception = shouldThrow<IllegalArgumentException> {
                    RESTMailtemplateConverter.convertForCreate(null)
                }
                exception.message shouldContain "RESTMailtemplate cannot be null"
            }
        }
    }

    given("Error handling for convertForUpdate") {
        `when`("convertForUpdate is called with null input") {
            then("it should throw IllegalArgumentException") {
                val exception = shouldThrow<IllegalArgumentException> {
                    RESTMailtemplateConverter.convertForUpdate(null)
                }
                exception.message shouldContain "RESTMailtemplate cannot be null"
            }
        }
    }

    given("Whitespace handling for template names") {
        `when`("convertForCreate is called with template name containing whitespace") {
            val restMailTemplate = createRestMailTemplate().apply { mailTemplateNaam = "  Test Template  " }

            then("it should trim whitespace and convert successfully") {
                val result = RESTMailtemplateConverter.convertForCreate(restMailTemplate)
                result.mailTemplateNaam shouldBe "Test Template"
            }
        }

        `when`("convertForUpdate is called with template name containing whitespace") {
            val restMailTemplate = createRestMailTemplate().apply { mailTemplateNaam = "  Updated Template  " }

            then("it should trim whitespace and convert successfully") {
                val result = RESTMailtemplateConverter.convertForUpdate(restMailTemplate)
                result.mailTemplateNaam shouldBe "Updated Template"
            }
        }
    }
})
