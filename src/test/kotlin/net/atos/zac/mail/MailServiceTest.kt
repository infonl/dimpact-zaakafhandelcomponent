/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail

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
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.mail.model.Bronnen
import net.atos.zac.mail.model.getBronnenFromZaak
import net.atos.zac.mailtemplates.MailTemplateHelper
import net.atos.zac.mailtemplates.model.createMailGegevens
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobject
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
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
                URI("dummyInformatieObjectType1")
            )
        )
        val mailGegevens = createMailGegevens(
            createDocumentFromMail = true
        )
        val bronnen = zaak.getBronnenFromZaak()
        val informatieObjectType = createInformatieObjectType(
            // omschrijving has to be exactly "e-mail"
            omschrijving = "e-mail"
        )
        val user = createLoggedInUser()
        val zaakInformatieobject = createZaakInformatieobject()
        val resolvedSubject = "resolvedSubject"

        every { mailTemplateHelper.resolveVariabelen(mailGegevens.subject) } returns "dummyResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedString1", zaak)
        } returns "dummyResolvedString2"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedString2", bronnen.document)
        } returns "dummyResolvedString3"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedString3", bronnen.taskInfo)
        } returns resolvedSubject
        every { mailTemplateHelper.resolveVariabelen(mailGegevens.body) } returns "dummyResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedBody2", zaak)
        } returns "dummyResolvedBody3"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedBody3", bronnen.document)
        } returns "dummyResolvedBody4"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedBody4", bronnen.taskInfo)
        } returns "dummyResolvedBody5"
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns zaakType
        every {
            ztcClientService.readInformatieobjecttype(URI("dummyInformatieObjectType1"))
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
                        content shouldBe "dummyResolvedBody5"
                    }
                }
            }
        }
    }

    Given("a task") {
        val task = mockk<Task>()
        val mailGegevens = createMailGegevens(
            createDocumentFromMail = true
        )
        val bronnen = Bronnen.Builder().add(task).build()
        val resolvedSubject = "resolvedSubject"
        val resolvedBody = "resolvedBody"

        every { mailTemplateHelper.resolveVariabelen(mailGegevens.subject) } returns "dummyResolvedString1"

        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedString1", null as Zaak?)
        } returns "dummyResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedString1", null as EnkelvoudigInformatieObject?)
        } returns "dummyResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedString1", task)
        } returns resolvedSubject
        every { mailTemplateHelper.resolveVariabelen(mailGegevens.body) } returns "dummyResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedBody2", null as Zaak?)
        } returns "dummyResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedBody2", null as EnkelvoudigInformatieObject?)
        } returns "dummyResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("dummyResolvedBody2", task)
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
