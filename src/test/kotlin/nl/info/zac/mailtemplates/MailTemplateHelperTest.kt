/*
 * SPDX-FileCopyrightText: 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.mailtemplates

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import nl.info.client.brp.BrpClientService
import nl.info.client.kvk.KvkClientService
import nl.info.client.kvk.model.createResultaatItem
import nl.info.client.zgw.model.createNietNatuurlijkPersoonIdentificatie
import nl.info.client.zgw.model.createRolNietNatuurlijkPersoon
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
import java.util.Optional

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

        When("the variables are resolved with a text containing placeholders") {
            val resolvedText = mailTemplateHelper.resolveVariabelen(
                "fakeText, {ZAAK_NUMMER}, {ZAAK_URL}, {ZAAK_TYPE}, {ZAAK_STATUS}, {ZAAK_STARTDATUM}",
                zaak
            )

            Then("the variables in the provided text should be replaced by the correct values from the zaak") {
                resolvedText shouldBe "fakeText, ${zaak.identificatie}, $zaakTonenURL, ${zaakType.omschrijving}, " +
                    "${statusType.omschrijving}, 12-10-2021"
            }
        }
    }

    Given("A zaak with an initiator of role niet-natuurlijk persoon with a vestigingnummer") {
        val zaakType = createZaakType()
        val zaak = createZaak(
            zaakTypeURI = zaakType.url,
            status = URI("https://example.com/fakeStatus"),
            startDate = LocalDate.of(2021, 10, 12)
        )
        val zaakStatus = createZaakStatus()
        val statusType = createStatusType(omschrijving = "fakeStatusTypeDescription")
        val zaakTonenURL = URI("https://example.com/fakeURL")
        val vestigingsnummer = "123456789"
        val rolNietNatuurlijkPersoon = createRolNietNatuurlijkPersoon(
            nietNatuurlijkPersoonIdentificatie = createNietNatuurlijkPersoonIdentificatie(
                vestigingsnummer = vestigingsnummer
            )
        )
        val resultaatItem = createResultaatItem()
        every { ztcClientService.readZaaktype(zaak.zaaktype) } returns createZaakType()
        every { configuratieService.zaakTonenUrl(zaak.identificatie) } returns zaakTonenURL
        every { zrcClientService.readStatus(zaak.status) } returns zaakStatus
        every { ztcClientService.readStatustype(zaakStatus.statustype) } returns statusType
        every { zgwApiService.findInitiatorRoleForZaak(zaak) } returns rolNietNatuurlijkPersoon
        // note that this is not correct; instead of a rechtspersoon we should search for a vestiging
        // this will be fixed in a follow-up pull request
        every { kvkClientService.findRechtspersoon(vestigingsnummer) } returns Optional.of(resultaatItem)

        When("the variables are resolved with a text containing a placeholder for the zaak initiator") {
            val resolvedText = mailTemplateHelper.resolveVariabelen(
                "fakeText, {ZAAK_INITIATOR}",
                zaak
            )

            Then("the variables in the provided text should be replaced by the correct values from the zaak") {
                resolvedText shouldBe "fakeText, ${resultaatItem.naam}"
            }
        }
    }
})
