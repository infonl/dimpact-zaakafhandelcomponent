/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mail

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Transport
import jakarta.mail.internet.MimeMultipart
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.shared.ZgwApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateHelper
import nl.info.zac.mailtemplates.model.MailTemplateVariables
import nl.info.zac.mailtemplates.model.createMailGegevens
import org.flowable.task.api.Task
import java.net.URI
import java.util.Properties

class MailServiceTest : BehaviorSpec({
    val configurationService = mockk<ConfigurationService>()
    val drcClientService = mockk<DrcClientService>()
    val mailTemplateHelper = mockk<MailTemplateHelper>()
    val zgwApiService = mockk<ZgwApiService>()
    val ztcClientService = mockk<ZtcClientService>()
    val loggedInUserName = "fakeLoggedInUserName"
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    val mailService = MailService(
        configurationService,
        zgwApiService,
        ztcClientService,
        drcClientService,
        mailTemplateHelper,
        loggedInUserInstance
    )

    afterEach {
        checkUnnecessaryStub()
    }

    given("a zaak and e-mail data with 'create document from mail' set to true") {
        val zaak = createZaak()
        val zaakType = createZaakType(
            informatieObjectTypen = listOf(
                URI("fakeInformatieObjectType1")
            )
        )
        val mailGegevens = createMailGegevens(
            createDocumentFromMail = true
        )
        val bronnen = Bronnen.Builder().add(zaak).build()
        val informatieObjectType = createInformatieObjectType(
            // omschrijving has to be exactly "e-mail"
            omschrijving = "e-mail"
        )
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
        val resolvedSubject = "resolvedSubject"

        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.subject) } returns "fakeResolvedString1"
        every {
            mailTemplateHelper.resolveZaakVariables(
                "fakeResolvedString1",
                zaak,
                loggedInUserName
            )
        } returns resolvedSubject
        every { mailTemplateHelper.resolveZaakdataVariables(resolvedSubject, emptyMap()) } returns resolvedSubject
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.body) } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveZaakVariables(
                "fakeResolvedBody2",
                zaak,
                loggedInUserName
            )
        } returns "fakeResolvedBody3"
        every {
            mailTemplateHelper.resolveZaakdataVariables(
                "fakeResolvedBody3",
                emptyMap()
            )
        } returns "fakeResolvedBody3"
        every { loggedInUserInstance.get() } returns createLoggedInUser(id = loggedInUserName)
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every { ztcClientService.readInformatieobjecttype(URI("fakeInformatieObjectType1")) } returns informatieObjectType
        every {
            zgwApiService.createZaakInformatieobjectForZaak(
                zaak, any(), resolvedSubject, resolvedSubject, "geen"
            )
        } returns zaakInformatieobject
        mockkObject(MailService.Companion)
        every { MailService.mailSession.properties } returns Properties()
        mockkStatic(Transport::class)
        val transportSendRequest = slot<Message>()
        every { Transport.send(capture(transportSendRequest)) } just runs
        every { configurationService.readBronOrganisatie() } returns "123443210"

        `when`("the send mail function is invoked") {
            val body = mailService.sendMail(mailGegevens, bronnen)

            then("an e-mail is sent") {
                body shouldBe "fakeResolvedBody3"
                verify(exactly = 1) {
                    Transport.send(any())
                }
                with(transportSendRequest.captured) {
                    subject shouldBe resolvedSubject
                    getHeader("Reply-To") shouldBe null
                    with((content as MimeMultipart).getBodyPart(0).dataHandler) {
                        contentType shouldBe "text/html; charset=UTF-8"
                        content shouldBe "fakeResolvedBody3"
                    }
                }
            }

            And("PDF document is created from the e-mail data and is attached to the zaak using the ZGW APIs") {
                verify(exactly = 1) {
                    zgwApiService.createZaakInformatieobjectForZaak(
                        zaak,
                        any(),
                        resolvedSubject,
                        resolvedSubject,
                        "geen"
                    )
                }
            }
        }
    }

    given("the mail transport cannot send the mail") {
        val zaak = createZaak()
        val mailGegevens = createMailGegevens(
            createDocumentFromMail = true
        )
        val bronnen = Bronnen.Builder().add(zaak).build()
        val resolvedSubject = "resolvedSubject"

        every { loggedInUserInstance.get() } returns createLoggedInUser(id = loggedInUserName)
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.subject) } returns "fakeResolvedString1"
        every {
            mailTemplateHelper.resolveZaakVariables(
                "fakeResolvedString1",
                zaak,
                loggedInUserName
            )
        } returns resolvedSubject
        every { mailTemplateHelper.resolveZaakdataVariables(resolvedSubject, emptyMap()) } returns resolvedSubject
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.body) } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveZaakVariables(
                "fakeResolvedBody2",
                zaak,
                loggedInUserName
            )
        } returns "fakeResolvedBody3"
        every {
            mailTemplateHelper.resolveZaakdataVariables(
                "fakeResolvedBody3",
                emptyMap()
            )
        } returns "fakeResolvedBody3"
        mockkObject(MailService.Companion)
        every { MailService.mailSession.properties } returns Properties()
        mockkStatic(Transport::class)
        every { Transport.send(any<Message>()) } throws MessagingException()

        `when`("the send mail function is invoked") {
            val body = mailService.sendMail(mailGegevens, bronnen)

            then("no body is returned") {
                body shouldBe null
            }
        }
    }

    given("a zaak with no zaakdata and an e-mail template body containing a zaakdata variable") {
        val zaak = createZaak()
        val bodyWithZaakdataVariable = "${MailTemplateVariables.ZAAKDATA_VARIABLE_PREFIX}name}"
        val mailGegevens = createMailGegevens(
            body = bodyWithZaakdataVariable,
            createDocumentFromMail = false
        )
        val bronnen = Bronnen.Builder().add(zaak).build()

        every { loggedInUserInstance.get() } returns createLoggedInUser(id = loggedInUserName)
        every { mailTemplateHelper.readZaakdata(zaak) } returns emptyMap()
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.subject) } returns mailGegevens.subject
        every {
            mailTemplateHelper.resolveZaakVariables(mailGegevens.subject, zaak, loggedInUserName)
        } returns mailGegevens.subject
        every {
            mailTemplateHelper.resolveZaakdataVariables(mailGegevens.subject, emptyMap())
        } returns mailGegevens.subject
        every { mailTemplateHelper.resolveGemeenteVariable(bodyWithZaakdataVariable) } returns bodyWithZaakdataVariable
        every {
            mailTemplateHelper.resolveZaakVariables(
                bodyWithZaakdataVariable,
                zaak,
                loggedInUserName
            )
        } returns bodyWithZaakdataVariable
        every { mailTemplateHelper.resolveZaakdataVariables(bodyWithZaakdataVariable, emptyMap()) } returns "Onbekend"

        mockkObject(MailService.Companion)
        every { MailService.mailSession.properties } returns Properties()
        mockkStatic(Transport::class)
        every { Transport.send(any<Message>()) } just runs

        `when`("the send mail function is invoked") {
            val body = mailService.sendMail(mailGegevens, bronnen)

            then("the zaakdata variable in the body is replaced with 'Onbekend'") {
                body shouldBe "Onbekend"
                verify(exactly = 1) {
                    mailTemplateHelper.resolveZaakdataVariables(bodyWithZaakdataVariable, emptyMap())
                }
            }
        }
    }

    given("A task and mail gegevens with an attachment UUID array string consisting of an empty string") {
        val task = mockk<Task>()
        val mailGegevens = createMailGegevens(
            createDocumentFromMail = true,
            // test if we can correctly handle an attachment UUID array string consisting of an empty string
            attachments = ""
        )
        val bronnen = Bronnen.Builder().add(task).build()
        val resolvedSubject = "resolvedSubject"
        val resolvedBody = "resolvedBody"

        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.subject) } returns "fakeResolvedString1"
        every { mailTemplateHelper.resolveTaskVariables("fakeResolvedString1", task) } returns resolvedSubject
        every { mailTemplateHelper.resolveZaakdataVariables(resolvedSubject, emptyMap()) } returns resolvedSubject
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.body) } returns "fakeResolvedBody2"
        every { mailTemplateHelper.resolveTaskVariables("fakeResolvedBody2", task) } returns resolvedBody
        every { mailTemplateHelper.resolveZaakdataVariables(resolvedBody, emptyMap()) } returns resolvedBody

        mockkObject(MailService.Companion)
        every { MailService.mailSession.properties } returns Properties()

        mockkStatic(Transport::class)
        val transportSendRequest = slot<Message>()
        every { Transport.send(capture(transportSendRequest)) } just runs

        `when`("the send mail function is invoked") {
            mailService.sendMail(mailGegevens, bronnen)

            then(
                """
                    an e-mail is sent with the task data and no PDF document is created
                """
            ) {
                verify(exactly = 1) {
                    Transport.send(any())
                }
                with(transportSendRequest.captured) {
                    subject shouldBe resolvedSubject
                    with((content as MimeMultipart).getBodyPart(0).dataHandler) {
                        contentType shouldBe "text/html; charset=UTF-8"
                        content shouldBe resolvedBody
                    }
                }
            }
        }
    }
})
