/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.mail.converter

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.checkUnnecessaryStub
import io.mockk.every
import io.mockk.mockk
import net.atos.zac.app.mail.model.createRESTMailGegevens
import nl.info.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import nl.info.zac.app.shared.RestVertrouwelijkheidaanduiding
import nl.info.zac.configuration.ConfigurationService

class RESTMailGegevensConverterTest : BehaviorSpec({
    val configurationService = mockk<ConfigurationService>()
    val restMailGegevensConverter = RESTMailGegevensConverter().also { instance ->
        instance.javaClass.getDeclaredField("configurationService").also {
            it.isAccessible = true
            it.set(instance, configurationService)
        }
    }
    val gemeenteNaam = "fakeGemeenteNaam"

    afterEach {
        checkUnnecessaryStub()
    }

    given("a RESTMailGegevens with a vertrouwelijkheidaanduiding") {
        every { configurationService.readGemeenteNaam() } returns gemeenteNaam
        val restMailGegevens = createRESTMailGegevens(
            verzender = "verzender@example.com",
            ontvanger = "ontvanger@example.com",
            vertrouwelijkheidaanduiding = RestVertrouwelijkheidaanduiding.GEHEIM
        ).apply {
            replyTo = "replyto@example.com"
            bijlagen = "fakeAttachmentUuid"
            createDocumentFromMail = true
        }

        `when`("convert is called") {
            val mailGegevens = restMailGegevensConverter.convert(restMailGegevens)

            then("the domain MailGegevens carries the equivalent ZGW confidentiality enum value") {
                mailGegevens.vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.GEHEIM
            }

            And("the remaining fields are mapped correctly") {
                mailGegevens.from.email shouldBe "verzender@example.com"
                mailGegevens.from.name shouldBe gemeenteNaam
                mailGegevens.to.email shouldBe "ontvanger@example.com"
                mailGegevens.replyTo?.email shouldBe "replyto@example.com"
                mailGegevens.replyTo?.name shouldBe gemeenteNaam
                mailGegevens.subject shouldBe "fakeOnderwerp"
                mailGegevens.body shouldBe "fakeBody"
                mailGegevens.attachments shouldBe listOf("fakeAttachmentUuid")
                mailGegevens.isCreateDocumentFromMail shouldBe true
            }
        }
    }

    given("a RESTMailGegevens without a vertrouwelijkheidaanduiding") {
        every { configurationService.readGemeenteNaam() } returns gemeenteNaam
        val restMailGegevens = createRESTMailGegevens(
            vertrouwelijkheidaanduiding = null
        )

        `when`("convert is called") {
            val mailGegevens = restMailGegevensConverter.convert(restMailGegevens)

            then("the domain MailGegevens defaults to Openbaar instead of the ZGW 'empty' placeholder") {
                mailGegevens.vertrouwelijkheidaanduiding shouldBe VertrouwelijkheidaanduidingEnum.OPENBAAR
            }
        }
    }

    given("a RESTMailGegevens without a replyTo address") {
        every { configurationService.readGemeenteNaam() } returns gemeenteNaam
        val restMailGegevens = createRESTMailGegevens()

        `when`("convert is called") {
            val mailGegevens = restMailGegevensConverter.convert(restMailGegevens)

            then("the domain MailGegevens has no replyTo") {
                mailGegevens.replyTo.shouldBeNull()
            }
        }
    }
})
