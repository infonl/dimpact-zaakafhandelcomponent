/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.mailtemplates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import nl.info.client.brp.BrpClientService
import nl.info.client.kvk.KvkClientService
import nl.info.client.zgw.model.createZaak
import nl.info.client.zgw.model.createZaakStatus
import nl.info.client.zgw.shared.ZGWApiService
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.client.zgw.ztc.model.createStatusType
import nl.info.client.zgw.ztc.model.createZaakType
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.identity.IdentityService
import java.net.URI
import java.time.LocalDate

class MailTemplateHelperTest : BehaviorSpec({
    val brpClientService = mockk<BrpClientService>()
    val configuratieService = mockk<ConfiguratieService>()
    val identityService = mockk<IdentityService>()
    val kvkClientService = mockk<KvkClientService>()
    val zgwApiService = mockk<ZGWApiService>()
    val zrcClientService = mockk<ZrcClientService>()
    val ztcClientService = mockk<ZtcClientService>()
    val mailTemplateHelper = MailTemplateHelper(
        brpClientService,
        configuratieService,
        identityService,
        kvkClientService,
        zgwApiService,
        zrcClientService,
        ztcClientService
    )

    Given("A zaak") {
        val zaakType = createZaakType()
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            status = URI("https://example.com/fakeStatus"),
            startDate = LocalDate.of(2021, 10, 12)
        )
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType(omschrijving = "fakeStatusTypeDescription")
        val zaakTonenURL = URI("https://example.com/fakeURL")
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
        every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType

        When("I call the getMailTemplate method") {
            val resolvedText = mailTemplateHelper.resolveVariabelen(
                "fakeText, {ZAAK_NUMMER}, {ZAAK_URL}, {ZAAK_TYPE}, {ZAAK_STATUS}, {ZAAK_STARTDATUM}",
                zaak
            )

            Then("I should get the correct mail template") {
                resolvedText shouldBe "fakeText, ${zaak.identificatie}, $zaakTonenURL, ${zaakType.omschrijving}, " +
                    "${statusType.omschrijving}, 12-10-2021"
            }
        }
    }
})
