/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.signalering

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import net.atos.zac.signalering.model.SignaleringDetail
import net.atos.zac.signalering.model.SignaleringType
import nl.info.zac.identity.IdentityService
import nl.info.zac.identity.model.Group
import nl.info.zac.identity.model.User
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import nl.info.zac.mailtemplates.model.MailTemplate
import nl.info.zac.signalering.model.createSignalering
import nl.info.zac.signalering.model.createSignaleringType

class SignaleringMailHelperTest : BehaviorSpec({
    val identityService = mockk<IdentityService>()
    val mailTemplateService = mockk<MailTemplateService>()
    val signaleringMailHelper = SignaleringMailHelper(identityService, mailTemplateService)

    afterEach { checkUnnecessaryStub() }

    context("getTargetMail for GROUP target") {
        given("A signalering targeting a group that has an email address") {
            val fakeGroup = Group(name = "fakeGroupName", description = "fakeGroupDescription", email = "fake@group.nl")
            val signalering = createSignalering(
                type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM),
                targetGroup = fakeGroup
            )
            every { identityService.readGroup(signalering.target) } returns fakeGroup

            `when`("getTargetMail is called") {
                val result = signaleringMailHelper.getTargetMail(signalering)

                then("a SignaleringTarget.Mail with the group description and email is returned") {
                    result?.naam shouldBe "fakeGroupDescription"
                    result?.emailadres shouldBe "fake@group.nl"
                }
            }
        }

        given("A signalering targeting a group that has no email address") {
            val fakeGroup = Group(name = "fakeGroupName", description = "fakeGroupDescription", email = null)
            val signalering = createSignalering(
                type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM),
                targetGroup = fakeGroup
            )
            every { identityService.readGroup(signalering.target) } returns fakeGroup

            `when`("getTargetMail is called") {
                val result = signaleringMailHelper.getTargetMail(signalering)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("getTargetMail for USER target") {
        given("A signalering targeting a user that has an email address") {
            val fakeUser = User(
                id = "fakeUserId",
                firstName = "fakeFirstName",
                lastName = "fakeLastName",
                email = "fake@user.nl"
            )
            val signalering = createSignalering(
                type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM),
                targetUser = fakeUser
            )
            every { identityService.readUser(signalering.target) } returns fakeUser

            `when`("getTargetMail is called") {
                val result = signaleringMailHelper.getTargetMail(signalering)

                then("a SignaleringTarget.Mail with the user full name and email is returned") {
                    result?.emailadres shouldBe "fake@user.nl"
                    result?.naam shouldBe "fakeFirstName fakeLastName"
                }
            }
        }

        given("A signalering targeting a user that has no email address") {
            val fakeUser = User(id = "fakeUserId", firstName = "fakeFirstName", lastName = "fakeLastName", email = null)
            val signalering = createSignalering(
                type = createSignaleringType(type = SignaleringType.Type.ZAAK_OP_NAAM),
                targetUser = fakeUser
            )
            every { identityService.readUser(signalering.target) } returns fakeUser

            `when`("getTargetMail is called") {
                val result = signaleringMailHelper.getTargetMail(signalering)

                then("null is returned") {
                    result shouldBe null
                }
            }
        }
    }

    context("getMailTemplate mapping") {
        val fakeMailTemplate = mockk<MailTemplate>()

        listOf(
            SignaleringType.Type.TAAK_OP_NAAM to Mail.SIGNALERING_TAAK_OP_NAAM,
            SignaleringType.Type.TAAK_VERLOPEN to Mail.SIGNALERING_TAAK_VERLOPEN,
            SignaleringType.Type.ZAAK_DOCUMENT_TOEGEVOEGD to Mail.SIGNALERING_ZAAK_DOCUMENT_TOEGEVOEGD,
            SignaleringType.Type.ZAAK_OP_NAAM to Mail.SIGNALERING_ZAAK_OP_NAAM,
        ).forEach { (signaleringType, expectedMail) ->
            given("A signalering of type $signaleringType") {
                val signalering = createSignalering(type = createSignaleringType(type = signaleringType))
                every { mailTemplateService.readMailtemplate(expectedMail) } returns fakeMailTemplate

                `when`("getMailTemplate is called") {
                    val result = signaleringMailHelper.getMailTemplate(signalering)

                    then("readMailtemplate is called with $expectedMail") {
                        verify { mailTemplateService.readMailtemplate(expectedMail) }
                        result shouldBe fakeMailTemplate
                    }
                }
            }
        }

        given("A ZAAK_VERLOPEND signalering with STREEFDATUM detail") {
            val signalering = createSignalering(
                type = createSignaleringType(type = SignaleringType.Type.ZAAK_VERLOPEND)
            ).apply { setDetailFromSignaleringDetail(SignaleringDetail.STREEFDATUM) }
            every { mailTemplateService.readMailtemplate(Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM) } returns fakeMailTemplate

            `when`("getMailTemplate is called") {
                val result = signaleringMailHelper.getMailTemplate(signalering)

                then("readMailtemplate is called with SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM") {
                    verify { mailTemplateService.readMailtemplate(Mail.SIGNALERING_ZAAK_VERLOPEND_STREEFDATUM) }
                    result shouldBe fakeMailTemplate
                }
            }
        }

        given("A ZAAK_VERLOPEND signalering with FATALE_DATUM detail") {
            val signalering = createSignalering(
                type = createSignaleringType(type = SignaleringType.Type.ZAAK_VERLOPEND)
            ).apply { setDetailFromSignaleringDetail(SignaleringDetail.FATALE_DATUM) }
            every { mailTemplateService.readMailtemplate(Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM) } returns fakeMailTemplate

            `when`("getMailTemplate is called") {
                val result = signaleringMailHelper.getMailTemplate(signalering)

                then("readMailtemplate is called with SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM") {
                    verify { mailTemplateService.readMailtemplate(Mail.SIGNALERING_ZAAK_VERLOPEND_FATALE_DATUM) }
                    result shouldBe fakeMailTemplate
                }
            }
        }
    }
})
