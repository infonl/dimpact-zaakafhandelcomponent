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
import jakarta.mail.Transport
import jakarta.mail.internet.MimeMultipart
import net.atos.client.zgw.drc.DrcClientService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobjectForCreatesAndUpdates
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mailtemplates.MailTemplateHelper
import nl.info.zac.mailtemplates.model.createMailGegevens
import org.flowable.task.api.Task
import java.net.URI
import java.util.Properties

class MailServiceTest : BehaviorSpec({
    val configuratieService = mockk<ConfiguratieService>()
    val drcClientService = mockk<DrcClientService>()
    val mailTemplateHelper = mockk<MailTemplateHelper>()
    val zgwApiService = mockk<ZGWApiService>()
    val ztcClientService = mockk<ZtcClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    val mailService = MailService(
        configuratieService,
        zgwApiService,
        ztcClientService,
        drcClientService,
        mailTemplateHelper,
        loggedInUserInstance
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    Given("a zaak and e-mail data with 'create document from mail' set to true") {
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
        val user = createLoggedInUser()
        val zaakInformatieobject = createZaakInformatieobjectForCreatesAndUpdates()
        val resolvedSubject = "resolvedSubject"

        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.subject) } returns "fakeResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString1", zaak)
        } returns resolvedSubject
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.body) } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody2", zaak)
        } returns "fakeResolvedBody3"
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every {
            ztcClientService.readInformatieobjecttype(URI("fakeInformatieObjectType1"))
        } returns informatieObjectType
        every { loggedInUserInstance.get() } returns user
        every {
            zgwApiService.createZaakInformatieobjectForZaak(
                zaak, any(), resolvedSubject, resolvedSubject, "geen"
            )
        } returns zaakInformatieobject
        mockkObject(MailService.Companion)
        every { MailService.Companion.mailSession.properties } returns Properties()
        mockkStatic(Transport::class)
        val transportSendRequest = slot<Message>()
        every { Transport.send(capture(transportSendRequest)) } just runs
        every { configuratieService.readBronOrganisatie() } returns "123443210"

        When("the send mail function is invoked") {
            mailService.sendMail(mailGegevens, bronnen)

            Then(
                """
                    an e-mail is sent and a PDF document is created from the e-mail data and
                    is attached to the zaak using the ZGW APIs
                """
            ) {
                verify(exactly = 1) {
                    Transport.send(any())
                    zgwApiService.createZaakInformatieobjectForZaak(
                        zaak,
                        any(),
                        resolvedSubject,
                        resolvedSubject,
                        "geen"
                    )
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
        }
    }

    Given("A task and mail gegevens with an attachment UUID array string consisting of an empty string") {
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
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString1", task)
        } returns resolvedSubject
        every { mailTemplateHelper.resolveGemeenteVariable(mailGegevens.body) } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody2", task)
        } returns resolvedBody

        mockkObject(MailService.Companion)
        every { MailService.Companion.mailSession.properties } returns Properties()

        mockkStatic(Transport::class)
        val transportSendRequest = slot<Message>()
        every { Transport.send(capture(transportSendRequest)) } just runs

        When("the send mail function is invoked") {
            mailService.sendMail(mailGegevens, bronnen)

            Then(
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
