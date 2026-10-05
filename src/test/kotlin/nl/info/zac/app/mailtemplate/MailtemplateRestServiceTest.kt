/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.mailtemplate

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.zac.admin.ZaaktypeConfigurationService
import nl.info.zac.admin.model.createMailTemplate
import nl.info.zac.admin.model.createMailtemplateKoppelingen
import nl.info.zac.admin.model.createZaaktypeCmmnConfiguration
import nl.info.zac.mailtemplates.MailTemplateService
import nl.info.zac.mailtemplates.model.Mail
import java.net.URI
import java.util.UUID

class MailtemplateRestServiceTest : BehaviorSpec({
    val mailTemplateService = mockk<MailTemplateService>()
    val zaaktypeConfigurationService = mockk<ZaaktypeConfigurationService>()
    val zrcClientService = mockk<ZrcClientService>()
    val mailtemplateRestService = MailtemplateRestService(
        mailTemplateService = mailTemplateService,
        zaaktypeConfigurationService = zaaktypeConfigurationService,
        zrcClientService = zrcClientService
    )
    val zaaktypeUuid = UUID.randomUUID()
    val zaak = createZaak(zaaktypeUri = URI("https://example.com/zaaktypen/$zaaktypeUuid"))

    afterEach { checkUnnecessaryStub() }

    context("findMailtemplate") {
        given("a zaak whose zaaktype configuration links a mail template for the requested mail") {
            val linkedMailTemplate = createMailTemplate(mail = Mail.ZAAK_ALGEMEEN).apply {
                id = 5678L
                mailTemplateNaam = "fakeLinkedMailTemplate"
            }
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration().apply {
                setMailtemplateKoppelingen(
                    setOf(createMailtemplateKoppelingen(zaaktypeConfiguration = this, mailTemplate = linkedMailTemplate))
                )
            }
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUuid)
            } returns zaaktypeCmmnConfiguration

            `when`("the mail template for that mail is requested") {
                val restMailtemplate = mailtemplateRestService.findMailtemplate(Mail.ZAAK_ALGEMEEN, zaak.uuid)

                then("the linked mail template is returned") {
                    restMailtemplate?.mailTemplateNaam shouldBe "fakeLinkedMailTemplate"
                }
            }
        }

        given("a zaak whose zaaktype configuration links no mail template for the requested mail") {
            val defaultMailTemplate = createMailTemplate(mail = Mail.ZAAK_ALGEMEEN).apply {
                mailTemplateNaam = "fakeDefaultMailTemplate"
            }
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration().apply {
                setMailtemplateKoppelingen(emptySet())
            }
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUuid)
            } returns zaaktypeCmmnConfiguration
            every { mailTemplateService.findDefaultMailtemplate(Mail.ZAAK_ALGEMEEN) } returns defaultMailTemplate

            `when`("the mail template for that mail is requested") {
                val restMailtemplate = mailtemplateRestService.findMailtemplate(Mail.ZAAK_ALGEMEEN, zaak.uuid)

                then("the default mail template of that mail is returned") {
                    restMailtemplate?.mailTemplateNaam shouldBe "fakeDefaultMailTemplate"
                }
            }
        }

        given("a zaak without a linked mail template and a mail without a default mail template") {
            val zaaktypeCmmnConfiguration = createZaaktypeCmmnConfiguration().apply {
                setMailtemplateKoppelingen(emptySet())
            }
            every { zrcClientService.readZaak(zaak.uuid) } returns zaak
            every {
                zaaktypeConfigurationService.readZaaktypeConfiguration(zaaktypeUuid)
            } returns zaaktypeCmmnConfiguration
            every { mailTemplateService.findDefaultMailtemplate(Mail.ZAAK_ALGEMEEN) } returns null

            `when`("the mail template for that mail is requested") {
                val restMailtemplate = mailtemplateRestService.findMailtemplate(Mail.ZAAK_ALGEMEEN, zaak.uuid)

                then("no mail template is returned") {
                    restMailtemplate.shouldBeNull()
                }
            }
        }
    }
})
