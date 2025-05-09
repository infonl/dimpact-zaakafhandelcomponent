/*
 * SPDX-FileCopyrightText: 2024 Lifely
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
import net.atos.client.zgw.zrc.ZrcClientService
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.zrc.util.StatusTypeUtil
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.mailtemplates.MailTemplateHelper
import net.atos.zac.mailtemplates.model.createMailGegevens
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakInformatieobject
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createInformatieObjectType
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.authentication.createLoggedInUser
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.mail.model.Bronnen
import nl.info.zac.mail.model.getBronnenFromZaak
import org.flowable.task.api.Task
import java.net.URI
import java.time.ZonedDateTime
import java.util.Properties
import java.util.UUID

class MailServiceTest : BehaviorSpec({
    val configuratieService = mockk<ConfiguratieService>()
    val drcClientService = mockk<DrcClientService>()
    val mailTemplateHelper = mockk<MailTemplateHelper>()
    val zgwApiService = mockk<ZGWApiService>()
    val ztcClientService = mockk<ZtcClientService>()
    val zrcClientService = mockk<ZrcClientService>()
    val zaakVariabelenService = mockk<ZaakVariabelenService>()
    val loggedInUserInstance = mockk<Instance<LoggedInUser>>()

    val mailService = MailService(
        configuratieService,
        zgwApiService,
        ztcClientService,
        zrcClientService,
        zaakVariabelenService,
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
        val bronnen = zaak.getBronnenFromZaak()
        val informatieObjectType = createInformatieObjectType(
            // omschrijving has to be exactly "e-mail"
            omschrijving = "e-mail"
        )
        val user = createLoggedInUser()
        val zaakInformatieobject = createZaakInformatieobject()
        val resolvedSubject = "resolvedSubject"

        every { mailTemplateHelper.resolveVariabelen(mailGegevens.subject) } returns "fakeResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString1", zaak)
        } returns "fakeResolvedString2"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString2", bronnen.document)
        } returns "fakeResolvedString3"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString3", bronnen.taskInfo)
        } returns resolvedSubject
        every { mailTemplateHelper.resolveVariabelen(mailGegevens.body) } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody2", zaak)
        } returns "fakeResolvedBody3"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody3", bronnen.document)
        } returns "fakeResolvedBody4"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody4", bronnen.taskInfo)
        } returns "fakeResolvedBody5"
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
                        content shouldBe "fakeResolvedBody5"
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

        every { mailTemplateHelper.resolveVariabelen(mailGegevens.subject) } returns "fakeResolvedString1"

        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString1", null as Zaak?)
        } returns "fakeResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString1", null as EnkelvoudigInformatieObject?)
        } returns "fakeResolvedString1"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedString1", task)
        } returns resolvedSubject
        every { mailTemplateHelper.resolveVariabelen(mailGegevens.body) } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody2", null as Zaak?)
        } returns "fakeResolvedBody2"
        every {
            mailTemplateHelper.resolveVariabelen("fakeResolvedBody2", null as EnkelvoudigInformatieObject?)
        } returns "fakeResolvedBody2"
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

    Given("a zaak that is not heropend") {
        val zaakUuid = UUID.randomUUID()
        val statusUuid = UUID.randomUUID()
        val zaak = createZaak().apply {
            uuid = zaakUuid
            status = URI(statusUuid.toString())
        }
        val statusType = createStatusType().apply {
            omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_IN_BEHANDELING
        }
        val status = createZaakStatus(
            statusUuid,
            URI(statusUuid.toString()),
            zaak.url,
            statusType.url,
            ZonedDateTime.now()
        )

        every { zrcClientService.readStatus(zaak.status) } returns status
        every { ztcClientService.readStatustype(status.statustype) } returns statusType
        mockkStatic(StatusTypeUtil::class)
        every { StatusTypeUtil.isHeropend(statusType) } returns false
        every { zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true) } just runs

        When("setOntvangstbevestigingVerstuurdIfNotHeropend is called") {
            mailService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)

            Then("ontvangstbevestiging is true") {
                verify(exactly = 1) {
                    zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, true)
                }
            }
        }
    }

    Given("a zaak is heropend") {
        val zaakUuid = UUID.randomUUID()
        val statusUuid = UUID.randomUUID()
        val zaak = createZaak().apply {
            uuid = zaakUuid
            status = URI(statusUuid.toString())
        }
        val statusType = createStatusType().apply {
            omschrijving = ConfiguratieService.STATUSTYPE_OMSCHRIJVING_HEROPEND
        }
        val status = createZaakStatus(
            statusUuid,
            URI(statusUuid.toString()),
            zaak.url,
            statusType.url,
            ZonedDateTime.now()
        )

        every { zrcClientService.readStatus(zaak.status) } returns status
        every { ztcClientService.readStatustype(status.statustype) } returns statusType
        mockkStatic(StatusTypeUtil::class)
        every { StatusTypeUtil.isHeropend(statusType) } returns true

        When("setOntvangstbevestigingVerstuurdIfNotHeropend is called") {
            mailService.setOntvangstbevestigingVerstuurdIfNotHeropend(zaak)

            Then("ontvangstbevestiging is false") {
                verify(exactly = 0) {
                    zaakVariabelenService.setOntvangstbevestigingVerstuurd(zaak.uuid, false)
                }
            }
        }
    }
})
