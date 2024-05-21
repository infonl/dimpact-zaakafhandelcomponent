/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.mail

import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.MailjetResponse
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldNotBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import jakarta.enterprise.inject.Instance
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import net.atos.client.zgw.drc.DRCClientService
import net.atos.client.zgw.shared.ZGWApiService
import net.atos.client.zgw.zrc.model.createZaak
import net.atos.client.zgw.zrc.model.createZaakInformatieobject
import net.atos.client.zgw.ztc.ZTCClientService
import net.atos.client.zgw.ztc.model.createInformatieObjectType
import net.atos.client.zgw.ztc.model.createZaakType
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.authentication.createLoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.mail.model.getBronnenFromZaak
import net.atos.zac.mailtemplates.MailTemplateHelper
import net.atos.zac.mailtemplates.model.createMailGegevens
import org.eclipse.microprofile.config.ConfigProvider
import org.json.JSONObject
import java.net.URI
import java.util.Properties

class MailServiceTest : BehaviorSpec({
    val configuratieService = mockk<ConfiguratieService>()
    val drcClientService = mockk<DRCClientService>()
    val mailTemplateHelper = mockk<MailTemplateHelper>()
    val mailJetClient = mockk<MailjetClient>()
    val zgwApiService = mockk<ZGWApiService>()
    val ztcClientService = mockk<ZTCClientService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()
    val mailSession = mockk<Session>()


    val mailService = MailService(
        configuratieService,
        zgwApiService,
        ztcClientService,
        drcClientService,
        mailTemplateHelper,
        loggedInUserInstance,
        mailSession
    )

    beforeEach {
        checkUnnecessaryStub()
    }

    beforeSpec {
        clearAllMocks()
    }

    Given("a zaak and e-mail data with 'create document from mail' set to true") {
        When("sendMail is invoked") {
            Then(
                "a mail is sent using MailJet and a PDF document is created from the e-mail data and " +
                    "is attached to the zaak using the ZGW APIs"
            ) {
                val zaak = createZaak()
                val zaakType = createZaakType(
                    informatieObjectTypen = setOf(
                        URI("dummyInformatieObjectType1")
                    )
                )
                val mailGegevens = createMailGegevens(
                    createDocumentFromMail = true
                )
                val bronnen = zaak.getBronnenFromZaak()
                val mailjetResponse = MailjetResponse(200, "{dummyAttribute: dummyValue}")
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
                every {
                    mailTemplateHelper.resolveVariabelen(mailGegevens.body, zaak)
                } returns "dummyResolvedBody1"
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

                every { mailSession.properties } returns Properties()
                every { mailSession.getProperty("mail.from") } returns "sender@example.com"
                every { mailSession.getProperty("mail.mime.address.strict") } returns "no"
                every { mailSession.getProperty("mail.mime.allowutf8") } returns "yes"

                mockkStatic(Transport::class)
                val transportSendRequest = slot<Message>()
                every { Transport.send(capture(transportSendRequest)) } just runs

                mailService.sendMail(mailGegevens, bronnen)

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
                    with ((content as MimeMultipart).getBodyPart(0)) {
                        contentType shouldBe "text/plain"
                        content shouldBe "dummyResolvedBody5"
                    }
                }
            }
        }
    }
})
